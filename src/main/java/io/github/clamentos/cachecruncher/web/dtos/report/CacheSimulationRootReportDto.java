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
public final class CacheSimulationRootReportDto {

    ///
    private final SimulationStatus status;

    ///..
    private final long beginTimestamp;
    private final long endTimestamp;

    ///..
    private final double averageMemoryAccessTime;
    private final CacheSimulationReportDto report;

    ///
    public static CacheSimulationRootReportDto newRejected() {

        long now = System.currentTimeMillis();
        return new CacheSimulationRootReportDto(SimulationStatus.REJECTED, now, now, -1, null);
    }

    ///
}
