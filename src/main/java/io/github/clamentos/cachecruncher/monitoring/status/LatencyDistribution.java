package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.utility.Pair;
import io.github.clamentos.cachecruncher.utility.Triple;

///.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

///..
import java.util.concurrent.atomic.AtomicInteger;

///
public final class LatencyDistribution {

    ///
    private final List<Triple<Integer, Integer, AtomicInteger>> buckets;

    ///..
    private final AtomicInteger outliersCounter;
    private final String outliersStartingBoundary;

    ///
    public LatencyDistribution(final List<Pair<Integer, Integer>> breakpoints, final int outliersStartingBoundary) {

        buckets = new ArrayList<>(breakpoints.size());

        for(final Pair<Integer, Integer> breakpoint : breakpoints) {

            buckets.add(new Triple<>(breakpoint.getA(), breakpoint.getB(), new AtomicInteger()));
        }

        outliersCounter = new AtomicInteger();
        this.outliersStartingBoundary = Integer.toString(outliersStartingBoundary) + "+";
    }

    ///
    public void update(final int latency) {

        for(final Triple<Integer, Integer, AtomicInteger> bucket : buckets) {

            if(bucket.getA().compareTo(latency) <= 0 && bucket.getB().compareTo(latency) >= 0) {

                bucket.getC().incrementAndGet();
                return;
            }
        }

        outliersCounter.incrementAndGet();
    }

    ///..
    public Map<String, Integer> getDistribution() {

        final Map<String, Integer> distribution = HashMap.newHashMap(buckets.size());

        for(final Triple<Integer, Integer, AtomicInteger> bucket : buckets) {

            distribution.put(

                bucket.getA().toString() + "-" + bucket.getB().toString(),
                bucket.getC().get()
            );
        }

        distribution.put(outliersStartingBoundary, outliersCounter.get());
        return distribution;
    }

    ///
}
