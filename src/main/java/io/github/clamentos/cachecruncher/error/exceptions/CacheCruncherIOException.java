package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///.
import java.io.IOException;
import java.io.Serializable;

///
public abstract sealed class CacheCruncherIOException extends IOException permits DeserializationException, SerializationException {

    ///
    protected CacheCruncherIOException(final ErrorCode errorCode, final Serializable... arguments) {

        super(new ErrorDetails(errorCode, arguments));
    }

    ///..
    protected CacheCruncherIOException(final ErrorCode errorCode, final Throwable cause, final Serializable... arguments) {

        super(new ErrorDetails(errorCode, cause, arguments));
    }

    ///
}
