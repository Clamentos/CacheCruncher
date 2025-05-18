package io.github.clamentos.cachecruncher.web.dtos.report;

///
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public sealed class MemorySimulationReportDto permits CacheSimulationReportDto {

    ///
    private long readRequests;
    private long writeRequests;

    ///
}
