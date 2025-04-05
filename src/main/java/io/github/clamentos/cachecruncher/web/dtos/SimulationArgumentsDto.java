package io.github.clamentos.cachecruncher.web.dtos;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.business.simulation.SimulationFlag;

///.
import java.util.Set;

///.
import lombok.Getter;

///
@Getter
@JsonIgnoreProperties(ignoreUnknown = false)

///
public final class SimulationArgumentsDto {

    ///
    private final Integer ramAccessTime;
    private final Set<Long> traceIds;
    private final Set<CacheConfigurationDto> cacheConfigurations;
    private final Set<SimulationFlag> simulationFlags;

    ///
    @JsonCreator
    public SimulationArgumentsDto(

        @JsonProperty("ramAccessTime") Integer ramAccessTime,
        @JsonProperty("traceIds") Set<Long> traceIds,
        @JsonProperty("cacheConfigurations") Set<CacheConfigurationDto> cacheConfigurations,
        @JsonProperty("simulationFlags") Set<SimulationFlag> simulationFlags
    ) {

        this.ramAccessTime = ramAccessTime;
        this.traceIds = traceIds;
        this.cacheConfigurations = cacheConfigurations;
        this.simulationFlags = simulationFlags;
    }

    ///
}
