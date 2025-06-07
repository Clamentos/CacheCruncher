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
    private final RuntimeInfoDto runtimeInfo;
    private final MemoryInfoDto memoryInfo;
    private final ThreadsInfoDto threadsInfo;
    private final ResponsesInfoDto responsesInfo;
    private final SimulationStatusInfoDto simulationStatusInfo;
    private final Integer sessionCount;
    private final Integer loggedInUserCount;

    ///
}
