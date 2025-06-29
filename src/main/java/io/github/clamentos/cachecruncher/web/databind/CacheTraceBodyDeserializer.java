package io.github.clamentos.cachecruncher.web.databind;

///
import com.fasterxml.jackson.core.JsonParser;

///..
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

///..
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

///..
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

///..
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

///.
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandType;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeA;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeB;
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandTypeC;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///..
import io.github.clamentos.cachecruncher.utility.ErrorMessages;
import io.github.clamentos.cachecruncher.utility.MultiValueMap;

///.
import java.io.IOException;

///..
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

///
public class CacheTraceBodyDeserializer extends StdDeserializer<CacheTraceBody> {

    ///
    public CacheTraceBodyDeserializer() {

        super(CacheTraceBody.class);
    }

    ///
    @Override
    public CacheTraceBody deserialize(final JsonParser parser, final DeserializationContext context) throws IOException {

        final JsonNode json = parser.getCodec().readTree(parser);
        if(json == null || json.isNull()) return null;

        return new CacheTraceBody(

            this.deserializeSections(json.get("sections"), parser),
            this.deserializeBody(json.get("body"), parser, true)
        );
    }

    ///.
    private MultiValueMap<String, CacheCommand> deserializeSections(final JsonNode jsonNode, final JsonParser parser)
    throws IOException {

        if(jsonNode == null || jsonNode.isNull()) return null;

        if(!jsonNode.isObject()) {

            throw InvalidFormatException.from(

                parser,
                ErrorMessages.EXPECTED_JSON_OBJECT + jsonNode.getClass(),
                jsonNode,
                MultiValueMap.class
            );
        }

        final ObjectNode sectionsObj = (ObjectNode)jsonNode;
        final Set<Map.Entry<String, JsonNode>> entries = sectionsObj.properties();
        final MultiValueMap<String, CacheCommand> sections = new MultiValueMap<>(entries.size());

        for(final Map.Entry<String, JsonNode> entry : entries) {

            sections.put(entry.getKey(), this.deserializeBody(entry.getValue(), parser, false));
        }

        return sections;
    }

    ///..
    private List<CacheCommand> deserializeBody(final JsonNode jsonNode, final JsonParser parser, final boolean allowRepeat)
    throws IOException {

        if(jsonNode == null || jsonNode.isNull()) return null;

        if(!jsonNode.isArray()) {

            throw InvalidFormatException.from(

                parser,
                ErrorMessages.EXPECTED_JSON_ARRAY + jsonNode.getClass(),
                jsonNode,
                List.class
            );
        }

        final ArrayNode bodyJson = (ArrayNode)jsonNode;
        final Iterator<JsonNode> iterator = bodyJson.values();
        final List<CacheCommand> commands = new ArrayList<>(bodyJson.size());

        while(iterator.hasNext()) {

            commands.add(this.deserializeCommand(iterator.next(), parser, allowRepeat));
        }

        return commands;
    }

    ///..
    private CacheCommand deserializeCommand(final JsonNode jsonNode, final JsonParser parser, final boolean allowRepeat)
    throws IOException {

        if(jsonNode == null || jsonNode.isNull()) return null;

        if(!jsonNode.isTextual()) {

            throw InvalidFormatException.from(parser, ErrorMessages.EXPECTED_STRING + jsonNode.getClass(), jsonNode, String.class);
        }

        final String commandString = jsonNode.textValue();
        CacheCommandType type;

        try { type = CacheCommandType.deserialize(commandString); }
        catch(final ValidationException exc) { throw new DeserializationException(ErrorCode.VALIDATOR_BAD_FORMAT, exc, commandString); }

        switch(type) {

            case READ, WRITE, PREFETCH, INVALIDATE:

                final String[] splits = commandString.split(" ");
                if(splits.length != 2) throw this.fail(commandString, ErrorMessages.MALFORMED_CC_TYPE_B);

            return new CacheCommandTypeB(type, Short.parseShort(splits[0].substring(1)), Long.parseLong(splits[1], 16));

            case REPEAT:

                if(!allowRepeat) {

                    throw new IllegalArgumentException(new ErrorDetails(

                        ErrorCode.VALIDATOR_BAD_FORMAT,
                        commandString,
                        ErrorMessages.NOT_ALLOWED
                    ));
                }

                final String[] splits2 = commandString.split("#");
                if(splits2.length != 3) throw this.fail(commandString, ErrorMessages.MALFORMED_CC_TYPE_C);

            return new CacheCommandTypeC(type, Integer.parseInt(splits2[1]), splits2[2]);

            case NOOP: return new CacheCommandTypeA(type, Long.parseLong(commandString.substring(1)));
            case FLUSH: return new CacheCommand(type);

            case null: return null;
            default: throw this.fail(commandString, ErrorMessages.UNKNOWN_CC_TYPE);
        }
    }

    ///..
    private DeserializationException fail(final String commandString, final String message) {

        return new DeserializationException(ErrorCode.VALIDATOR_BAD_FORMAT, commandString, message);
    }

    ///
}
