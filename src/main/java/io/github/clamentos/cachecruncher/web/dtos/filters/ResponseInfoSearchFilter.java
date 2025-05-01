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
public final class ResponseInfoSearchFilter extends PageableSearch {

    ///
    private final Long createdAtStart;
    private final Long createdAtEnd;

    ///
    @JsonCreator
    public ResponseInfoSearchFilter(

        @JsonProperty("lastTimestamp") Long lastTimestamp,
        @JsonProperty("count") Integer count,
        @JsonProperty("createdAtStart") Long createdAtStart,
        @JsonProperty("createdAtEnd") Long createdAtEnd
    ) {

        super(lastTimestamp, count);

        this.createdAtStart = createdAtStart;
        this.createdAtEnd = createdAtEnd;
    }

    ///
}
