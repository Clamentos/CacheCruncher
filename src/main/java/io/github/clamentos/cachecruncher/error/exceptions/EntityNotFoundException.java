package io.github.clamentos.cachecruncher.error.exceptions;

///
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///
public final class EntityNotFoundException extends RuntimeException {

    ///
    public EntityNotFoundException(ErrorDetails errorDetails) {

        super(errorDetails.getErrorCode().getMessage(), errorDetails);
    }

    ///
}
