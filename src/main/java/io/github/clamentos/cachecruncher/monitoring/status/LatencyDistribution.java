package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.utility.Pair;

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
    private final List<LatencyDistributionEntry> buckets;

    ///..
    private final AtomicInteger outliersCounter;
    private final String outliersStartingBoundary;

    ///
    public LatencyDistribution(final List<Pair<Short, Short>> breakpoints, final int outliersStartingBoundary) {

        buckets = new ArrayList<>(breakpoints.size());

        for(final Pair<Short, Short> breakpoint : breakpoints) {

            buckets.add(new LatencyDistributionEntry(breakpoint.getA(), breakpoint.getB(), new AtomicInteger()));
        }

        outliersCounter = new AtomicInteger();
        this.outliersStartingBoundary = Integer.toString(outliersStartingBoundary) + "+";
    }

    ///
    public void update(final int latency) {

        for(final LatencyDistributionEntry bucket : buckets) {

            if(latency >= bucket.getLowerBound() && latency <= bucket.getUpperBound()) {

                bucket.getCount().incrementAndGet();
                return;
            }
        }

        outliersCounter.incrementAndGet();
    }

    ///..
    public Map<String, Integer> getDistribution() {

        final Map<String, Integer> distribution = HashMap.newHashMap(buckets.size());

        for(final LatencyDistributionEntry bucket : buckets) {

            distribution.put(bucket.getLowerBound() + "-" + bucket.getUpperBound(), bucket.getCount().get());
        }

        distribution.put(outliersStartingBoundary, outliersCounter.get());
        return distribution;
    }

    ///
}
