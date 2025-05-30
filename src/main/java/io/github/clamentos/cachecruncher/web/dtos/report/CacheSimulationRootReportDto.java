package io.github.clamentos.cachecruncher.web.dtos.report;

///
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///.
import lombok.Getter;

///
@Getter

///
public final class CacheSimulationRootReportDto extends RootReport {

    ///
    private final double averageMemoryAccessTime;
    private final CacheSimulationReportDto report;

    ///
    public CacheSimulationRootReportDto(

        final SimulationStatus status,
        final long beginTimestamp,
        final long endTimestamp,
        final double averageMemoryAccessTime,
        final CacheSimulationReportDto report
    ) {

        super(status, beginTimestamp, endTimestamp);

        this.averageMemoryAccessTime = averageMemoryAccessTime;
        this.report = report;
    }

    ///
}
