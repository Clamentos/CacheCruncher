package io.github.clamentos.cachecruncher.web.dtos.trace;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import lombok.Getter;

///
@Getter

///
public final class CacheTraceDto {

    ///
    private final Long id;
    private final String name;
    private final String description;
    private final Long createdAt;
    private final Long updatedAt;
    private final CacheTraceBodyDto trace;

    ///
    @JsonCreator
    public CacheTraceDto(

        @JsonProperty("id") final Long id,
        @JsonProperty("name") final String name,
        @JsonProperty("description") final String description,
        @JsonProperty("createdAt") final Long createdAt,
        @JsonProperty("updatedAt") final Long updatedAt,
        @JsonProperty("trace") final CacheTraceBodyDto trace
    ) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.trace = trace;
    }

    ///
}
