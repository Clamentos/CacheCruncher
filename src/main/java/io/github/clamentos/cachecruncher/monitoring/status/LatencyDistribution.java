package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.utility.Pair;
import io.github.clamentos.cachecruncher.utility.Triple;

///.
import java.util.ArrayList;
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
    public LatencyDistribution(List<Pair<Integer, Integer>> breakpoints, int outliersStartingBoundary) {

        buckets = new ArrayList<>(breakpoints.size());

        for(Pair<Integer, Integer> breakpoint : breakpoints) {

            buckets.add(new Triple<>(breakpoint.getA(), breakpoint.getB(), new AtomicInteger()));
        }

        outliersCounter = new AtomicInteger();
        this.outliersStartingBoundary = Integer.toString(outliersStartingBoundary);
    }

    ///
    public void update(int latency) {

        for(Triple<Integer, Integer, AtomicInteger> bucket : buckets) {

            if(bucket.getA().compareTo(latency) >= 0 && bucket.getB().compareTo(latency) <= 0) {

                bucket.getC().incrementAndGet();
                return;
            }
        }

        outliersCounter.incrementAndGet();
    }

    ///..
    public List<Map<String, Integer>> getDistribution() {

        List<Map<String, Integer>> distribution = new ArrayList<>(buckets.size() + 1);

        for(Triple<Integer, Integer, AtomicInteger> bucket : buckets) {

            distribution.add(Map.of(

                bucket.getA().toString() + "-" + bucket.getB().toString(),
                bucket.getC().get()
            ));
        }

        distribution.add(Map.of(outliersStartingBoundary, outliersCounter.get()));
        return distribution;
    }

    ///
}
