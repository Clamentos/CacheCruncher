package io.github.clamentos.cachecruncher.web.dtos.simulation;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

///.
import io.github.clamentos.cachecruncher.web.dtos.DepthLimitedDto;

///.
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@Getter
@EqualsAndHashCode(callSuper = false)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")

@JsonSubTypes({

    @JsonSubTypes.Type(value = MemoryConfigurationDto.class, name = "LEAF"),
    @JsonSubTypes.Type(value = CacheConfigurationDto.class, name = "NODE")
})

///
public sealed class MemoryConfigurationDto extends DepthLimitedDto permits CacheConfigurationDto {

    ///
    private final Long accessTime;

    ///
    @JsonCreator
    public MemoryConfigurationDto(@JsonProperty("accessTime") Long accessTime) {

        super(MemoryConfigurationDto.class, 8);
        this.accessTime = accessTime;
    }

    ///
}
