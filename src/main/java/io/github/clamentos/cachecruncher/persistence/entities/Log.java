package io.github.clamentos.cachecruncher.persistence.entities;

///
import io.github.clamentos.cachecruncher.monitoring.logging.LogLevel;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class Log {

    ///
    private final long id;
    private final long createdAt;
    private final String thread;
    private final String logger;
    private final String message;
    private final LogLevel level;

    ///
}
