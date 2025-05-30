package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///..
import io.github.clamentos.cachecruncher.web.dtos.AuthDto;

///.
import me.gosimple.nbvcxz.Nbvcxz;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class UserValidator extends BasicValidator {

    ///
    private static final String PASSWORD_NAME = "password";

    ///..
    private final Nbvcxz passwordEstimator;

    ///
    public UserValidator() {

        passwordEstimator = new Nbvcxz();
    }

    ///
    public void validate(final String password) throws IllegalArgumentException {

        super.requireNotNull(password, PASSWORD_NAME);
        final int score = passwordEstimator.estimate(password).getBasicScore();

        if(score < 3) throw super.fail("is too weak: score " + score + "/4", PASSWORD_NAME);
    }

    ///..
    public void validate(final AuthDto authDto) throws IllegalArgumentException {

        super.requireNotNull(authDto, "DTO");
        
        super.requireNotBlank(authDto.getEmail(), "email");
        super.requireNotBlank(authDto.getPassword(), PASSWORD_NAME);
    }

    ///
}
