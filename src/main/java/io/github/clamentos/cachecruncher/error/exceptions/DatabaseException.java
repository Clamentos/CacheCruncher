package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///
public final class DatabaseException extends CacheCruncherException {

    ///
    public DatabaseException(final Throwable cause) {

        super(ErrorCode.GENERIC, cause);
    }

    ///
}
