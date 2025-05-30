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
public final class ResponsesInfo {

    ///
    private final Map<String, Integer> uriIdMap;
    private final Map<Integer, Map<Integer, Map<HttpStatus, Map<String, Integer>>>> metrics;

    ///
}
