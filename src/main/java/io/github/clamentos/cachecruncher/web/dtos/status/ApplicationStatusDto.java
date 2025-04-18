package io.github.clamentos.cachecruncher.web.dtos.status;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class ApplicationStatusDto {

    ///
    private final RuntimeInfo runtimeInfo;
    private final MemoryInfo memoryInfo;
    private final ThreadsInfo threadsInfo;
    private final ResponsesInfo responsesInfo;
    private final SimulationStatusInfo simulationInfo;

    ///
}
