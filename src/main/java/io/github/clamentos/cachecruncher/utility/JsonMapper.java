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
public class JsonMapper {

    ///
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public JsonMapper(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    ///
    public String serialize(Object object) throws IllegalArgumentException {

        try {

            return objectMapper.writeValueAsString(object);
        }

        catch(JsonProcessingException exc) {

            log.error("Could not serialize JSON", exc);
            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.SERIALIZATION_ERROR));
        }
    }

    ///..
    public <T> T deserialize(String object, TypeReference<T> type) throws IllegalArgumentException {

        if(object == null || type == null) {

            throw new IllegalArgumentException("Method arguments cannot be null");
        }

        try {

            return objectMapper.readValue(object, type);
        }

        catch(JsonProcessingException exc) {

            log.error("Could not deserialize JSON", exc);
            throw new IllegalArgumentException(new ErrorDetails(ErrorCode.DESERIALIZATION_ERROR));
        }
    }

    ///
}
