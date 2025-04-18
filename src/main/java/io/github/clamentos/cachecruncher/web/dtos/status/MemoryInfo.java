package io.github.clamentos.cachecruncher.web.dtos.status;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class MemoryInfo {

    ///
    private final MemorySubInfo heapUsage;
    private final MemorySubInfo nonHeapUsage;

    ///
}
