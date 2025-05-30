package io.github.clamentos.cachecruncher.business.simulation.cache;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

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
    public static CacheCommandType determineType(final String command) throws IllegalArgumentException {

        if(command == null || command.isEmpty()) {

            return null;
        }

        final char opcode = command.charAt(0);

        if(opcode == 'R') return READ;
        if(opcode == 'W') return WRITE;
        if(opcode == '#') return REPEAT;
        if(opcode == 'P') return PREFETCH;
        if(opcode == 'N') return NOOP;
        if(opcode == 'F') return FLUSH;
        if(opcode == 'I') return INVALIDATE;

        throw new IllegalArgumentException(new ErrorDetails(ErrorCode.UNKNOWN_COMMAND_TYPE, command));
    }

    ///
}
