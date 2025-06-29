package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///.
import java.io.Serializable;

///
public abstract sealed class CacheCruncherException extends Exception permits AuthenticationException, AuthorizationException,
DatabaseException, EntityAlreadyExistsException, EntityNotFoundException, UnprocessableRequestException, ValidationException {

    ///
    protected CacheCruncherException(final ErrorCode errorCode, final Serializable... arguments) {

        super(new ErrorDetails(errorCode, arguments));
    }

    ///..
    protected CacheCruncherException(final ErrorCode errorCode, final Throwable cause, final Serializable... arguments) {

        super(new ErrorDetails(errorCode, cause, arguments));
    }

    ///
}
