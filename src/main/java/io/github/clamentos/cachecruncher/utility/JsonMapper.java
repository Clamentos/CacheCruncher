package io.github.clamentos.cachecruncher.utility;

///
import com.fasterxml.jackson.core.JsonProcessingException;

///..
import com.fasterxml.jackson.core.type.TypeReference;

///..
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

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

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "JsonMapper.serialize -> Could not serialize JSON"
            ));
        }
    }

    ///..
    public <T> T deserialize(String object, TypeReference<T> type) throws IllegalArgumentException {

        if(object == null || type == null) {

            throw new IllegalArgumentException("Arguments cannot be null");
        }

        try {

            return objectMapper.readValue(object, type);
        }

        catch(JsonProcessingException exc) {

            log.error("Could not serialize JSON", exc);

            throw new IllegalArgumentException(ErrorFactory.create(

                ErrorCode.VALIDATOR_BAD_FORMAT,
                "JsonMapper.deserialize -> Could not deserialize JSON"
            ));
        }
    }

    ///
}
