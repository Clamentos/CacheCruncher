package io.github.clamentos.cachecruncher.business.simulation.cache.commands;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///.
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum CacheCommandType {

    ///
    READ("R"),               // R8 FFF01FFFFFFFFFA3              type B
    WRITE("W"),              // W4 FFB0863CDFFFFFA3              type B
    REPEAT("#"),             // #123#section_name                type C
    PREFETCH("P"),           // P64 FAB0863CDFFFFF00             type B
    NOOP("N"),               // N12                              type A
    FLUSH("F"),              // F                                base
    INVALIDATE("I");         // I32 FFF01FFFFFFFFFA3             type B

    ///
    private final String type;

    ///
    public static CacheCommandType determineType(final String command) throws IllegalArgumentException {

        if(command == null || command.isEmpty()) return null;
        final char opcode = command.charAt(0);

        if(opcode == 'R') return READ;
        if(opcode == 'W') return WRITE;
        if(opcode == '#') return REPEAT;
        if(opcode == 'P') return PREFETCH;
        if(opcode == 'N') return NOOP;
        if(opcode == 'F') return FLUSH;
        if(opcode == 'I') return INVALIDATE;

        throw new IllegalArgumentException(new ErrorDetails(ErrorCode.UNKNOWN_COMMAND_TYPE, opcode));
    }

    ///
}
