package io.github.clamentos.cachecruncher.web.dtos.trace;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import java.util.List;
import java.util.Map;

///.
import lombok.Getter;

///
@Getter

///
public final class CacheTraceBodyDto {

    ///
    private final Map<String, List<String>> sections;
    private final List<String> body;

    ///
    @JsonCreator
    public CacheTraceBodyDto(

        @JsonProperty("sections") Map<String, List<String>> sections,
        @JsonProperty("body") List<String> body
    ) {

        this.sections = sections;
        this.body = body;
    }

    ///
}
