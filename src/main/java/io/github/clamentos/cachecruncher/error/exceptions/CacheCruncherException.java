package io.github.clamentos.cachecruncher.error.exceptions;

///
public abstract sealed class CacheCruncherException extends Exception permits AuthenticationException, AuthorizationException, DatabaseException, EntityAlreadyExistsException, EntityNotFoundException, ValidationException {

    ///
    protected CacheCruncherException(Throwable cause) {

        super(cause);
    }
}

///
