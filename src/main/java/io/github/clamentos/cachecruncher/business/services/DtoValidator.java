package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.business.simulation.CommandType;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;
import io.github.clamentos.cachecruncher.web.dtos.CacheConfigurationDto;
///..
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceDto;
import io.github.clamentos.cachecruncher.web.dtos.SimulationArgumentsDto;

import java.util.Collection;
///.
import java.util.List;
import java.util.Map;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class DtoValidator extends BasicValidator {

    ///
    public void validateForCreate(CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        super.requireNotNull(cacheTraceDto, "DTO");

        super.requireNull(cacheTraceDto.getId(), "id");
        super.requireNotBlank(cacheTraceDto.getName(), "name");
        super.requireNotBlank(cacheTraceDto.getDescription(), "description");
        super.requireNull(cacheTraceDto.getCreatedAt(), "createdAt");
        super.requireNull(cacheTraceDto.getUpdatedAt(), "updatedAt");

        CacheTraceBodyDto data = cacheTraceDto.getData();

        super.requireNotNull(data, "data");
        this.validateData(data);
    }

    ///..
    public void validateForUpdate(CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        super.requireNotNull(cacheTraceDto, "DTO");

        super.requireNotNull(cacheTraceDto.getId(), "id");
        super.requireNull(cacheTraceDto.getCreatedAt(), "createdAt");
        super.requireNull(cacheTraceDto.getUpdatedAt(), "updatedAt");

        String name = cacheTraceDto.getName();
        String description = cacheTraceDto.getDescription();
        CacheTraceBodyDto data = cacheTraceDto.getData();

        if(name != null) super.requireNotBlank(name, "name");
        if(description != null) super.requireNotBlank(description, "description");
        if(data != null) this.validateData(data);
    }

    ///..
    public void validate(SimulationArgumentsDto simulationArgumentsDto) throws IllegalArgumentException {

        super.requireNotNull(simulationArgumentsDto, "DTO");

        super.requireNotNull(simulationArgumentsDto.getRamAccessTime(), "ramAccessTime");
        super.requireNotNullAll(simulationArgumentsDto.getTraceIds(), "traceIds");
        super.requireNotNullAll(simulationArgumentsDto.getSimulationFlags(), "simulationFlags");

        this.validateConfigurations(simulationArgumentsDto.getCacheConfigurations());
    }

    ///.
    private void validateData(CacheTraceBodyDto data) throws IllegalArgumentException {

        super.requireNotNull(data.getSections(), "data.sections");

        for(Map.Entry<String, List<String>> section : data.getSections().entrySet()) {

            String key = section.getKey();
            List<String> value = section.getValue();

            super.requireNotBlank(key, "data.sections.key");
            super.requireNotNull(value, "data.sections.value");

            for(int i = 0; i < value.size(); i++) {

                String command = value.get(i);

                super.requireNotBlank(command, "data.sections.value" + "[" + i + "]");
                CommandType commandType = CommandType.determineType(command);

                if(commandType == CommandType.REPEAT) {

                    throw new IllegalArgumentException(ErrorFactory.create(

                        ErrorCode.ILLEGAL_COMMAND_TYPE,
                        "DtoValidator.validateData -> Cannot use REPEAT command in sections",
                        CommandType.REPEAT
                    ));
                }
            }
        }

        List<String> trace = data.getTrace();
        super.requireNotNull(trace, "data.trace");

        for(int i = 0; i < trace.size(); i++) {

            String command = trace.get(i);

            super.requireNotBlank(command, "data.trace" + "[" + i + "]");
            CommandType.determineType(command);
            // TODO: fully validate command (also check that repeat commands point to existing sections)
        }
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
        super.requirePositive(cacheConfiguration.getAccessTime(), prefix + ".accessTime");
        super.requirePositive(cacheConfiguration.getNumSetsExp(), prefix + ".numSetsExp");
        super.requirePositive(cacheConfiguration.getLineSizeExp(), prefix + ".lineSizeExp");
        super.requirePositive(cacheConfiguration.getAssociativity(), prefix + ".associativity");

        if(cacheConfiguration.getAssociativity() > 0) {

            super.requireNotNull(cacheConfiguration.getReplacementPolicyType(), prefix + ".replacementPolicyType");
        }

        if(cacheConfiguration.getNextLevelConfiguration() != null) {

            this.validateConfiguration(cacheConfiguration, prefix + ".nextLevelConfiguration");
        }
    }

    ///
}
