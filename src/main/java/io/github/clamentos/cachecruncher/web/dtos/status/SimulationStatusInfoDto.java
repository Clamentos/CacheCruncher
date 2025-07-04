package io.github.clamentos.cachecruncher.web.dtos.status;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SimulationStatusInfoDto {

    ///
    private final int queueSize;
    private final long completedCount;
    private final long rejectedCount;

    ///
}
