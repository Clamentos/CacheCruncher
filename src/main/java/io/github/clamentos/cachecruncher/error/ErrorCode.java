package io.github.clamentos.cachecruncher.error;

///
import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum ErrorCode {

    ///
    REAL_IP_MISSING("The request is missing the X-Real-IP header or it is malformed", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS("The limit of ? requests per minute has been reached. Retry after ? ms", HttpStatus.TOO_MANY_REQUESTS),
    API_NOT_FOUND("The endpoint ? does not exist", HttpStatus.NOT_FOUND),

    VALIDATOR_BAD_FORMAT("Argument ? ?", HttpStatus.BAD_REQUEST),
    SERIALIZATION_ERROR("Could not serialize JSON", HttpStatus.UNPROCESSABLE_ENTITY),
    DESERIALIZATION_ERROR("Could not deserialize JSON", HttpStatus.UNPROCESSABLE_ENTITY),
    JSON_TOO_DEEP("Input JSON is to deep, maximum depth for type ? is ?", HttpStatus.BAD_REQUEST),
    UNKNOWN_COMMAND_TYPE("Unknown command type ?", HttpStatus.BAD_REQUEST),

    CACHE_TRACE_NOT_FOUND("The trace with id ? does not exist", HttpStatus.NOT_FOUND),

    USER_ALREADY_EXISTS("User ? already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND("User ? does not exist", HttpStatus.NOT_FOUND),
    USER_NOT_VALIDATED("User ? hasn't validated the email yet", HttpStatus.UNAUTHORIZED),
    USER_ALREADY_VALIDATED("User ? has already validated the email", HttpStatus.UNAUTHORIZED),
    USER_LOCKED("User is locked until ? because of too many failed login attempts (?)", HttpStatus.UNAUTHORIZED),
    FAILED_LOGIN("Bad credentials", HttpStatus.UNAUTHORIZED),

    TOO_MANY_OVERALL_SESSIONS("Could not create session, too many sessions (?)", HttpStatus.FORBIDDEN),
    TOO_MANY_SESSIONS("Could not create session, user has too many (?)", HttpStatus.FORBIDDEN),
    SESSION_NOT_FOUND("Session does not exist", HttpStatus.UNAUTHORIZED),
    EXPIRED_SESSION("Session expired (?)", HttpStatus.UNAUTHORIZED),
    INVALID_VERIFICATION_TOKEN("Invalid verification token", HttpStatus.UNAUTHORIZED),
    EXPIRED_VERIFICATION_TOKEN("Expired verification token", HttpStatus.UNAUTHORIZED),

    INVALID_AUTH_HEADER("Bad or missing auth header", HttpStatus.UNAUTHORIZED),
    NOT_ENOUGH_PRIVILEGES("?", HttpStatus.FORBIDDEN),

    GENERIC("?", HttpStatus.UNPROCESSABLE_ENTITY),    // Generic error that can contain any message (for "unexpected but handled").
    UNCATEGORIZED("Uncategorized", HttpStatus.INTERNAL_SERVER_ERROR);     // Default fallback.

    ///
    private final String message;
    private final HttpStatus responseStatus;

    ///
    public static ErrorCode getDefault() {

        return(ErrorCode.UNCATEGORIZED);
    }

    ///
}
