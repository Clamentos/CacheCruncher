package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///.
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

///..
import java.util.HexFormat;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;

///..
import org.springframework.mail.javamail.JavaMailSender;

///..
import org.springframework.stereotype.Service;

///
@Service
@Slf4j

///
public class VerificationService {

    ///
    private final JavaMailSender javaMailSender;

    ///..
    private final MessageDigest hasher;
    private final HexFormat hexer;

    ///..
    private final String mailVerifySecret;
    private final long mailVerifyDuration;

    ///
    @Autowired
    public VerificationService(final JavaMailSender javaMailSender, Environment environment) throws NoSuchAlgorithmException {

        this.javaMailSender = javaMailSender;

        hasher = MessageDigest.getInstance("SHA-256");
        hexer = HexFormat.of();

        mailVerifySecret = environment.getProperty("cache-cruncher.auth.mailVerifySecret", String.class);
        mailVerifyDuration = environment.getProperty("cache-cruncher.auth.mailVerifyDuration", Long.class, 120_000L);
    }

    ///
    public String sendVerificationEmail(String email) {

        final String plainToken = System.currentTimeMillis() + ";" + email;
        final String hexHash = hexer.formatHex(hasher.digest((plainToken + ";" + mailVerifySecret).getBytes()));
        final String hexToken = hexer.formatHex(plainToken.getBytes()) + ";" + hexHash;

        // Only when testing.
        if(mailVerifySecret == null || mailVerifySecret.isEmpty()) {

            final SimpleMailMessage emailMessage = new SimpleMailMessage();

            emailMessage.setTo(email);
            emailMessage.setFrom("cache-cruncher-noreply@gmail.com");
            emailMessage.setSubject("Account verification");
            emailMessage.setText("..."); // link with token in url

            // https://.../cache-cruncher/user/confirm-email

            try { javaMailSender.send(emailMessage); }
            catch(final MailException exc) { log.error("Could not send mail", exc); }

            return "";
        }

        return hexToken;
    }

    ///..
    public String verify(String token, final long now) throws AuthenticationException, ValidationException {

        final String[] splits = token.split(";");

        if(splits.length != 2) throw this.badlyFormattedToken();

        final String hexToken = splits[0];
        final String hexHashToCheck = splits[1];

        final String plainToken = new String(hexer.parseHex(hexToken)); // timestamp;email
        final String[] components = plainToken.split(";");

        if(components.length != 2) throw this.badlyFormattedToken();

        final long timestamp = Long.parseLong(components[0]);
        final String email = components[1];

        if(timestamp + mailVerifyDuration <= now) {

            throw new AuthenticationException(new ErrorDetails(ErrorCode.EXPIRED_SESSION, email));
        }

        final String hexHash = hexer.formatHex(hasher.digest((plainToken + ";" + mailVerifySecret).getBytes()));

        if(!hexHashToCheck.equals(hexHash)) { 

            throw new AuthenticationException(new ErrorDetails(ErrorCode.INVALID_VERIFICATION_TOKEN));
        }

        return email;
    }

    ///.
    private ValidationException badlyFormattedToken() {

        return new ValidationException(new ErrorDetails(ErrorCode.VALIDATOR_BAD_FORMAT, "token", "is badly formatted"));
    }

    ///
}
