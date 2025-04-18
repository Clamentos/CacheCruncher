package io.github.clamentos.cachecruncher.web.dtos.status;

///
import java.lang.management.ThreadInfo;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class ThreadsInfo {

    ///
    private final int platformThreadCount;
    private final int daemonThreadCount;
    private final int peakThreadCount;

    ///..
    private final ThreadInfo[] threadDump;

    ///
}
