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
    REAL_IP_MISSING("The request is missing the X-Real-IP header or it is malformed"),
    TOO_MANY_REQUESTS("The limit of ? requests per minute has been reached. Retry after ? ms"),
    API_NOT_FOUND("The endpoint ? does not exist"),
    
    VALIDATOR_BAD_FORMAT("Argument ? ?"),
    SERIALIZATION_ERROR("Could not serialize JSON"),
    DESERIALIZATION_ERROR("Could not deserialize JSON"),
    JSON_TOO_DEEP("JSON is to deep, maximum depth for type ? is ?"),
    UNKNOWN_COMMAND_TYPE("Unknown command type ?"),
    ILLEGAL_COMMAND_TYPE("?"),

    CACHE_TRACE_NOT_FOUND("The trace with id ? does not exist"),

    USER_ALREADY_EXISTS("User ? already exists"),
    USER_NOT_FOUND("User ? does not exist"),
    USER_NOT_VALIDATED("User ? hasn't validated the email yet"),
    USER_ALREADY_VALIDATED("User ? has already validated the email"),
    USER_LOCKED("User is locked until ? because of too many failed login attempts (?)"),

    TOO_MANY_OVERALL_SESSIONS("Could not create session, too many sessions (?)"),
    TOO_MANY_SESSIONS("Could not create session, user has too many (?)"),
    SESSION_NOT_FOUND("Session does not exist"),
    EXPIRED_SESSION("Session expired (?)"),
    INVALID_VERIFICATION_TOKEN("Invalid verification token"),

    INVALID_AUTH_HEADER("Bad or missing auth header"),
    NOT_ENOUGH_PRIVILEGES("?"),
    WRONG_PASSWORD("Wrong password"),

    GENERIC("?"),                       // Generic error that can contain any message.
    UNCATEGORIZED("Uncategorized");     // Default fallback.

    ///
    private final String message;

    ///
    public static ErrorCode getDefault() {

        return(ErrorCode.UNCATEGORIZED);
    }

    ///
}
