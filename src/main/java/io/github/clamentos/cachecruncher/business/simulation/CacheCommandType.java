package io.github.clamentos.cachecruncher.business.simulation;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///
public enum CacheCommandType {

    ///
    READ,
    WRITE,
    REPEAT,
    PREFETCH,
    NOOP,
    FLUSH,
    INVALIDATE;

    ///
    public static CacheCommandType determineType(String command) throws IllegalArgumentException {

        if(command == null || command.isEmpty()) {

            return null;
        }

        char opcode = command.charAt(0);

        if(opcode == 'R') return READ;
        if(opcode == 'W') return WRITE;
        if(opcode == '#') return REPEAT;
        if(opcode == 'P') return PREFETCH;
        if(opcode == 'N') return NOOP;
        if(opcode == 'F') return FLUSH;
        if(opcode == 'I') return INVALIDATE;

        throw new IllegalArgumentException(ErrorFactory.create(

            ErrorCode.UNKNOWN_COMMAND_TYPE,
            "CommandType.determineType -> Unknown command type",
            command
        ));
    }

    ///
}
