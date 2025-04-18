package io.github.clamentos.cachecruncher.web.dtos.status;

///
import java.util.List;
import java.util.Map;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class RuntimeInfo {

    ///
    private final long startTime;
    private final long uptime;

    ///..
    private final List<String> jvmArguments;
    private final Map<String, String> properties;

    ///
}
