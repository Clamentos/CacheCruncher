package io.github.clamentos.cachecruncher.mappers;

///
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandType;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeA;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeB;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeC;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.mappers.serializers.CacheTraceBodyInputStream;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///..
import io.github.clamentos.cachecruncher.utility.MultiValueMap;

///.
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

///..
import java.sql.SQLException;

///..
import java.util.ArrayList;
import java.util.List;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public final class CacheTraceMapper {

    ///
    public CacheTraceBody deserializeBody(final InputStream blob) throws SQLException { // TODO: chunked to improve perf.

        final DataInputStream inputStream = new DataInputStream(blob);

        try {

            final int sectionsLength = inputStream.readInt();
            final MultiValueMap<String, CacheCommand> sections = new MultiValueMap<>(sectionsLength);

            for(int i = 0; i < sectionsLength; i++) {

                final String name = this.readString(inputStream);
                final int entryLength = inputStream.readInt();

                for(int j = 0; j < entryLength; j++) {

                    sections.add(name, this.readCommand(inputStream));
                }
            }

            final int traceLength = inputStream.readInt();
            final List<CacheCommand> trace = new ArrayList<>(traceLength);

            for(int i = 0; i < traceLength; i++) {

                trace.add(this.readCommand(inputStream));
            }

            return new CacheTraceBody(sections, trace);
        }

        catch(final IOException exc) {

            throw new SQLException(exc);
        }
    }

    ///..
    public InputStream serializeBody(final CacheTraceBody cacheTraceBody) {

        return new CacheTraceBodyInputStream(cacheTraceBody);
    }

    ///.
    private String readString(final DataInputStream inputStream) throws IOException {

        final int length = inputStream.readInt();
        final byte[] string = new byte[length];

        inputStream.readFully(string);
        return new String(string);
    }

    ///..
    private CacheCommand readCommand(final DataInputStream inputStream) throws IOException {

        final byte type = inputStream.readByte();

        switch(type) {

            case 0: return new CacheCommandTypeB(CacheCommandType.READ, inputStream.readShort(), inputStream.readLong());
            case 1: return new CacheCommandTypeB(CacheCommandType.WRITE, inputStream.readShort(), inputStream.readLong());
            case 2: return new CacheCommandTypeC(CacheCommandType.REPEAT, inputStream.readInt(), this.readString(inputStream));
            case 3: return new CacheCommandTypeB(CacheCommandType.PREFETCH, inputStream.readShort(), inputStream.readLong());
            case 4: return new CacheCommandTypeA(CacheCommandType.NOOP, inputStream.readLong());
            case 5: return new CacheCommand(CacheCommandType.FLUSH);
            case 6: return new CacheCommandTypeB(CacheCommandType.INVALIDATE, inputStream.readShort(), inputStream.readLong());

            default: throw new IOException(new ErrorDetails(ErrorCode.UNKNOWN_COMMAND_TYPE, type));
        }
    }

    ///
}

/*
 * - sections length 4B (counts elements)
 *     [
 *         - name length 4B (counts bytes)
 *         - name nB (string)
 *         - elements length 4B (counts elements)
 *             [
 *                 #command
 *             ]
 *     ]
 * - trace length 4B (counts elements)
 *     [
 *         #command
 *     ]
*/
