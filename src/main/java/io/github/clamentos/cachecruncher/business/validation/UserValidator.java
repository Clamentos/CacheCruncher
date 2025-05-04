package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///.
import java.util.regex.Pattern;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class UserValidator extends BasicValidator {

    ///
    private final Pattern emailPattern;
    private final Pattern passwordPattern;

    ///
    public UserValidator() {

        this.emailPattern = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
        passwordPattern = Pattern.compile("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=?!])(?=\\S+$).{10,64}$");
    }

    ///
    public void validate(String email, String password) throws IllegalArgumentException {

        super.requireNotNull(email, "email");
        super.requireNotNull(password, "password");

        if(!emailPattern.matcher(email).matches()) {

            throw super.fail("UserValidator.validate -> Malformed email", email);
        }

        if(!passwordPattern.matcher(password).matches()) {

            throw super.fail("UserValidator.validate -> Weak password", password);
        }
    }

    ///
}
