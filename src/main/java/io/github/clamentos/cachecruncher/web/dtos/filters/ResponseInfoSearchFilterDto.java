package io.github.clamentos.cachecruncher.web.dtos.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import lombok.Getter;

///
@Getter

///
public final class ResponseInfoSearchFilterDto extends PageableSearchDto {

    ///
    private final Long createdAtStart;
    private final Long createdAtEnd;

    ///
    @JsonCreator
    public ResponseInfoSearchFilterDto(

        @JsonProperty("lastTimestamp") final Long lastTimestamp,
        @JsonProperty("count") final Integer count,

        @JsonProperty("createdAtStart") final Long createdAtStart,
        @JsonProperty("createdAtEnd") final Long createdAtEnd
    ) {

        super(lastTimestamp, count);

        this.createdAtStart = createdAtStart;
        this.createdAtEnd = createdAtEnd;
    }

    ///
}
