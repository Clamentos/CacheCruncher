package io.github.clamentos.cachecruncher.web.dtos;

///
import io.github.clamentos.cachecruncher.business.simulation.replacement.ReplacementPolicyType;

///.
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class CacheConfigurationDto {

    ///
    private String name;
    private int lineSize;
    private int numSets;
    private int associativity;
    private ReplacementPolicyType replacementPolicyType;

    ///..
    private CacheConfigurationDto nextLevelConfiguration;

    ///
}
