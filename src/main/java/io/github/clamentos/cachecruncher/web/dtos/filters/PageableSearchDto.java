package io.github.clamentos.cachecruncher.web.dtos.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import lombok.Getter;

///
@Getter

///
public sealed class PageableSearchDto permits RangeSearchFilterDto, LogSearchFilterDto {

    ///
    private final Long lastTimestamp;
    private final Integer count;

    ///
    @JsonCreator
    public PageableSearchDto(@JsonProperty("lastTimestamp") final Long lastTimestamp, @JsonProperty("count") final Integer count) {

        this.lastTimestamp = lastTimestamp;
        this.count = count;
    }

    ///
}
