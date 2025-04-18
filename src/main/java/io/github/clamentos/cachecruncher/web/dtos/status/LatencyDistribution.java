package io.github.clamentos.cachecruncher.web.dtos.status;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LatencyDistribution {

    ///
    private final int rangeStart;
    private final int rangeEnd;
    private final long count;

    ///
}
