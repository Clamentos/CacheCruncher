package io.github.clamentos.cachecruncher.web.dtos.simulation;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

///.
import java.util.Objects;

///.
import lombok.Getter;

///
@Getter
@JsonIgnoreProperties(ignoreUnknown = false)

///
public final class CacheConfigurationDto {

    ///
    private final String name;
    private final Integer accessTime;
    private final Integer numSetsExp;
    private final Integer lineSizeExp;
    private final Integer associativity;
    private final ReplacementPolicyType replacementPolicyType;

    ///..
    private final CacheConfigurationDto nextLevelConfiguration;

    ///..
    @JsonIgnore
    private final int hashCodeCache;

    ///
    @JsonCreator
    public CacheConfigurationDto(

        @JsonProperty("name") String name,
        @JsonProperty("accessTime") Integer accessTime,
        @JsonProperty("numSetsExp") Integer numSetsExp,
        @JsonProperty("lineSizeExp") Integer lineSizeExp,
        @JsonProperty("associativity") Integer associativity,
        @JsonProperty("replacementPolicyType") ReplacementPolicyType replacementPolicyType,
        @JsonProperty("nextLevelConfiguration") CacheConfigurationDto nextLevelConfiguration
    ) {

        this.name = name;
        this.accessTime = accessTime;
        this.numSetsExp = numSetsExp;
        this.lineSizeExp = lineSizeExp;
        this.associativity = associativity;
        this.replacementPolicyType = replacementPolicyType;
        this.nextLevelConfiguration = nextLevelConfiguration;

        hashCodeCache = Objects.hash(

            accessTime,
            numSetsExp,
            lineSizeExp,
            associativity,
            replacementPolicyType,
            nextLevelConfiguration
        );
    }

    ///
    @Override
    public boolean equals(Object obj) {

        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;

        boolean isEqual = true;
        CacheConfigurationDto other = (CacheConfigurationDto) obj;

        isEqual &= Objects.equals(accessTime, other.accessTime);
        isEqual &= Objects.equals(numSetsExp, other.numSetsExp);
        isEqual &= Objects.equals(lineSizeExp, other.lineSizeExp);
        isEqual &= Objects.equals(associativity, other.associativity);
        isEqual &= Objects.equals(replacementPolicyType, other.replacementPolicyType);
        isEqual &= Objects.equals(nextLevelConfiguration, other.nextLevelConfiguration);

        return isEqual;
    }

    ///..
    @Override
    public int hashCode() {

        return hashCodeCache;
    }

    ///
}
