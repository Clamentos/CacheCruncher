package io.github.clamentos.cachecruncher.web.dtos.simulation;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.business.simulation.SimulationFlag;

///.
import java.util.Set;

///.
import lombok.Getter;

///
@Getter

///
public final class CacheSimulationArgumentsDto {

    ///
    private final Set<Long> traceIds;
    private final Set<CacheConfigurationDto> cacheConfigurations;
    private final Set<SimulationFlag> simulationFlags;

    ///
    @JsonCreator
    public CacheSimulationArgumentsDto(

        @JsonProperty("traceIds") final Set<Long> traceIds,
        @JsonProperty("cacheConfigurations") final Set<CacheConfigurationDto> cacheConfigurations,
        @JsonProperty("simulationFlags") final Set<SimulationFlag> simulationFlags
    ) {

        this.traceIds = traceIds;
        this.cacheConfigurations = cacheConfigurations;
        this.simulationFlags = simulationFlags;
    }

    ///
}
