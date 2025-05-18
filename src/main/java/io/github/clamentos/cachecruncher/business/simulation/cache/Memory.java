package io.github.clamentos.cachecruncher.business.simulation.cache;

///
import io.github.clamentos.cachecruncher.business.simulation.Simulatable;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.MemorySimulationReportDto;

///
public interface Memory extends Simulatable {

    ///
    long getAccessTime();
    MemorySimulationReportDto getMemorySimulationReportDto();

    ///..
    long read(long address);
    long write(long address);

    ///
}
