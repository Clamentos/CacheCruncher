package io.github.clamentos.cachecruncher.web.dtos.status;

///
import java.util.List;
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
    private final Map<String, Map<HttpStatus, Long>> responseStatusCounts;
    private final Map<String, List<LatencyDistribution>> latencyDistributions;

    ///
}
