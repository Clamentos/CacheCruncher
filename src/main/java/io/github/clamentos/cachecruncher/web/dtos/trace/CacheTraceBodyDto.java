package io.github.clamentos.cachecruncher.web.dtos.trace;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.utility.MultiValueMap;

///.
import java.util.List;

///.
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class CacheTraceBodyDto {

    ///
    private CacheTraceStatistics statistics;

    ///..
    private final MultiValueMap<String, String> sections;
    private final List<String> body;

    ///
    @JsonCreator
    public CacheTraceBodyDto(

        @JsonProperty("sections") final MultiValueMap<String, String> sections,
        @JsonProperty("body") final List<String> body
    ) {

        statistics = null;

        this.sections = sections;
        this.body = body;
    }

    ///
}
