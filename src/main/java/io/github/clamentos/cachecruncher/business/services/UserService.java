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

///..
import io.github.clamentos.cachecruncher.web.dtos.AuthDto;

///.
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

///.
import java.util.HexFormat;
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
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;

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

    ///..
    private final UserDao userDao;

    ///..
    private final JavaMailSender javaMailSender;

    ///..
    private final MessageDigest hasher;
    private final HexFormat hexer;

    ///..
    private final int bcryptEffort;
    private final int loginFailures;
    private final int loginFailuresCap;
    private final long userLockTime;
    private final String mailVerifySecret;
    private final boolean doSendVerificationEmail;
    private final long mailVerifyDuration;

    ///
    @Autowired
    public UserService(

        final UserValidator userValidator,
        final SessionService sessionService,
        final UserDao userDao,
        final JavaMailSender javaMailSender,
        final Environment environment

    ) throws IllegalArgumentException, NoSuchAlgorithmException {

        this.userValidator = userValidator;
        this.sessionService = sessionService;
        this.userDao = userDao;
        this.javaMailSender = javaMailSender;

        bcryptEffort = environment.getProperty("cache-cruncher.auth.bcryptEffort", Integer.class, 12);
        loginFailures = environment.getProperty("cache-cruncher.auth.loginFailures", Integer.class, 5);
        loginFailuresCap = loginFailures + environment.getProperty("cache-cruncher.auth.loginFailuresCap", Integer.class, 3);
        userLockTime = environment.getProperty("cache-cruncher.auth.userLockTime", Long.class, 60_000L);
        mailVerifySecret = environment.getProperty("cache-cruncher.auth.mailVerifySecret", String.class);
        doSendVerificationEmail = environment.getProperty("cache-cruncher.auth.doSendVerificationEmail", Boolean.class, true);
        mailVerifyDuration = environment.getProperty("cache-cruncher.auth.mailVerifyDuration", Long.class, 120_000L);

        if(mailVerifySecret == null || mailVerifySecret.isEmpty()) {

            throw new IllegalArgumentException("The property \"cache-cruncher.auth.mailVerifySecret\" cannot be null or empty");
        }

        hasher = MessageDigest.getInstance("SHA-256");
        hexer = HexFormat.of();
    }

    ///
    @Transactional
    public String register(final AuthDto authDto)
    throws DataAccessException, EntityAlreadyExistsException, IllegalArgumentException, MailException {

        userValidator.validate(authDto);

        final String email = authDto.getEmail();
        final String password = authDto.getPassword();

        userValidator.validate(password);
        if(userDao.exists(email)) throw new EntityAlreadyExistsException(new ErrorDetails(ErrorCode.USER_ALREADY_EXISTS, email));

        final String encryptedPassword = BCrypt.withDefaults().hashToString(bcryptEffort, password.toCharArray());
        userDao.insert(new User(-1L, null, System.currentTimeMillis(), null, email, encryptedPassword, (short)0, false));

        String plainToken = System.currentTimeMillis() + ";" + email;
        String hexHash = hexer.formatHex(hasher.digest((plainToken + ";" + mailVerifySecret).getBytes()));
        String hexToken = hexer.formatHex(plainToken.getBytes()) + ";" + hexHash;

        // false when testing.
        if(doSendVerificationEmail) {

            SimpleMailMessage emailMessage = new SimpleMailMessage();

            emailMessage.setTo(email);
            emailMessage.setFrom("cache-cruncher-noreply@gmail.com");
            emailMessage.setSubject("Account verification");
            emailMessage.setText("..."); // link with token in url

            javaMailSender.send(emailMessage);
            return "";
        }

        return hexToken;
    }

    ///..
    @Transactional
    public void confirmEmail(final String token)
    throws DataAccessException, EntityNotFoundException, IllegalArgumentException, IllegalStateException {

        userValidator.requireNotBlank(token, "token");
        final String[] splits = token.split(";");

        if(splits.length != 2) throw this.badlyFormattedToken();

        final String hexToken = splits[0];
        final String hexHashToCheck = splits[1];

        final String plainToken = new String(hexer.parseHex(hexToken)); // timestamp;email
        final String[] components = plainToken.split(";");

        if(components.length != 2) throw this.badlyFormattedToken();

        final long timestamp = Long.parseLong(components[0]);
        final String email = components[1];
        final long now = System.currentTimeMillis();

        if(timestamp + mailVerifyDuration <= now) {

            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.EXPIRED_SESSION, email));
        }

        final String hexHash = hexer.formatHex(hasher.digest((plainToken + ";" + mailVerifySecret).getBytes()));

        if(!hexHashToCheck.equals(hexHash)) { 

            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.INVALID_VERIFICATION_TOKEN));
        }

        final User user = userDao.selectByEmail(email);

        if(user == null) throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, email));
        if(user.getValidatedAt() != null) throw new IllegalStateException(new ErrorDetails(ErrorCode.USER_ALREADY_VALIDATED, email));

        if(!userDao.updateForEmailValidation(email, now)) {

            throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, email));
        }
    }

    ///..
    @Transactional(noRollbackFor = WrongPasswordException.class)
    public Pair<User, Session> login(final AuthDto authDto, final String device) throws AuthenticationException, DataAccessException {

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
            if(doLock) sessionService.removeAll(user.getId());

            throw new WrongPasswordException(new ErrorDetails(ErrorCode.WRONG_PASSWORD));
        }
    }

    ///..
    @Transactional
    public Session refresh(final Session session, final String device)
    throws AuthenticationException, AuthorizationException, DataAccessException {

        sessionService.remove(session.getId());
        return sessionService.generate(session.getUserId(), session.getEmail(), session.isAdmin(), device);
    }

    ///..
    @Transactional
    public void logout(final Session session) throws AuthenticationException, DataAccessException {

        sessionService.remove(session.getId());
        log.info("User {} logged out from single session", session.getUserId());
    }

    ///..
    @Transactional
    public void logoutAll(final Session session) {

        sessionService.removeAll(session.getUserId());
        log.info("User {} logged out from all sessions", session.getUserId());
    }

    ///..
    public List<User> getAll() throws DataAccessException {

        return userDao.selectAll();
    }

    ///..
    @Transactional
    public void updatePrivilege(final long userId, final boolean privilege) throws DataAccessException, EntityNotFoundException {

        if(!userDao.updatePrivilege(userId, privilege)) {

            throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, userId));
        }
    }

    ///..
    @Transactional
    public void delete(final long userId, final Session session)
    throws AuthorizationException, DataAccessException, EntityNotFoundException {

        final Boolean privilege = userDao.getPrivilege(userId);

        if(privilege == null) throw new EntityNotFoundException(new ErrorDetails(ErrorCode.USER_NOT_FOUND, userId));
        if(userId != session.getUserId()) sessionService.check(session.getId(), true, "Cannot delete a user other than self");

        userDao.delete(userId);
    }

    ///..
    private IllegalArgumentException badlyFormattedToken() {

        return new IllegalArgumentException(new ErrorDetails(ErrorCode.VALIDATOR_BAD_FORMAT, "token", "is badly formatted"));
    }

    ///
}
