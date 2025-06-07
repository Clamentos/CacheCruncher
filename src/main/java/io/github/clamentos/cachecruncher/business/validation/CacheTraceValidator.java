package io.github.clamentos.cachecruncher.business.validation;

///
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///..
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///..
import io.github.clamentos.cachecruncher.web.dtos.trace.CacheTraceDto;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class CacheTraceValidator extends BasicValidator {

    ///
    public void validateForCreate(final CacheTraceDto cacheTraceDto) throws ValidationException {

        this.validateBasic(cacheTraceDto);

        super.requireNull(cacheTraceDto.getId(), "id");
        super.requireNotBlank(cacheTraceDto.getName(), "name");
        super.requireNotBlank(cacheTraceDto.getDescription(), "description");
        super.requireNull(cacheTraceDto.getStatistics(), "statistics");

        final CacheTraceBody trace = cacheTraceDto.getTrace();

        super.requireNotNull(trace, "trace");
        this.validateTraceBody(trace);
    }

    ///..
    public void validateForUpdate(final CacheTraceDto cacheTraceDto) throws ValidationException {

        this.validateBasic(cacheTraceDto);
        super.requireNotNull(cacheTraceDto.getId(), "id");
        super.requireNull(cacheTraceDto.getStatistics(), "statistics");

        final String name = cacheTraceDto.getName();
        final String description = cacheTraceDto.getDescription();
        final CacheTraceBody trace = cacheTraceDto.getTrace();

        if(name != null) super.requireNotBlank(name, "name");
        if(description != null) super.requireNotBlank(description, "description");
        if(trace != null) this.validateTraceBody(trace);
    }

    ///.
    private void validateBasic(final CacheTraceDto cacheTraceDto) throws ValidationException {

        super.requireNotNull(cacheTraceDto, "DTO");

        super.requireNull(cacheTraceDto.getCreatedAt(), "createdAt");
        super.requireNull(cacheTraceDto.getUpdatedAt(), "updatedAt");
    }

    ///..
    private void validateTraceBody(final CacheTraceBody trace) throws ValidationException {

        super.requireNotNull(trace.getSections(), "trace.sections");
        super.requireNotNull(trace.getBody(), "trace.body");
    }

    ///
}
