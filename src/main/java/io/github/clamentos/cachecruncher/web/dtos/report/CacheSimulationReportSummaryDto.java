package io.github.clamentos.cachecruncher.web.dtos.report;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class CacheSimulationReportSummaryDto {

    ///
    private final long beginTimestamp;
    private final long endTimestamp;

    ///..
    private final double averageMemoryAccessTime;
    private final CacheSimulationReportDto report;

    ///
}
