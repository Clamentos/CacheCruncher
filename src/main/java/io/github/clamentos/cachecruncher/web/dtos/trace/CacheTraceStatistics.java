package io.github.clamentos.cachecruncher.web.dtos.trace;

///
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommandType;

///.
import java.util.Map;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class CacheTraceStatistics {

    ///
    private final Map<CacheCommandType, Integer> commandDistribution;
    private final Map<String, Integer> addressDistribution;

    ///
}
