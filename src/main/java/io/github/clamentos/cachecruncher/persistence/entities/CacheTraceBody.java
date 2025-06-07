package io.github.clamentos.cachecruncher.persistence.entities;

///
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

///.
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;

///..
import io.github.clamentos.cachecruncher.utility.MultiValueMap;

///..
import io.github.clamentos.cachecruncher.web.databind.CacheTraceBodyDeserializer;
import io.github.clamentos.cachecruncher.web.databind.CacheTraceBodySerializer;

///.
import java.util.List;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///..
@JsonSerialize(using = CacheTraceBodySerializer.class)
@JsonDeserialize(using = CacheTraceBodyDeserializer.class)

///
public final class CacheTraceBody {

    ///
    private final MultiValueMap<String, CacheCommand> sections;
    private final List<CacheCommand> body;

    ///
}
