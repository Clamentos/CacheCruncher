package io.github.clamentos.cachecruncher.business.services;

///
import at.favre.lib.crypto.bcrypt.BCrypt;

///.
import io.github.clamentos.cachecruncher.business.validation.UserValidator;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityAlreadyExistsException;
import io.github.clamentos.cachecruncher.error.exceptions.EntityNotFoundException;
import io.github.clamentos.cachecruncher.error.exceptions.WrongPasswordException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.UserDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;
import io.github.clamentos.cachecruncher.persistence.entities.User;

///..
import io.github.clamentos.cachecruncher.utility.Pair;

///.
import java.util.List;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.dao.DataAccessException;

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

    ///..
    private final UserDao userDao;

    ///..
    private final int bcryptEffort;
    private final int loginFailures;
    private final int loginFailuresCap;
    private final long userLockTime;

    ///
    @Autowired
    public UserService(UserValidator userValidator, SessionService sessionService, UserDao userDao, Environment environment) {

        this.userValidator = userValidator;
        this.sessionService = sessionService;
        this.userDao = userDao;

        bcryptEffort = environment.getProperty("cache-cruncher.auth.bcryptEffort", Integer.class, 12);
        loginFailures = environment.getProperty("cache-cruncher.auth.loginFailures", Integer.class, 5);
        loginFailuresCap = loginFailures + environment.getProperty("cache-cruncher.auth.loginFailuresCap", Integer.class, 3);
        userLockTime = environment.getProperty("cache-cruncher.auth.userLockTime", Long.class, 60_000L);
    }

    ///
    @Transactional
    public void register(String email, String password)
    throws DataAccessException, EntityAlreadyExistsException, IllegalArgumentException {

        userValidator.validate(email, password);

        if(userDao.exists(email)) {

            throw new EntityAlreadyExistsException(ErrorFactory.create(

                ErrorCode.USER_ALREADY_EXISTS,
                "UserService.register -> User already exists",
                email
            ));
        }

        String encryptedPassword = BCrypt.withDefaults().hashToString(bcryptEffort, password.toCharArray());
        userDao.insert(new User(-1L, null, System.currentTimeMillis(), email, encryptedPassword, (short)0, false));
    }

    ///..
    @Transactional(noRollbackFor = WrongPasswordException.class)
    public Pair<User, Session> login(String email, String password, String device) throws AuthenticationException, DataAccessException {

        User user = userDao.selectByUsername(email);

        if(user == null) {

            throw new AuthenticationException(ErrorFactory.create(

                ErrorCode.USER_NOT_FOUND,
                "UserService.login -> User not found",
                email
            ));
        }

        long now = System.currentTimeMillis();

        if(user.getLockedUntil() != null && user.getLockedUntil() >= now) {

            throw new AuthorizationException(ErrorFactory.create(

                ErrorCode.USER_LOCKED,
                "UserService.login -> User is locked because of too many failed login attempts",
                user.getLockedUntil()
            ));
        }

        short failedAccesses = user.getFailedAccesses();

        if(BCrypt.verifyer().verify(password.toCharArray(), user.getPassword()).verified) {

            if(failedAccesses > 0) {

                userDao.updateForLogin(user.getId(), user.getLockedUntil(), (short)0);
            }

            Session session = sessionService.generate(user.getId(), user.getEmail(), user.isAdmin(), device);
            user.setPassword(null);

            log.info("User {} logged in", user.getId());
            return new Pair<>(user, session);
        }

        else {

            Long lockedUntil;
            boolean doLock = failedAccesses >= loginFailures;

            if(doLock) {

                int shiftAmount = Math.min(failedAccesses - loginFailures, loginFailuresCap);
                lockedUntil = now + (userLockTime * (2 << shiftAmount));
            }

            else {

                lockedUntil = user.getLockedUntil();
            }

            userDao.updateForLogin(user.getId(), lockedUntil, (short)(failedAccesses + 1));
            if(doLock) sessionService.removeAll(user.getId());

            throw new WrongPasswordException(ErrorFactory.create(ErrorCode.WRONG_PASSWORD, "UserService::login -> Wrong password"));
        }
    }

    ///..
    @Transactional
    public void logout(Session session) throws AuthenticationException, DataAccessException {

        sessionService.remove(session.getId());
        log.info("User {} logged out from single session", session.getUserId());
    }

    ///..
    @Transactional
    public void logoutAll(Session session) {

        sessionService.removeAll(session.getUserId());
        log.info("User {} logged out from all sessions", session.getUserId());
    }

    ///..
    public List<User> getAll() throws DataAccessException {

        return userDao.selectAll();
    }

    ///..
    @Transactional
    public void updatePrivilege(long userId, boolean privilege) throws DataAccessException {

        userDao.updatePrivilege(userId, privilege);
    }

    ///..
    @Transactional
    public void delete(long userId, Session session) throws AuthorizationException, DataAccessException, EntityNotFoundException {

        Boolean privilege = userDao.getPrivilege(userId);

        if(privilege == null) {

            throw new AuthenticationException(ErrorFactory.create(

                ErrorCode.USER_NOT_FOUND,
                "UserService::delete -> User not found",
                userId
            ));
        }

        if(userId != session.getUserId()) {

            sessionService.check(session.getId(), true, "Cannot delete a user other than self");
        }

        userDao.delete(userId);
    }

    ///
}
