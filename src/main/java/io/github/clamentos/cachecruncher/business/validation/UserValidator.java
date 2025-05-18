package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///.
import java.util.regex.Pattern;

///.
import me.gosimple.nbvcxz.Nbvcxz;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class UserValidator extends BasicValidator {

    ///
    private final Pattern emailPattern;
    private final Nbvcxz passwordEstimator;

    ///
    public UserValidator() { // TODO: better idea would be to actually send a verification code to the email address

        this.emailPattern = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
        passwordEstimator = new Nbvcxz();
    }

    ///
    public void validate(String email, String password) throws IllegalArgumentException {

        super.requireNotNull(email, "email");
        super.requireNotNull(password, "password");

        if(!emailPattern.matcher(email).matches()) {

            throw super.fail("UserValidator.validate -> Malformed email", email);
        }

        if(passwordEstimator.estimate(password).getBasicScore() < 3) {

            throw super.fail("UserValidator.validate -> Weak password", password);
        }
    }

    ///
}
