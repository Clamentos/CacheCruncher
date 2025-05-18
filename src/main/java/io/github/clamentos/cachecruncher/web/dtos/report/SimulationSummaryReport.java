package io.github.clamentos.cachecruncher.web.dtos.report;

///
import java.util.Map;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SimulationSummaryReport<R extends RootReport> {

    ///
    private final boolean failed;
    private final Map<Long, SimulationReport<R>> summary;

    ///
}
