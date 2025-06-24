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
    public String serialize(final Object object) throws IllegalArgumentException {

        try {

            return objectMapper.writeValueAsString(object);
        }

        catch(final JsonProcessingException exc) {

            log.error("Could not serialize JSON", exc);
            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.SERIALIZATION_ERROR));
        }
    }

    ///..
    public <T> T deserialize(final String object, final TypeReference<T> type) throws IllegalArgumentException {

        if(type == null) {

            log.error(ErrorMessages.METHOD_ILLEGAL_ARGS);
            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.GENERIC, ErrorMessages.METHOD_ILLEGAL_ARGS));
        }

        if(object == null) return null;

        try {

            return objectMapper.readValue(object, type);
        }

        catch(final JsonProcessingException exc) {

            log.error("Could not deserialize JSON", exc);
            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.DESERIALIZATION_ERROR));
        }
    }

    ///
}
