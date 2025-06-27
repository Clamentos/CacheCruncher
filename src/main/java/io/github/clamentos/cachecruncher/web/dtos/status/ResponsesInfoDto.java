package io.github.clamentos.cachecruncher.web.dtos.status;

///
import java.util.Map;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///.
import org.springframework.http.HttpStatus;

///
@AllArgsConstructor
@Getter

///
public final class ResponsesInfoDto {

    ///
    private final Map<String, Integer> uriIdMappings;
    private final Map<Long, Map<Integer, Map<HttpStatus, Map<String, Integer>>>> metrics;

    ///
}
