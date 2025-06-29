package io.github.clamentos.cachecruncher.web.dtos.trace;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import com.fasterxml.jackson.core.type.TypeReference;

///.
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTrace;
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;

///.
import lombok.Getter;

///
@Getter

///
public final class CacheTraceDto {

    ///
    private static final TypeReference<CacheTraceStatistics> cacheTraceStatisticsType = new TypeReference<>(){};

    ///.
    private final Long id;
    private final String name;
    private final String description;
    private final Long createdAt;
    private final Long updatedAt;
    private final CacheTraceStatistics statistics;
    private final CacheTraceBody trace;

    ///
    @JsonCreator
    public CacheTraceDto(

        @JsonProperty("id") final Long id,
        @JsonProperty("name") final String name,
        @JsonProperty("description") final String description,
        @JsonProperty("createdAt") final Long createdAt,
        @JsonProperty("updatedAt") final Long updatedAt,
        @JsonProperty("statistics") CacheTraceStatistics statistics,
        @JsonProperty("trace") final CacheTraceBody trace
    ) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.statistics = statistics;
        this.trace = trace;
    }

    ///..
    public CacheTraceDto(CacheTrace cacheTrace, JsonMapper mapper) throws DeserializationException {

        id = cacheTrace.getId();
        name = cacheTrace.getName();
        description = cacheTrace.getDescription();
        createdAt = cacheTrace.getCreatedAt();
        updatedAt = cacheTrace.getUpdatedAt();
        statistics = mapper.deserialize(cacheTrace.getStatistics(), cacheTraceStatisticsType);
        trace = cacheTrace.getTrace();
    }

    ///
}
