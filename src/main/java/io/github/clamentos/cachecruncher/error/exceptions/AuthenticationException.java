package io.github.clamentos.cachecruncher.error.exceptions;

///
public sealed class AuthenticationException extends CacheCruncherException permits WrongPasswordException {

    ///
    public AuthenticationException(Throwable cause) {

        super(cause);
    }

    ///
}
