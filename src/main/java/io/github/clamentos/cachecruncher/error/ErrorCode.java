package io.github.clamentos.cachecruncher.error;

///
public enum ErrorCode {

    ///
    VALIDATOR_BAD_FORMAT,
    UNKNOWN_COMMAND_TYPE,
    ILLEGAL_COMMAND_TYPE,

    CACHE_TRACE_NOT_FOUND,

    TOO_MANY_USERS,
    USER_ALREADY_EXISTS,
    USER_NOT_FOUND,
    USER_LOCKED,

    TOO_MANY_SESSIONS,
    SESSION_NOT_FOUND,
    EXPIRED_SESSION,

    INVALID_AUTH_HEADER,
    NOT_ENOUGH_PRIVILEGES,
    WRONG_PASSWORD,

    UNCATEGORIZED;

    ///
    private ErrorCode() {}

    ///
    public static ErrorCode getDefault() {

        return(ErrorCode.UNCATEGORIZED);
    }

    ///
}
