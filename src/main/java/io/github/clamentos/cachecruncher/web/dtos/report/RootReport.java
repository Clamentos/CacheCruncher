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
public abstract sealed class RootReport permits CacheSimulationRootReportDto {

    ///
    private final SimulationStatus status;

    ///..
    private final long beginTimestamp;
    private final long endTimestamp;

    ///
}
