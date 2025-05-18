package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///
public class AuthenticationException extends SecurityException {

    ///
    public AuthenticationException(ErrorDetails errorDetails) {

        super(errorDetails);
    }

    ///
}
