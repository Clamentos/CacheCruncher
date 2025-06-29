package io.github.clamentos.cachecruncher.web.dtos.simulation;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

///..
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;

///.
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.EqualsAndHashCode.CacheStrategy;

///
@Getter
@EqualsAndHashCode(callSuper = true, cacheStrategy = CacheStrategy.LAZY)

///
public final class CacheConfigurationDto extends MemoryConfigurationDto {

    ///
    private final String name;
    private final Integer numSetsExp;
    private final Integer lineSizeExp;
    private final Integer associativity;
    private final ReplacementPolicyType replacementPolicyType;

    ///..
    private final MemoryConfigurationDto nextLevelConfiguration;

    ///
    @JsonCreator
    public CacheConfigurationDto(

        @JsonProperty("accessTime") final Long accessTime,

        @JsonProperty("name") final String name,
        @JsonProperty("numSetsExp") final Integer numSetsExp,
        @JsonProperty("lineSizeExp") final Integer lineSizeExp,
        @JsonProperty("associativity") final Integer associativity,
        @JsonProperty("replacementPolicyType") final ReplacementPolicyType replacementPolicyType,
        @JsonProperty("nextLevelConfiguration") final MemoryConfigurationDto nextLevelConfiguration

    ) throws DeserializationException {

        super(accessTime);

        this.name = name;
        this.numSetsExp = numSetsExp;
        this.lineSizeExp = lineSizeExp;
        this.associativity = associativity;
        this.replacementPolicyType = replacementPolicyType;
        this.nextLevelConfiguration = nextLevelConfiguration;

        super.clear(MemoryConfigurationDto.class);
    }

    ///
}
