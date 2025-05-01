package io.github.clamentos.cachecruncher.error;

///
public enum ErrorCode {

    ///
    VALIDATOR_BAD_FORMAT,
    UNKNOWN_COMMAND_TYPE,
    ILLEGAL_COMMAND_TYPE,

    CACHE_TRACE_NOT_FOUND,

    SIMULATION_ERROR,

    SERVICE_TEMPORARILY_UNAVAILABLE,

    UNCATEGORIZED;

    ///
    private ErrorCode() {}

    ///
    public static ErrorCode getDefault() {

        return(ErrorCode.UNCATEGORIZED);
    }

    ///
}
