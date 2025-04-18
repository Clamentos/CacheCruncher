package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.business.simulation.CacheCommandType;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceBodyDto;
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceDto;

///.
import java.util.List;
import java.util.Map;

///..
import java.util.regex.Pattern;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class CacheTraceValidator extends BasicValidator {

    ///
    private final Pattern rwpPattern;
    private final Pattern digitsPattern;
    private final Pattern invalidatePattern;

    ///
    public CacheTraceValidator() {

        rwpPattern = Pattern.compile("^[A-Z]\\d+ [\\dA-F]{16}$");
        digitsPattern = Pattern.compile("^\\d+$");
        invalidatePattern = Pattern.compile("^[A-Z]+ [\\dA-F]{16}$");
    }

    ///
    public void validateForCreate(CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        this.validateBasic(cacheTraceDto);

        super.requireNull(cacheTraceDto.getId(), "id");
        super.requireNotBlank(cacheTraceDto.getName(), "name");
        super.requireNotBlank(cacheTraceDto.getDescription(), "description");

        CacheTraceBodyDto data = cacheTraceDto.getData();

        super.requireNotNull(data, "data");
        this.validateData(data);
    }

    ///..
    public void validateForUpdate(CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        this.validateBasic(cacheTraceDto);
        super.requireNotNull(cacheTraceDto.getId(), "id");

        String name = cacheTraceDto.getName();
        String description = cacheTraceDto.getDescription();
        CacheTraceBodyDto data = cacheTraceDto.getData();

        if(name != null) super.requireNotBlank(name, "name");
        if(description != null) super.requireNotBlank(description, "description");
        if(data != null) this.validateData(data);
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
                CacheCommandType commandType = CacheCommandType.determineType(command);

                if(commandType == CacheCommandType.REPEAT) {

                    throw new IllegalArgumentException(ErrorFactory.create(

                        ErrorCode.ILLEGAL_COMMAND_TYPE,
                        "DtoValidator.validateData -> Cannot use REPEAT command in sections",
                        CacheCommandType.REPEAT
                    ));
                }

                this.validateCommandSyntax(CacheCommandType.determineType(command), command);
            }
        }

        List<String> trace = data.getTrace();
        super.requireNotNull(trace, "data.trace");

        for(int i = 0; i < trace.size(); i++) {

            String command = trace.get(i);

            super.requireNotBlank(command, "data.trace" + "[" + i + "]");
            this.validateCommandSyntax(CacheCommandType.determineType(command), command);
        }
    }

    ///..
    private void validateBasic(CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        super.requireNotNull(cacheTraceDto, "DTO");
        super.requireNull(cacheTraceDto.getCreatedAt(), "createdAt");
        super.requireNull(cacheTraceDto.getUpdatedAt(), "updatedAt");
    }

    ///..
    private void validateCommandSyntax(CacheCommandType commandType, String command) throws IllegalArgumentException {

        switch(commandType) {

            case READ, WRITE, PREFETCH:

                if(!rwpPattern.matcher(command).matches()) {

                    throw this.fail("CacheTraceValidator.validateCommandSyntax -> Malformed READ, WRITE or PREFETCH command", command);
                }
                
            break;

            case REPEAT:

                String[] splits = command.substring(1).split("#");

                if(splits.length != 2 || !digitsPattern.matcher(splits[0]).matches() || splits[1].isBlank()) {

                    throw this.fail("CacheTraceValidator.validateCommandSyntax -> Malformed REPEAT command", command);
                }

            break;

            case NOOP, FLUSH:

                if(command.length() != 1) {

                    throw this.fail("CacheTraceValidator.validateCommandSyntax -> Malformed NOOP or FLUSH command", command);
                }

            break;

            case INVALIDATE:

                if(!invalidatePattern.matcher(command).matches()) {

                    throw this.fail("CacheTraceValidator.validateCommandSyntax -> Malformed INVALIDATE command", command);
                }

            break;
        }
    }

    ///..
    private IllegalArgumentException fail(String message, String command) {

        return new IllegalArgumentException(ErrorFactory.create(ErrorCode.VALIDATOR_BAD_FORMAT, message, command));
    }

    ///
}
