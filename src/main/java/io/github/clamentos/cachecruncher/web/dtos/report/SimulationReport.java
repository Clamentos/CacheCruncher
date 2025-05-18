package io.github.clamentos.cachecruncher.web.dtos.report;

///
import io.github.clamentos.cachecruncher.business.simulation.SimulationStatus;

///.
import java.util.Map;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SimulationReport<R extends RootReport> {

    ///
    private final SimulationStatus status;
    private final Map<String, R> report;

    ///
}
