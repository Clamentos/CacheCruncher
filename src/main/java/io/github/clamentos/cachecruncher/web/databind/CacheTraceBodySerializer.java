package io.github.clamentos.cachecruncher.web.databind;

///
import com.fasterxml.jackson.core.JsonGenerator;

///..
import com.fasterxml.jackson.databind.SerializerProvider;

///..
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

///.
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///..
import io.github.clamentos.cachecruncher.utility.MultiValueMap;

///.
import java.io.IOException;

///..
import java.util.List;
import java.util.Map;
import java.util.Objects;

///
public final class CacheTraceBodySerializer extends StdSerializer<CacheTraceBody> {

    ///
    public CacheTraceBodySerializer() {

        super(CacheTraceBody.class);
    }

    ///
    @Override
    public void serialize(final CacheTraceBody dto, final JsonGenerator generator, final SerializerProvider provider)
    throws IOException {

        generator.writeStartObject();

        if(dto != null) {

            this.serializeSections(dto.getSections(), generator);

            generator.writeArrayFieldStart("body");
            this.serializeCommands(dto.getBody(), generator);
            generator.writeEndArray();
        }

        generator.writeEndObject();
    }

    ///.
    private void serializeSections(final MultiValueMap<String, CacheCommand> sections, final JsonGenerator generator)
    throws IOException {

        generator.writeFieldName("sections");

        if(sections != null) {

            generator.writeStartObject();

            for(final Map.Entry<String, List<CacheCommand>> section : sections.entrySet()) {

                generator.writeArrayFieldStart(Objects.toString(section.getKey()));
                this.serializeCommands(section.getValue(), generator);
                generator.writeEndArray();
            }

            generator.writeEndObject();
        }

        else {

            generator.writeNull();
        }
    }

    ///..
    private void serializeCommands(final List<CacheCommand> commands, final JsonGenerator generator) throws IOException {

        if(commands != null) {

            for(final CacheCommand command : commands) {

                generator.writeString(Objects.toString(command));
            }
        }

        else {

            generator.writeNull();
        }
    }

    ///
}
