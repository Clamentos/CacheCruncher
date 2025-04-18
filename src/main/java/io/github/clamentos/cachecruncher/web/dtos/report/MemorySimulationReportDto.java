package io.github.clamentos.cachecruncher.web.dtos.report;

///
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public class MemorySimulationReportDto {

    ///
    private long readRequests;
    private long writeRequests;

    ///
}
