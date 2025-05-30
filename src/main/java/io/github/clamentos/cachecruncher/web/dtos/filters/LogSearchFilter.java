package io.github.clamentos.cachecruncher.web.dtos.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.monitoring.logging.LogLevel;

///.
import java.util.Set;

///.
import lombok.Getter;

///
@Getter

///
public final class LogSearchFilter extends PageableSearch {

    ///
    private final Long createdAtStart;
    private final Long createdAtEnd;
    private final Set<LogLevel> levels;
    private final String threadLike;
    private final String loggerLike;
    private final String messageLike;

    ///
    @JsonCreator
    public LogSearchFilter(

        @JsonProperty("lastTimestamp") final Long lastTimestamp,
        @JsonProperty("count") final Integer count,
        @JsonProperty("createdAtStart") final Long createdAtStart,
        @JsonProperty("createdAtEnd") final Long createdAtEnd,
        @JsonProperty("levels") final Set<LogLevel> levels,
        @JsonProperty("threadLike") final String threadLike,
        @JsonProperty("loggerLike") final String loggerLike,
        @JsonProperty("messageLike") final String messageLike
    ) {

        super(lastTimestamp, count);

        this.createdAtStart = createdAtStart;
        this.createdAtEnd = createdAtEnd;
        this.levels = levels;
        this.threadLike = threadLike;
        this.loggerLike = loggerLike;
        this.messageLike = messageLike;
    }

    ///
}
