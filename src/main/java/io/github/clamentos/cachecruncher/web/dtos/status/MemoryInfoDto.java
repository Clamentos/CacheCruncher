package io.github.clamentos.cachecruncher.web.dtos.status;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class MemoryInfoDto {

    ///
    private final MemorySubInfoDto heapUsage;
    private final MemorySubInfoDto nonHeapUsage;

    ///
}
