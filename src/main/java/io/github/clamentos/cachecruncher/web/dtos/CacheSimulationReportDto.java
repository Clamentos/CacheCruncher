package io.github.clamentos.cachecruncher.web.dtos;

///
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class CacheSimulationReportDto extends SimulationReportDto {

    ///
    private long readMisses;
    private long writeMisses;

    ///..
    private SimulationReportDto nextLevelReport;

    ///
}
