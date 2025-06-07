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
public final class SimulationSummaryReport<R extends RootReportDto> {

    ///
    private final boolean hasErrors;
    private final Map<Long, SimulationReport<R>> summary;

    ///
}
