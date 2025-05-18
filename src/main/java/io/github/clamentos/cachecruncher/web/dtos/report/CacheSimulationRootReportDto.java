package io.github.clamentos.cachecruncher.web.dtos.report;

///
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class CacheSimulationRootReportDto implements RootReport {

    ///
    private final SimulationStatus status;

    ///..
    private final long beginTimestamp;
    private final long endTimestamp;

    ///..
    private final double averageMemoryAccessTime;
    private final CacheSimulationReportDto report;

    ///
}
