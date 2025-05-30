package io.github.clamentos.cachecruncher.business.simulation.replacement;

///
import java.util.Random;

///..
import java.util.random.RandomGenerator;

///
public final class RandomReplacementPolicy implements ReplacementPolicy {

    ///
    private final RandomGenerator rng;

    ///..
    private final int rngUpperBound;

    ///
    public RandomReplacementPolicy(final int associativity) {

        rng = new Random();
        this.rngUpperBound = associativity - 1;
    }

    ///
    @Override
    public int getVictim(final int index) {

        return rng.nextInt(0, rngUpperBound);
    }

    ///..
    @Override
    public void update(final int index, final int way, final boolean onHit) {

        // This does nothing since there is no need to track victims.
    }

    ///
}
