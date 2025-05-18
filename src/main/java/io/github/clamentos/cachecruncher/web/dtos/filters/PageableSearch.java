package io.github.clamentos.cachecruncher.web.dtos.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import lombok.Getter;

///
@Getter

///
public sealed class PageableSearch permits ResponseInfoSearchFilter, LogSearchFilter {

    ///
    private final Long lastTimestamp;
    private final Integer count;

    ///
    @JsonCreator
    public PageableSearch(@JsonProperty("lastTimestamp") Long lastTimestamp, @JsonProperty("count") Integer count) {

        this.lastTimestamp = lastTimestamp;
        this.count = count;
    }

    ///
}
