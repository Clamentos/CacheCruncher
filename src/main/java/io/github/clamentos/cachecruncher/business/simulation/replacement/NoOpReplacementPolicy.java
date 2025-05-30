package io.github.clamentos.cachecruncher.business.simulation.replacement;

///
import lombok.NoArgsConstructor;

///
@NoArgsConstructor

///
public final class NoOpReplacementPolicy implements ReplacementPolicy {

    ///
    @Override
    public int getVictim(final int index) {

        return 0;
    }

    ///..
    @Override
    public void update(final int index, final int way, final boolean onHit) {

        // This does nothing since there is no need to track victims.
    }

    ///
}
