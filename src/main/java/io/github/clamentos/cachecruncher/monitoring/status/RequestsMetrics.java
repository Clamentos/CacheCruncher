package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.utility.Pair;
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

///..
import java.util.concurrent.ConcurrentHashMap;

///..
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

///..
import java.util.function.Consumer;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.http.HttpStatus;

///..
import org.springframework.stereotype.Component;

///
@Component

///
public final class RequestsMetrics {

    ///
    private final Map<Long, Map<String, Map<HttpStatus, LatencyDistribution>>> latencyTracker;
    private final Map<Long, Map<String, Map<HttpStatus, LatencyDistribution>>> latencyTrackerShadow;

    ///..
    private final AtomicBoolean direction;
    private final AtomicLong currentTimeSlot;
    private final AtomicLong rolloverTimeSlot;

    ///..
    private final List<Pair<Short, Short>> breakpoints;

    ///..
    private final long rolloverTime;

    ///
    @Autowired
    public RequestsMetrics(final PropertyProvider propertyProvider) throws BeanCreationException {

        latencyTracker = new ConcurrentHashMap<>();
        latencyTrackerShadow = new ConcurrentHashMap<>();

        final long now = System.currentTimeMillis();

        direction = new AtomicBoolean();
        currentTimeSlot = new AtomicLong(now);

        final String breakpointsProp = propertyProvider.getString(

            "cache-cruncher.monitoring.status.breakpoints",
            "0-10,11-20,21-50,51-100,101-200,201-500"
        );

        final String[] breakpointsSplits = breakpointsProp.split(",");
        breakpoints = new ArrayList<>(breakpointsSplits.length);

        if(breakpointsSplits.length == 0) {

            throw new BeanCreationException("Property \"cache-cruncher.monitoring.status.breakpoints\" must have at least 1 element");
        }

        for(final String breakpoint : breakpointsSplits) {

            final String[] boundaries = breakpoint.split("-");
            if(boundaries.length != 2) throw new BeanCreationException("Breakpoints must be formatted: \"X-Y\"");

            final short boundaryStart = Short.parseShort(boundaries[0]);
            final short boundaryEnd = Short.parseShort(boundaries[1]);

            if(boundaryStart >= boundaryEnd) throw new BeanCreationException("Breakpoints must respect: Y > X");
            breakpoints.add(new Pair<>(boundaryStart, boundaryEnd));
        }

        rolloverTime = propertyProvider.getLong("cache-cruncher.monitoring.status.rolloverTime", 300_000L, 1, Long.MAX_VALUE);
        rolloverTimeSlot = new AtomicLong(now + rolloverTime);
    }

    ///
    public void updateMetrics(final String path, final HttpStatus status, final int elapsed) {

        while(currentTimeSlot.get() >= rolloverTimeSlot.get()) {

            // busy wait (only when is rolling over).
            // should stay here for a very small amount of time, that is,
            // the time it takes for the scheduled task to set the direction.
        }

        final long slotValue = currentTimeSlot.get();
        final var currentTracker = direction.get() ? latencyTrackerShadow : latencyTracker;

        currentTracker

            .computeIfAbsent(slotValue, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(path, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(status, _ -> new LatencyDistribution(breakpoints))
            .update(elapsed)
        ;
    }

    ///..
    public void tryRollover(final boolean force, final Consumer<Map<Long, Map<String, Map<HttpStatus, LatencyDistribution>>>> action) {

        if(currentTimeSlot.get() >= rolloverTimeSlot.get() || force) {

            final long now = System.currentTimeMillis();
            final boolean oldDirection = direction.get();
            final var currentTracker = oldDirection ? latencyTrackerShadow : latencyTracker;

            direction.set(!oldDirection);
            currentTimeSlot.set(now);
            rolloverTimeSlot.set(now + rolloverTime);
            action.accept(currentTracker);
            currentTracker.clear();
        }

        else {

            currentTimeSlot.set(System.currentTimeMillis());
        }
    }

    ///..
    public Map<Long, Map<Integer, Map<HttpStatus, Map<String, Integer>>>> getMetrics(final Map<String, Integer> uriIdMap) {

        final Map<Long, Map<Integer, Map<HttpStatus, Map<String, Integer>>>> metrics = new HashMap<>();
        final var currentTracker = direction.get() ? latencyTrackerShadow : latencyTracker;

        for(final Map.Entry<Long, Map<String, Map<HttpStatus, LatencyDistribution>>> trackerEntry : currentTracker.entrySet()) {

            final Map<Integer, Map<HttpStatus, Map<String, Integer>>> pathMetrics = new HashMap<>();
            metrics.put(trackerEntry.getKey(), pathMetrics);

            for(final Map.Entry<String, Map<HttpStatus, LatencyDistribution>> pathEntry : trackerEntry.getValue().entrySet()) {

                final Map<HttpStatus, Map<String, Integer>> statusMetrics = new EnumMap<>(HttpStatus.class);
                pathMetrics.put(uriIdMap.get(pathEntry.getKey()), statusMetrics);

                for(final Map.Entry<HttpStatus, LatencyDistribution> statusEntry : pathEntry.getValue().entrySet()) {

                    statusMetrics.put(statusEntry.getKey(), statusEntry.getValue().getDistribution());
                }
            }
        }

        return metrics;
    }

    ///
}
