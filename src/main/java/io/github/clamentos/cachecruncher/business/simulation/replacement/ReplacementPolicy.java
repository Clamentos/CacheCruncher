package io.github.clamentos.cachecruncher.business.simulation.replacement;

///
public interface ReplacementPolicy {

    ///
    int getVictim(final int index);
    void update(final int index, final int way, final boolean onHit);

    ///
}
