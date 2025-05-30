package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

///..
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///..
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheConfigurationDto;
import io.github.clamentos.cachecruncher.web.dtos.simulation.CacheSimulationArgumentsDto;
import io.github.clamentos.cachecruncher.web.dtos.simulation.MemoryConfigurationDto;

///.
import java.util.Set;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class SimulationArgumentsValidator extends BasicValidator {

    ///
    private static final String REPLACEMENT_POLICY_TYPE_FIELD = ".replacementPolicyType";

    ///
    public void validate(final CacheSimulationArgumentsDto simulationArgumentsDto) throws IllegalArgumentException {

        super.requireNotNull(simulationArgumentsDto, "DTO");
        final Set<Long> traceIds = simulationArgumentsDto.getTraceIds();

        super.requireNotNullAll(traceIds, "traceIds");
        super.requireBetween(traceIds.size(), 1, 8, "traceIds");
        super.requireNotNullAll(simulationArgumentsDto.getSimulationFlags(), "simulationFlags");
        this.validateConfigurations(simulationArgumentsDto.getCacheConfigurations());
    }

    ///..
    private void validateConfigurations(final Set<CacheConfigurationDto> cacheConfigurations) throws IllegalArgumentException {

        int i = 0;

        super.requireNotEmpty(cacheConfigurations, "cacheConfigurations");
        super.requireBetween(cacheConfigurations.size(), 1, 8, "cacheConfigurations");

        for(final CacheConfigurationDto cacheConfiguration : cacheConfigurations) {

            this.validateConfiguration(cacheConfiguration, "cacheConfigurations[" + i + "]");
            i++;
        }
    }

    ///..
    private void validateConfiguration(final MemoryConfigurationDto memoryConfiguration, final String prefix)
    throws IllegalArgumentException {

        super.requireNotNull(memoryConfiguration, prefix);

        if(memoryConfiguration instanceof CacheConfigurationDto cacheConfiguration) {

            super.requireNotBlank(cacheConfiguration.getName(), prefix + ".name");
            super.requireBetween(cacheConfiguration.getAccessTime(), 0L, Long.MAX_VALUE, prefix + ".accessTime");
            super.requireBetween(cacheConfiguration.getNumSetsExp(), 0, 16, prefix + ".numSetsExp");
            super.requireBetween(cacheConfiguration.getLineSizeExp(), 0, 63, prefix + ".lineSizeExp");

            final Integer associativity = cacheConfiguration.getAssociativity();
            final ReplacementPolicyType replacementPolicyType = cacheConfiguration.getReplacementPolicyType();
            super.requireBetween(associativity, 1, 65536, prefix + ".associativity");

            if(associativity > 1) {

                super.requireNotNull(replacementPolicyType, prefix + REPLACEMENT_POLICY_TYPE_FIELD);

                if(replacementPolicyType == ReplacementPolicyType.NOOP) {

                    throw super.fail("Replacement policy type cannot be NOOP", prefix + REPLACEMENT_POLICY_TYPE_FIELD);
                }
            }

            else {

                if(replacementPolicyType != null && replacementPolicyType != ReplacementPolicyType.NOOP) {

                    throw super.fail("Replacement policy must be NOOP", prefix + REPLACEMENT_POLICY_TYPE_FIELD);
                }
            }

            if(cacheConfiguration.getNextLevelConfiguration() != null) {

                this.validateConfiguration(cacheConfiguration.getNextLevelConfiguration(), prefix + ".nextLevelConfiguration");
            }
        }

        else {

            super.requireBetween(memoryConfiguration.getAccessTime(), 0L, Long.MAX_VALUE, prefix + ".accessTime");
        }
    }

    ///
}
