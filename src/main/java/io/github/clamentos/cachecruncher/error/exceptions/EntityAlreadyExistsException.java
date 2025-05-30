package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///
public final class EntityAlreadyExistsException extends RuntimeException {

    ///
    public EntityAlreadyExistsException(final ErrorDetails errorDetails) {

        super(errorDetails);
    }

    ///
}
