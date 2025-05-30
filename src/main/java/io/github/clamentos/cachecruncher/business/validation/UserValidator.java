package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///.
import me.gosimple.nbvcxz.Nbvcxz;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class UserValidator extends BasicValidator {

    ///
    private final Nbvcxz passwordEstimator;

    ///
    public UserValidator() {

        passwordEstimator = new Nbvcxz();
    }

    ///
    public void validate(String password) throws IllegalArgumentException {

        super.requireNotNull(password, "password");
        int score = passwordEstimator.estimate(password).getBasicScore();

        if(score < 3) throw super.fail("is too weak: score " + score + "/4", "password");
    }

    ///
}
