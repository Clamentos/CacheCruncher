package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.business.simulation.CommandType;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.CacheTraceDto;

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

        super.requireNotNull(cacheTraceDto.getData(), "data");
        validateData(cacheTraceDto.getData());
    }

    ///..
    public void validateForUpdate(CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        super.requireNotNull(cacheTraceDto, "DTO");

        super.requireNotNull(cacheTraceDto.getId(), "id");
        super.requireNull(cacheTraceDto.getCreatedAt(), "createdAt");
        super.requireNull(cacheTraceDto.getUpdatedAt(), "updatedAt");

        if(cacheTraceDto.getName() != null) super.requireNotBlank(cacheTraceDto.getName(), "name");
        if(cacheTraceDto.getDescription() != null) super.requireNotBlank(cacheTraceDto.getDescription(), "description");

        if(cacheTraceDto.getData() != null) {

            validateData(cacheTraceDto.getData());
        }
    }

    ///.
    private void validateData(CacheTraceBodyDto data) {

        super.requireNotNull(data.getSections(), "data.sections");

        for(Map.Entry<String, List<String>> section : data.getSections().entrySet()) {

            super.requireNotBlank(section.getKey(), "data.sections.key");
            super.requireNotNull(section.getValue(), "data.sections.value");

            for(String command : section.getValue()) {

                super.requireNotBlank(section.getKey(), "data.sections." + section.getKey() + "[]");
                CommandType commandType = CommandType.determineType(command);

                if(commandType == CommandType.REPEAT) {

                    throw new IllegalArgumentException(ErrorFactory.create(

                        ErrorCode.ILLEGAL_COMMAND_TYPE,
                        "DtoValidator.validateData -> Illegal command type",
                        CommandType.REPEAT
                    ));
                }
            }
        }

        super.requireNotNull(data.getTrace(), "data.trace");

        for(String command : data.getTrace()) {

            super.requireNotBlank(command, "data.trace" + "[]");
            CommandType.determineType(command);
        }
    }

    ///
}
