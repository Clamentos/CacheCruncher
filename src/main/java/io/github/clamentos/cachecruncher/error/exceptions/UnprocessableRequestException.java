package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///.
import java.io.Serializable;

///
public final class UnprocessableRequestException extends CacheCruncherException {

    ///
    public UnprocessableRequestException(final ErrorCode errorCode, final Serializable... arguments) {

        super(errorCode, arguments);
    }

    ///..
    public UnprocessableRequestException(final ErrorCode errorCode, final Throwable cause, final Serializable... arguments) {

        super(errorCode, cause, arguments);
    }

    ///
}
