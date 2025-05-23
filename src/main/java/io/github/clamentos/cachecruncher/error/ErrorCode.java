package io.github.clamentos.cachecruncher.error;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum ErrorCode {

    ///
    VALIDATOR_BAD_FORMAT("Argument ? ?"),
    SERIALIZATION_ERROR("Could not serialize JSON"),
    DESERIALIZATION_ERROR("Could not deserialize JSON"),
    JSON_TOO_DEEP("JSON is to deep, maximum depth for type ? is ?"),
    UNKNOWN_COMMAND_TYPE("Unknown command type ?"),
    ILLEGAL_COMMAND_TYPE("?"),

    CACHE_TRACE_NOT_FOUND("The trace with id ? does not exist"),

    TOO_MANY_USERS("Could not create session, too many logged users (?)"),
    USER_ALREADY_EXISTS("User ? already exists"),
    USER_NOT_FOUND("User ? does not exist"),
    USER_LOCKED("User is locked until ? because of too many failed login attempts (?)"),

    TOO_MANY_SESSIONS("Could not create session, user has too many (?)"),
    SESSION_NOT_FOUND("Session does not exist"),
    EXPIRED_SESSION("Session expired (?)"),

    INVALID_AUTH_HEADER("Bad or missing auth header"),
    NOT_ENOUGH_PRIVILEGES("?"),
    WRONG_PASSWORD("Wrong password"),

    GENERIC("?"),                       // Generic error that can contain any.
    UNCATEGORIZED("Uncategorized");     // Default fallback.

    ///
    private final String message;

    ///
    public static ErrorCode getDefault() {

        return(ErrorCode.UNCATEGORIZED);
    }

    ///
}
