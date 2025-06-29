package io.github.clamentos.cachecruncher.business.simulation.cache.commands;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.utility.ErrorMessages;

///.
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///.
import org.springframework.http.HttpStatus;

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
    public static CacheCommandType deserialize(final String command) throws ValidationException {

        if(command == null || command.isEmpty()) return null;
        final char opcode = command.charAt(0);

        if(opcode == 'R') return READ;
        if(opcode == 'W') return WRITE;
        if(opcode == '#') return REPEAT;
        if(opcode == 'P') return PREFETCH;
        if(opcode == 'N') return NOOP;
        if(opcode == 'F') return FLUSH;
        if(opcode == 'I') return INVALIDATE;

        throw new ValidationException(

            ErrorCode.VALIDATOR_BAD_FORMAT,
            HttpStatus.BAD_REQUEST,
            command,
            ErrorMessages.UNKNOWN_CC_TYPE
        );
    }

    ///
}
