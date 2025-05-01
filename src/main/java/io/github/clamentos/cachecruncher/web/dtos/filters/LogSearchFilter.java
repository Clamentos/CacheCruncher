package io.github.clamentos.cachecruncher.web.dtos.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.monitoring.logging.LogLevel;

///.
import java.util.Set;

///.
import lombok.Getter;

///
@Getter
@JsonIgnoreProperties(ignoreUnknown = false)

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

        @JsonProperty("lastTimestamp") Long lastTimestamp,
        @JsonProperty("count") Integer count,
        @JsonProperty("createdAtStart") Long createdAtStart,
        @JsonProperty("createdAtEnd") Long createdAtEnd,
        @JsonProperty("levels") Set<LogLevel> levels,
        @JsonProperty("threadLike") String threadLike,
        @JsonProperty("loggerLike") String loggerLike,
        @JsonProperty("messageLike") String messageLike
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
