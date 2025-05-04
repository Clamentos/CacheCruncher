package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.utility.Pair;

///.
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

///..
import java.util.concurrent.ConcurrentHashMap;

///..
import java.util.concurrent.atomic.AtomicInteger;

///
public final class LatencyDistribution {

    ///
    private final Map<Pair<Integer, Integer>, AtomicInteger> buckets;
    private final AtomicInteger outliersCounter;
    private final String outliersStartingBoundary;

    ///
    public LatencyDistribution(List<Pair<Integer, Integer>> breakpoints, int outliersStartingBoundary) {

        buckets = new ConcurrentHashMap<>();
        outliersCounter = new AtomicInteger();
        this.outliersStartingBoundary = Integer.toString(outliersStartingBoundary);

        for(Pair<Integer, Integer> breakpoint : breakpoints) {

            buckets.put(breakpoint, new AtomicInteger());
        }
    }

    ///
    public void update(int latency) {

        for(Map.Entry<Pair<Integer, Integer>, AtomicInteger> bucket : buckets.entrySet()) {

            Pair<Integer, Integer> range = bucket.getKey();

            if(range.getA().compareTo(latency) >= 0 && range.getB().compareTo(latency) <= 0) {

                bucket.getValue().incrementAndGet();
                return;
            }
        }

        outliersCounter.incrementAndGet();
    }

    ///..
    public List<Map<String, Integer>> getDistribution() {

        List<Map<String, Integer>> distribution = new ArrayList<>();

        for(Map.Entry<Pair<Integer, Integer>, AtomicInteger> bucket : buckets.entrySet()) {

            distribution.add(Map.of(

                bucket.getKey().getA().toString() + "-" + bucket.getKey().getB().toString(),
                bucket.getValue().get()
            ));
        }

        distribution.add(Map.of(outliersStartingBoundary, outliersCounter.get()));
        return distribution;
    }

    ///
}
