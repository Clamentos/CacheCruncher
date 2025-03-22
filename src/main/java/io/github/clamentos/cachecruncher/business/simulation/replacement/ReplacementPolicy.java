package io.github.clamentos.cachecruncher.business.simulation.replacement;

///
public interface ReplacementPolicy {

    ///
    int getVictim(int index);
    void update(int index, int way);

    ///
}
