package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheSimulationArgumentsDto;

///.
import java.util.Collection;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class SimulationArgumentsValidator extends BasicValidator {

    ///
    public void validate(CacheSimulationArgumentsDto simulationArgumentsDto) throws IllegalArgumentException {

        super.requireNotNull(simulationArgumentsDto, "DTO");

        super.requireNotNull(simulationArgumentsDto.getRamAccessTime(), "ramAccessTime");
        super.requireNotNullAll(simulationArgumentsDto.getTraceIds(), "traceIds");
        super.requireNotNullAll(simulationArgumentsDto.getSimulationFlags(), "simulationFlags");

        this.validateConfigurations(simulationArgumentsDto.getCacheConfigurations());
    }

    ///..
    private void validateConfigurations(Collection<CacheConfigurationDto> cacheConfigurations) throws IllegalArgumentException {

        int i = 0;
        super.requireNotEmpty(cacheConfigurations, "cacheConfigurations");

        for(CacheConfigurationDto cacheConfiguration : cacheConfigurations) {

            this.validateConfiguration(cacheConfiguration, "cacheConfigurations[" + i + "]");
            i++;
        }
    }

    ///..
    private void validateConfiguration(CacheConfigurationDto cacheConfiguration, String prefix) throws IllegalArgumentException {

        super.requireNotNull(cacheConfiguration, prefix);
        super.requireNotBlank(cacheConfiguration.getName(), prefix + ".name");
        super.requireGreaterOrEqual(cacheConfiguration.getAccessTime(), 0, prefix + ".accessTime");
        super.requireGreaterOrEqual(cacheConfiguration.getNumSetsExp(), 0, prefix + ".numSetsExp");
        super.requireGreaterOrEqual(cacheConfiguration.getLineSizeExp(), 0, prefix + ".lineSizeExp");
        super.requireGreaterOrEqual(cacheConfiguration.getAssociativity(), 1, prefix + ".associativity");

        if(cacheConfiguration.getAssociativity() > 0) {

            super.requireNotNull(cacheConfiguration.getReplacementPolicyType(), prefix + ".replacementPolicyType");
        }

        if(cacheConfiguration.getNextLevelConfiguration() != null) {

            this.validateConfiguration(cacheConfiguration, prefix + ".nextLevelConfiguration");
        }
    }

    ///
}
