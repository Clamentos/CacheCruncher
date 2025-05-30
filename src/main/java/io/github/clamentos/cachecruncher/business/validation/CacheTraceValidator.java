package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.business.simulation.cache.CacheCommandType;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.utility.BasicValidator;
import io.github.clamentos.cachecruncher.utility.MultiValueMap;

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

        rwpPattern = Pattern.compile("^[A-Z]\\d{1,9} [\\dA-F]{16}$");
        digitsPattern = Pattern.compile("^\\d{1,9}$");
        invalidatePattern = Pattern.compile("^[A-Z] [\\dA-F]{16}$");
    }

    ///
    public void validateForCreate(final CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        this.validateBasic(cacheTraceDto);

        super.requireNull(cacheTraceDto.getId(), "id");
        super.requireNotBlank(cacheTraceDto.getName(), "name");
        super.requireNotBlank(cacheTraceDto.getDescription(), "description");

        final CacheTraceBodyDto trace = cacheTraceDto.getTrace();

        super.requireNotNull(trace, "trace");
        this.validateTraceBody(trace);
    }

    ///..
    public void validateForUpdate(final CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        this.validateBasic(cacheTraceDto);
        super.requireNotNull(cacheTraceDto.getId(), "id");

        final String name = cacheTraceDto.getName();
        final String description = cacheTraceDto.getDescription();
        final CacheTraceBodyDto trace = cacheTraceDto.getTrace();

        if(name != null) super.requireNotBlank(name, "name");
        if(description != null) super.requireNotBlank(description, "description");
        if(trace != null) this.validateTraceBody(trace);
    }

    ///.
    private void validateBasic(final CacheTraceDto cacheTraceDto) throws IllegalArgumentException {

        super.requireNotNull(cacheTraceDto, "DTO");

        super.requireNull(cacheTraceDto.getCreatedAt(), "createdAt");
        super.requireNull(cacheTraceDto.getUpdatedAt(), "updatedAt");
    }

    ///..
    private void validateTraceBody(final CacheTraceBodyDto trace) throws IllegalArgumentException {

        final MultiValueMap<String, String> sections = trace.getSections();
        super.requireNotNull(sections, "trace.sections");

        for(final Map.Entry<String, List<String>> section : sections.entrySet()) {

            final String key = section.getKey();
            final List<String> value = section.getValue();

            super.requireNotBlank(key, "trace.sections.key");
            super.requireNotEmpty(value, "trace.sections.value");

            for(int i = 0; i < value.size(); i++) {

                final String command = value.get(i);
                super.requireNotBlank(command, "trace.sections.value" + "[" + i + "]");
                final CacheCommandType commandType = CacheCommandType.determineType(command);

                if(commandType == CacheCommandType.REPEAT) {

                    throw new IllegalArgumentException(new ErrorDetails(

                        ErrorCode.ILLEGAL_COMMAND_TYPE,
                        "Cannot use REPEAT command in sections"
                    ));
                }

                this.validateCommandSyntax(CacheCommandType.determineType(command), command);
            }
        }

        final List<String> body = trace.getBody();
        super.requireNotNull(body, "trace.body");

        for(int i = 0; i < body.size(); i++) {

            final String command = body.get(i);

            super.requireNotBlank(command, "trace.body" + "[" + i + "]");
            this.validateCommandSyntax(CacheCommandType.determineType(command), command);
        }
    }

    ///..
    private void validateCommandSyntax(final CacheCommandType commandType, final String command) throws IllegalArgumentException {

        switch(commandType) {

            case READ, WRITE, PREFETCH:

                if(!rwpPattern.matcher(command).matches()) {

                    throw super.fail("Malformed READ, WRITE or PREFETCH command", command);
                }
                
            break;

            case REPEAT:

                final String[] splits = command.substring(1).split("#");

                if(splits.length != 2 || !digitsPattern.matcher(splits[0]).matches() || splits[1].isBlank()) {

                    throw super.fail("Malformed REPEAT command", command);
                }

            break;

            case NOOP:

                if(command.length() > 1 && !digitsPattern.matcher(command.substring(1)).matches()) {

                    throw super.fail("Malformed NOOP command", command);
                }

            break;

            case FLUSH:

                if(command.length() != 1) {

                    throw super.fail("Malformed FLUSH command", command);
                }

            break;

            case INVALIDATE:

                if(!invalidatePattern.matcher(command).matches()) {

                    throw super.fail("Malformed INVALIDATE command", command);
                }

            break;
        }
    }

    ///
}
