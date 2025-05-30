package io.github.clamentos.cachecruncher.web.dtos;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import lombok.Getter;

///
@Getter

///
public final class AuthDto {

    ///
    private final String email;
    private final String password;

    ///
    @JsonCreator
    public AuthDto(@JsonProperty("email") final String email, @JsonProperty("password") final String password) {

        this.email = email;
        this.password = password;
    }

    ///
}
