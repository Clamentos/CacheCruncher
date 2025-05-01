package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.utility.BasicValidator;

///..
import io.github.clamentos.cachecruncher.web.dtos.filters.ResponseInfoSearchFilter;

///.
import org.springframework.stereotype.Service;

///
@Service

///
public class ResponseInfoSearchFilterValidator extends BasicValidator {

    ///
    public void validate(ResponseInfoSearchFilter searchFilter) throws IllegalArgumentException {

        super.requireNotNull(searchFilter, "DTO");

        super.requireNotNull(searchFilter.getLastTimestamp(), "lastTimestamp");
        super.requireNotNull(searchFilter.getCount(), "count");
        super.requireBetween(searchFilter.getCount(), 1, 1000, "count");
        super.requireNotNull(searchFilter.getCreatedAtStart(), "createdAtStart");
        super.requireNotNull(searchFilter.getCreatedAtEnd(), "createdAtEnd");
    }

    ///
}
