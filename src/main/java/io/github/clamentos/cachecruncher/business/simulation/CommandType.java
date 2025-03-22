package io.github.clamentos.cachecruncher.business.simulation;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///
public enum CommandType {

    ///
    READ,
    WRITE,
    REPEAT;

    ///
    public static CommandType determineType(String command) throws IllegalArgumentException {

        if(command == null || command.isEmpty()) {

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "CommandType.determineType -> Command cannot be null or empty"
            ));
        }

        if(command.charAt(0) == 'R') return READ;
        if(command.charAt(0) == 'W') return WRITE;
        if(command.charAt(0) == '#') return REPEAT;

        throw new IllegalArgumentException(ErrorFactory.create(

            ErrorCode.UNKNOWN_COMMAND_TYPE,
            "CommandType.determineType -> Unknown command type",
            command.charAt(0)
        ));
    }

    ///
}
