package io.github.clamentos.cachecruncher.web.dtos;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import java.util.List;
import java.util.Map;

///.
import lombok.Getter;

///
@Getter
@JsonIgnoreProperties(ignoreUnknown = false)

///
public final class CacheTraceBodyDto {

    ///
    private final Map<String, List<String>> sections;
    private final List<String> trace;

    ///
    @JsonCreator
    public CacheTraceBodyDto(

        @JsonProperty("sections") Map<String, List<String>> sections,
        @JsonProperty("trace") List<String> trace
    ) {

        this.sections = sections;
        this.trace = trace;
    }

    ///
}
