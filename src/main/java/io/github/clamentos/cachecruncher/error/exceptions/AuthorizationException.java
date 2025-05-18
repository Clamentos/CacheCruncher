package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///
public class AuthorizationException extends SecurityException {

    ///
    public AuthorizationException(ErrorDetails errorDetails) {

        super(errorDetails);
    }

    ///
}
