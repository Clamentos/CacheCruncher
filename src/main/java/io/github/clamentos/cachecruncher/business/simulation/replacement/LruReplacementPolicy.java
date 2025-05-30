package io.github.clamentos.cachecruncher.business.simulation.replacement;

///
public final class LruReplacementPolicy implements ReplacementPolicy {

    ///
    private final int associativity;
    private final int[][] lruCounters;

    ///
    public LruReplacementPolicy(final int associativity, final int numSetsPow) {

        this.associativity = associativity;
        lruCounters = new int[numSetsPow][associativity];

        for(int i = 0; i < lruCounters.length; i++) {

            int value = associativity - 1;

            for(int j = 0; j < associativity; j++) {

                lruCounters[i][j] = value;
                value--;
            }
        }
    }

    ///
    @Override
    public int getVictim(final int index) {

        final int[] lruCountersOfSet = lruCounters[index];

        for(int i = 0; i < lruCountersOfSet.length; i++) {

            if(lruCountersOfSet[i] == 0) return i;
        }

        return 0;
    }

    ///..
    @Override
    public void update(final int index, final int way, final boolean onHit) {

        final int oldLru = lruCounters[index][way];

        for(int i = 0; i < lruCounters[index].length; i++) {

            if(i != way && lruCounters[index][i] > oldLru) lruCounters[index][i]--;
        }

        lruCounters[index][way] = associativity - 1;
    }

    ///
}
