package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

///..
import java.util.HexFormat;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

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
    public VerificationService(

        final JavaMailSender javaMailSender,
        final PropertyProvider propertyProvider

    ) throws BeanCreationException {

        this.javaMailSender = javaMailSender;

        try { hasher = MessageDigest.getInstance("SHA-256"); }
        catch(final NoSuchAlgorithmException exc) { throw new BeanCreationException(exc.getMessage()); }

        hexer = HexFormat.of();

        mailVerifySecret = propertyProvider.getString("cache-cruncher.auth.mailVerifySecret", null);
        mailVerifyDuration = propertyProvider.getLong("cache-cruncher.auth.mailVerifyDuration", 120_000L, 60_000L, Long.MAX_VALUE);
    }

    ///
    public String sendVerificationEmail(String email) {

        final String plainToken = System.currentTimeMillis() + ";" + email;
        final String hexHash = hexer.formatHex(hasher.digest((plainToken + ";" + mailVerifySecret).getBytes()));
        final String hexToken = hexer.formatHex(plainToken.getBytes()) + ";" + hexHash;

        // Only when testing.
        if(mailVerifySecret != null && !mailVerifySecret.isEmpty()) {

            final SimpleMailMessage emailMessage = new SimpleMailMessage();

            emailMessage.setTo(email);
            emailMessage.setFrom("cache-cruncher-noreply@gmail.com");
            emailMessage.setSubject("Account verification");
            emailMessage.setText("..."); // link with token in url TODO

            // https://.../cache-cruncher/user/confirm-email

            try { javaMailSender.send(emailMessage); }
            catch(final MailException exc) { log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage()); }

            return "";
        }

        return hexToken;
    }

    ///..
    public String verify(String token, final long now) throws AuthenticationException, ValidationException {

        final String[] splits = token.split(";");

        if(splits.length != 2) throw new ValidationException(ErrorCode.VALIDATOR_BAD_FORMAT, "token", "is badly formatted");

        final String hexToken = splits[0];
        final String hexHashToCheck = splits[1];

        final String plainToken = new String(hexer.parseHex(hexToken)); // timestamp;email
        final String[] components = plainToken.split(";");

        if(components.length != 2) throw new ValidationException(ErrorCode.VALIDATOR_BAD_FORMAT, "token", "is badly formatted");

        final long timestamp = Long.parseLong(components[0]);
        final String email = components[1];

        if(timestamp + mailVerifyDuration <= now) {

            throw new AuthenticationException(ErrorCode.EXPIRED_VERIFICATION_TOKEN, email);
        }

        final String hexHash = hexer.formatHex(hasher.digest((plainToken + ";" + mailVerifySecret).getBytes()));
        if(!hexHashToCheck.equals(hexHash)) throw new AuthenticationException(ErrorCode.INVALID_VERIFICATION_TOKEN);

        return email;
    }

    ///
}
