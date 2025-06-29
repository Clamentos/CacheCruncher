package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///.
import java.io.Serializable;

///
public sealed class AuthenticationException extends CacheCruncherException permits WrongPasswordException {

    ///
    public AuthenticationException(final ErrorCode errorCode, final Serializable... arguments) {

        super(errorCode, arguments);
    }

    ///..
    public AuthenticationException(final ErrorCode errorCode, final Throwable cause, final Serializable... arguments) {

        super(errorCode, cause, arguments);
    }

    ///
}
