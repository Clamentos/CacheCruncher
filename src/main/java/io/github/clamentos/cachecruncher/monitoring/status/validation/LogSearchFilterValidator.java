package io.github.clamentos.cachecruncher.monitoring.status.validation;

///
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

///..
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///..
import io.github.clamentos.cachecruncher.web.dtos.filters.LogSearchFilterDto;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class LogSearchFilterValidator extends BasicValidator {

    ///
    public void validate(final LogSearchFilterDto searchFilter) throws ValidationException {

        super.requireNotNull(searchFilter, "DTO");

        super.requireNotNull(searchFilter.getLastTimestamp(), "lastTimestamp");
        super.requireNotNull(searchFilter.getCount(), "count");
        super.requireBetween(searchFilter.getCount(), 1, 100, "count");
        super.requireNotNull(searchFilter.getCreatedAtStart(), "createdAtStart");
        super.requireNotNull(searchFilter.getCreatedAtEnd(), "createdAtEnd");
        super.requireNotEmpty(searchFilter.getLevels(), "levels");
        super.requireNotNullAll(searchFilter.getLevels(), "levels");
        super.requireNotNull(searchFilter.getThreadLike(), "threadLike");
        super.requireNotNull(searchFilter.getLoggerLike(), "loggerLike");
        super.requireNotNull(searchFilter.getMessageLike(), "messageLike");
    }

    ///
}
