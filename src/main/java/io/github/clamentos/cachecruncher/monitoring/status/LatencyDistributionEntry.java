package io.github.clamentos.cachecruncher.monitoring.status;

///
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LatencyDistributionEntry {

    ///
    private final short lowerBound;
    private final short upperBound;
    private final AtomicInteger count;

    ///
}
