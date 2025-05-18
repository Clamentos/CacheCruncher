package io.github.clamentos.cachecruncher.web.dtos.simulation;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

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

        @JsonProperty("accessTime") Long accessTime,
        @JsonProperty("name") String name,
        @JsonProperty("numSetsExp") Integer numSetsExp,
        @JsonProperty("lineSizeExp") Integer lineSizeExp,
        @JsonProperty("associativity") Integer associativity,
        @JsonProperty("replacementPolicyType") ReplacementPolicyType replacementPolicyType,
        @JsonProperty("nextLevelConfiguration") MemoryConfigurationDto nextLevelConfiguration
    ) {

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
