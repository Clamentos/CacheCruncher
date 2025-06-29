package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///.
import java.io.Serializable;

///
public final class DeserializationException extends CacheCruncherIOException {

    ///
    public DeserializationException(final ErrorCode errorCode, final Serializable... arguments) {

        super(errorCode, arguments);
    }

    ///..
    public DeserializationException(final ErrorCode errorCode, final Throwable cause, final Serializable... arguments) {

        super(errorCode, cause, arguments);
    }

    ///
}
