package io.github.clamentos.cachecruncher.business.services;

///
import at.favre.lib.crypto.bcrypt.BCrypt;

///.
import io.github.clamentos.cachecruncher.business.validation.UserValidator;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.CacheCruncherException;
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;
import io.github.clamentos.cachecruncher.error.exceptions.WrongPasswordException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.UserDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;
import io.github.clamentos.cachecruncher.persistence.entities.User;

///..
import io.github.clamentos.cachecruncher.utility.Pair;

///..
import io.github.clamentos.cachecruncher.web.dtos.AuthDto;

///.
import java.util.List;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.mail.javamail.JavaMailSender;

///..
import org.springframework.stereotype.Service;

///..
import org.springframework.transaction.annotation.Transactional;

///
@Service
@Slf4j

///
public class UserService {

    ///
    private final UserValidator userValidator;

    ///..
    private final SessionService sessionService;
    private final VerificationService verificationService;

    ///..
    private final UserDao userDao;

    ///..
    private final int bcryptEffort;
    private final int loginFailures;
    private final int loginFailuresCap;
    private final long userLockTime;

    ///
    @Autowired
    public UserService(

        final UserValidator userValidator,
        final SessionService sessionService,
        final VerificationService verificationService,
        final UserDao userDao,
        final JavaMailSender javaMailSender,
        final Environment environment
    ) {

        this.userValidator = userValidator;
        this.sessionService = sessionService;
        this.verificationService = verificationService;
        this.userDao = userDao;

        bcryptEffort = environment.getProperty("cache-cruncher.auth.bcryptEffort", Integer.class, 12);
        loginFailures = environment.getProperty("cache-cruncher.auth.loginFailures", Integer.class, 5);
        loginFailuresCap = loginFailures + environment.getProperty("cache-cruncher.auth.loginFailuresCap", Integer.class, 3);
        userLockTime = environment.getProperty("cache-cruncher.auth.userLockTime", Long.class, 60_000L);
    }

    ///
    @Transactional(rollbackFor = CacheCruncherException.class)
    public String register(final AuthDto authDto) throws DatabaseException, EntityAlreadyExistsException, ValidationException {

        userValidator.validate(authDto);

        final String email = authDto.getEmail();
        final String password = authDto.getPassword();

        userValidator.validate(password);
        if(userDao.exists(email)) throw new EntityAlreadyExistsException(new ErrorDetails(ErrorCode.USER_ALREADY_EXISTS, email));

        final String encryptedPassword = BCrypt.withDefaults().hashToString(bcryptEffort, password.toCharArray());
        userDao.insert(new User(-1L, null, System.currentTimeMillis(), null, email, encryptedPassword, (short)0, false));

        return verificationService.sendVerificationEmail(email);
    }

    ///..
    public String resendVerificationEmail(final String email) {

        return verificationService.sendVerificationEmail(email);
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void confirmEmail(final String token)
    throws AuthenticationException, DatabaseException, EntityNotFoundException, ValidationException {

        userValidator.requireNotBlank(token, "token");

        final long now = System.currentTimeMillis();
        final String email = verificationService.verify(token, now);
        final User user = userDao.selectByEmail(email);

        if(user == null) throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, email));
        if(user.getValidatedAt() != null) throw new AuthenticationException(new ErrorDetails(ErrorCode.USER_ALREADY_VALIDATED, email));

        if(!userDao.updateForEmailValidation(email, now)) {

            throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, email));
        }
    }

    ///..
    @Transactional(

        rollbackFor = {AuthenticationException.class, AuthorizationException.class, DatabaseException.class, ValidationException.class},
        noRollbackFor = WrongPasswordException.class
    )
    public Pair<User, Session> login(final AuthDto authDto, final String device)
    throws AuthenticationException, AuthorizationException, DatabaseException, ValidationException {

        userValidator.validate(authDto);

        final String email = authDto.getEmail();
        final String password = authDto.getPassword();
        final User user = userDao.selectByEmail(email);
        final long now = System.currentTimeMillis();

        if(user == null) throw new AuthenticationException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, email));
        if(user.getValidatedAt() == null) throw new AuthenticationException(new ErrorDetails(ErrorCode.USER_NOT_VALIDATED, email));

        if(user.getLockedUntil() != null && user.getLockedUntil() >= now) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.USER_LOCKED, user.getLockedUntil()));
        }

        final short failedAccesses = user.getFailedAccesses();

        if(BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified) {

            if(failedAccesses > 0) userDao.updateForLogin(user.getId(), user.getLockedUntil(), (short)0);

            final Session session = sessionService.generate(user.getId(), user.getEmail(), user.isAdmin(), device);
            user.setPassword(null);

            log.info("User {} logged in", user.getId());
            return new Pair<>(user, session);
        }

        else {

            Long lockedUntil;
            final boolean doLock = failedAccesses >= loginFailures;

            if(doLock) {

                final int shiftAmount = Math.min(failedAccesses - loginFailures, loginFailuresCap);
                lockedUntil = now + (userLockTime * (2 << shiftAmount));
            }

            else {

                lockedUntil = user.getLockedUntil();
            }

            userDao.updateForLogin(user.getId(), lockedUntil, (short)(failedAccesses + 1));
            if(doLock) this.logoutAll(user.getId());

            throw new WrongPasswordException(new ErrorDetails(ErrorCode.WRONG_PASSWORD));
        }
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public Session refresh(final Session session, final String device)
    throws AuthenticationException, AuthorizationException, DatabaseException {

        sessionService.remove(session.getId());
        return sessionService.generate(session.getUserId(), session.getEmail(), session.isAdmin(), device);
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void logout(final Session session) throws AuthenticationException, DatabaseException {

        sessionService.remove(session.getId());
        log.info("User {} logged out from single session", session.getUserId());
    }

    ///..
    public void logoutAll(final long userId) {

        for(final Session session : sessionService.getSessions()) {

            if(session.getUserId() == userId) {

                try { sessionService.remove(session.getId()); }
                catch(final AuthenticationException exc) { log.warn(((ErrorDetails)exc.getCause()).getErrorCode().toString()); }

                catch(final DatabaseException exc) {

                    log.error(

                        "Could not remove session because of {}: {}, will skip this one",
                        exc.getClass().getSimpleName(),
                        exc.getMessage()
                    );
                }
            }
        }

        log.info("User {} logged out from all sessions", userId);
    }

    ///..
    public List<User> getAll() throws DatabaseException {

        return userDao.selectAll();
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void updatePrivilege(final long userId, final boolean privilege) throws DatabaseException, EntityNotFoundException {

        if(!userDao.updatePrivilege(userId, privilege)) {

            throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, userId));
        }
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void delete(final long userId, final Session session)
    throws AuthenticationException, AuthorizationException, DatabaseException, EntityNotFoundException {

        final Boolean privilege = userDao.getPrivilege(userId);

        if(privilege == null) throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, userId));
        if(userId != session.getUserId()) sessionService.check(session.getId(), true, "Cannot delete a user other than self");

        userDao.delete(userId);
    }

    ///
}
