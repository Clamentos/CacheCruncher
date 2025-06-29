package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///.
import java.io.Serializable;

///
public final class SerializationException extends CacheCruncherIOException {

    ///
    public SerializationException(final ErrorCode errorCode, final Serializable... arguments) {

        super(errorCode, arguments);
    }

    ///..
    public SerializationException(final ErrorCode errorCode, final Throwable cause, final Serializable... arguments) {

        super(errorCode, cause, arguments);
    }

    ///
}
