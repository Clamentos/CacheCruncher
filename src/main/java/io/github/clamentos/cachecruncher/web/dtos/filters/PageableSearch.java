package io.github.clamentos.cachecruncher.web.dtos.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import lombok.Getter;

///
@Getter
@JsonIgnoreProperties(ignoreUnknown = false)

///
public class PageableSearch {

    ///
    private final Long lastTimestamp;
    private final Integer count;

    ///
    @JsonCreator
    public PageableSearch(

        @JsonProperty("lastTimestamp") Long lastTimestamp,
        @JsonProperty("count") Integer count
    ) {

        this.lastTimestamp = lastTimestamp;
        this.count = count;
    }

    ///
}
