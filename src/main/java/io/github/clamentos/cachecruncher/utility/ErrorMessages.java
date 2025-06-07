package io.github.clamentos.cachecruncher.utility;

///
public final class ErrorMessages {

    ///
    private ErrorMessages() {}

    ///
    public static final String NOT_ALLOWED = "is not allowed";

    ///..
    public static final String NOT_ENOUGH_PRIVILEGES = "Not enough privileges to call this API";

    ///..
    public static final String EXPECTED_JSON_OBJECT = "Expected json object, found: ";
    public static final String EXPECTED_JSON_ARRAY = "Expected json array, found: ";
    public static final String EXPECTED_STRING = "Expected string, found: ";
    public static final String MALFORMED_CC_TYPE_B = "is a malformed cache READ, WRITE, PREFETCH or INVALIDATE command";
    public static final String MALFORMED_CC_TYPE_C = "is a malformed cache REPEAT command";
    public static final String UNKNOWN_CC_TYPE = "is an unknown cache command type";

    ///..
    public static final String VALIDATOR_NN = "cannot be null";
    public static final String VALIDATOR_N = "must be null";
    public static final String VALIDATOR_NB = "cannot be null or blank";
    public static final String VALIDATOR_NE = "cannot be null or empty";
    public static final String VALIDATOR_B = "cannot be null and must be between ";

    ///..
    public static final String METHOD_ILLEGAL_ARGS = "Method arguments cannot be null";

    ///..
    public static final String PASSWORD_WEAK = "is too weak: score ";

    ///..
    public static final String REPLACEMENT_NO_NOOP = "Replacement policy type cannot be NOOP";
    public static final String REPLACEMENT_NOOP = "Replacement policy type must be NOOP";

    ///
}
