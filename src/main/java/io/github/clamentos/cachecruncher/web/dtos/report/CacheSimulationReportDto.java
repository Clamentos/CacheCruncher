package io.github.clamentos.cachecruncher.web.dtos.report;

///
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class CacheSimulationReportDto extends MemorySimulationReportDto {

    ///
    private long readMisses;
    private long writeMisses;

    ///..
    private MemorySimulationReportDto nextLevelReport;

    ///
}
