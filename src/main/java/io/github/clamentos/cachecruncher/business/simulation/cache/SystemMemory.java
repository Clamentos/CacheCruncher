package io.github.clamentos.cachecruncher.business.simulation.cache;

///
import io.github.clamentos.cachecruncher.business.simulation.event.EventManager;

///..
import io.github.clamentos.cachecruncher.web.dtos.report.MemorySimulationReportDto;

///.
import lombok.Getter;

///
@Getter

///
public final class SystemMemory implements Memory {

    ///
    private final long accessTime;
    private final EventManager eventManager;

    ///..
    private final MemorySimulationReportDto memorySimulationReportDto;

    ///
    public SystemMemory(final long accessTime, final EventManager eventManager) {

        this.accessTime = accessTime;
        this.eventManager = eventManager;

        memorySimulationReportDto = new MemorySimulationReportDto();
    }

    ///
    @Override
    public long advance(final long cycles) {

        return eventManager.advance(cycles);
    }

    ///..
    @Override
    public long read(final long address) {

        memorySimulationReportDto.setReadRequests(memorySimulationReportDto.getReadRequests() + 1);
        return eventManager.advance(accessTime);
    }

    ///..
    @Override
    public long write(final long address) {

        memorySimulationReportDto.setWriteRequests(memorySimulationReportDto.getWriteRequests() + 1);
        return eventManager.advance(accessTime);
    }

    ///
}
