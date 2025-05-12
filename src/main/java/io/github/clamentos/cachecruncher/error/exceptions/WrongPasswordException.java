package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///
public final class WrongPasswordException extends AuthenticationException {

    ///
    public WrongPasswordException(ErrorDetails errorDetails) {

        super(errorDetails);
    }

    ///
}
