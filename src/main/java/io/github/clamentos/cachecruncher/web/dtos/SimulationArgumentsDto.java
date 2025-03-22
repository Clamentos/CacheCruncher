package io.github.clamentos.cachecruncher.web.dtos;

///
import java.util.List;
import java.util.Set;

///.
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class SimulationArgumentsDto {

    ///
    private Set<Long> traceIds;
    private List<CacheConfigurationDto> cacheConfigurations;

    ///
}
