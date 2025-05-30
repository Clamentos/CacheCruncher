package io.github.clamentos.cachecruncher.business.simulation.replacement;

///
public class FifoReplacementPolicy implements ReplacementPolicy {

    ///
    private final int[][] fifoCounters;
    private final int[] queueSize;

    ///..
    private final int associativity;

    ///
    public FifoReplacementPolicy(final int associativity, final int numSetsPow) {

        fifoCounters = new int[numSetsPow][associativity];
        queueSize = new int[numSetsPow];

        this.associativity = associativity;
    }

    ///
    @Override
    public int getVictim(final int index) {

        final int[] fifoCountersOfSet = fifoCounters[index];

        for(int i = 0; i < fifoCountersOfSet.length; i++) {

            if(fifoCountersOfSet[i] == 1) return i;
        }

        return 0;
    }

    ///..
    @Override
    public void update(final int index, final int way, final boolean onHit) {
        
        if(!onHit) {

            final int[] fifoCountersOfSet = fifoCounters[index];

            if(queueSize[index] < associativity) {

                // Queue not yet full.

                fifoCountersOfSet[way] = queueSize[index] + 1;
                queueSize[index]++;
            }

            else {

                // Queue full.
                // From now on, 1 victim will exit the queue and free the next slot.
                // This creates a circular queue.

                for(int i = 0; i < associativity; i++) {

                    if(i == way) fifoCountersOfSet[i] = way;
                    else fifoCountersOfSet[i]--;
                }
            }
        }
    }

    ///
}
