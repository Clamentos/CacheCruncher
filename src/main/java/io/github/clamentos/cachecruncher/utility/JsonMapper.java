package io.github.clamentos.cachecruncher.utility;

///
import com.fasterxml.jackson.core.JsonProcessingException;

///..
import com.fasterxml.jackson.core.type.TypeReference;

///..
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;
import io.github.clamentos.cachecruncher.error.exceptions.SerializationException;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///.
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class JsonMapper {

    ///
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public JsonMapper(final ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    ///
    public String serialize(final Object object) throws SerializationException {

        try {

            return objectMapper.writeValueAsString(object);
        }

        catch(final JsonProcessingException exc) {

            log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());
            throw new SerializationException(ErrorCode.SERIALIZATION_ERROR);
        }
    }

    ///..
    public <T> T deserialize(final String object, final TypeReference<T> type)
    throws DeserializationException, IllegalArgumentException {

        if(type == null) {

            log.error(ErrorMessages.METHOD_ILLEGAL_ARGS);
            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.UNCATEGORIZED, ErrorMessages.METHOD_ILLEGAL_ARGS));
        }

        if(object == null) return null;

        try {

            return objectMapper.readValue(object, type);
        }

        catch(final JsonProcessingException exc) {

            log.error("{}: {}", exc.getClass().getSimpleName(), exc.getMessage());
            throw new DeserializationException(ErrorCode.DESERIALIZATION_ERROR);
        }
    }

    ///
}
