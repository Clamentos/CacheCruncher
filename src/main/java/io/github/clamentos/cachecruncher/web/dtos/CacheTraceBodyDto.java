package io.github.clamentos.cachecruncher.web.dtos;

///
import java.util.List;
import java.util.Map;

///.
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class CacheTraceBodyDto {

    ///
    private Map<String, List<String>> sections;
    private List<String> trace;

    ///
}
