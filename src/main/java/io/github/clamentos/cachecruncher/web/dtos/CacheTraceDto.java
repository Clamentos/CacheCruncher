package io.github.clamentos.cachecruncher.web.dtos;

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
public final class CacheTraceDto {

    ///
    private final Long id;
    private final String name;
    private final String description;
    private final Long createdAt;
    private final Long updatedAt;
    private final CacheTraceBodyDto data;

    ///
    @JsonCreator
    public CacheTraceDto(

        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("createdAt") Long createdAt,
        @JsonProperty("updatedAt") Long updatedAt,
        @JsonProperty("data") CacheTraceBodyDto data
    ) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.data = data;
    }

    ///
}
