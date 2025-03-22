package io.github.clamentos.cachecruncher.business.simulation.replacement;

///
public final class LruReplacementPolicy implements ReplacementPolicy {

    ///
    private final int associativity;

    ///..
    private final int[][] lruCounters;

    ///
    public LruReplacementPolicy(int associativity, int numSets) {

        this.associativity = associativity;
        lruCounters = new int[2 << (numSets - 1)][associativity];

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
    public int getVictim(int index) {

        int[] lruCountersOfSet = lruCounters[index];
        int victimPosition = 0;

        for(int i = 0; i < lruCountersOfSet.length; i++) {

            if(lruCountersOfSet[i] == 0) {

                victimPosition = i;
                break;
            }
        }

        return victimPosition;
    }

    ///..
    @Override
    public void update(int index, int way) {

        int oldLru = lruCounters[index][way];

        for(int i = 0; i < lruCounters[index].length; i++) {

            if(i != way && lruCounters[index][i] > oldLru) {

                lruCounters[index][i]--;
            }
        }

        lruCounters[index][way] = associativity - 1;
    }

    ///
}
