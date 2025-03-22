package io.github.clamentos.cachecruncher.utility;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///.
import com.fasterxml.jackson.core.JsonProcessingException;

///..
import com.fasterxml.jackson.core.type.TypeReference;

///..
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class JsonMapper {

    ///
    private JsonMapper() {}

    ///
    public static String serialize(Object object, ObjectMapper objectMapper) throws IllegalArgumentException {

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
    public static <T> T deserialize(String object, TypeReference<T> type, ObjectMapper objectMapper) throws IllegalArgumentException {

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
