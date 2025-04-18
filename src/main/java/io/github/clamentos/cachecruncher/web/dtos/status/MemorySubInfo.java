package io.github.clamentos.cachecruncher.web.dtos.status;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class MemorySubInfo {

    ///
    private final long initial;
    private final long inUse;
    private final long allocated;
    private final long maximum;

    ///
}
