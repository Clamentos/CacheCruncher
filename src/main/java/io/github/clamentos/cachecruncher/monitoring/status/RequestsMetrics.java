package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.utility.Pair;

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
import java.util.concurrent.atomic.AtomicInteger;

///..
import java.util.function.Consumer;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.http.HttpStatus;

///..
import org.springframework.stereotype.Component;

///
@Component

///
public final class RequestsMetrics {

    ///
    private final Map<Integer, Map<String, Map<HttpStatus, LatencyDistribution>>> latencyTracker;
    private final Map<Integer, Map<String, Map<HttpStatus, LatencyDistribution>>> latencyTrackerShadow;

    ///..
    private final AtomicBoolean direction;
    private final AtomicInteger currentSecond;

    ///..
    private final List<Pair<Integer, Integer>> breakpoints;
    private final int outliersStartingBoundary;
    private final int rolloverTime;

    ///
    @Autowired
    public RequestsMetrics(Environment environment) throws IllegalArgumentException {

        latencyTracker = new ConcurrentHashMap<>();
        latencyTrackerShadow = new ConcurrentHashMap<>();

        direction = new AtomicBoolean();
        currentSecond = new AtomicInteger();

        String breakpointsProp = environment.getProperty(

            "cache-cruncher.monitoring.status.breakpoints",
            String.class,
            "0-10,11-20,21-50-51-100,101-200,201-500"
        );

        String[] breakpointsSplits = breakpointsProp.split(",");
        breakpoints = new ArrayList<>(breakpointsSplits.length);

        if(breakpointsSplits.length == 0) {

            throw this.fail("Property \"cache-cruncher.monitoring.status.breakpoints\" must have at least 1 element");
        }

        for(String breakpoint : breakpointsSplits) {

            String[] boundaries = breakpoint.split("-");

            if(boundaries.length != 2) {

                throw this.fail("Breakpoints must be formatted: \"X-Y\"");
            }

            int boundaryStart = Integer.parseInt(boundaries[0]);
            int boundaryEnd = Integer.parseInt(boundaries[1]);

            if(boundaryStart >= boundaryEnd) {

                throw this.fail("Breakpoints must respect: Y > X");
            }

            breakpoints.add(new Pair<>(boundaryStart, boundaryEnd));
        }

        outliersStartingBoundary = environment.getProperty(

            "cache-cruncher.monitoring.status.outliersStartingBoundary",
            Integer.class,
            501
        );

        rolloverTime = environment.getProperty("cache-cruncher.monitoring.status.rolloverTime", Integer.class, 300);
    }

    ///
    public void updateMetrics(String path, HttpStatus status, int elapsed) {

        while(currentSecond.get() == rolloverTime) {

            // busy wait.
            // should stay here for a very small amount of time, that is,
            // the time it takes for the scheduled task to set the direction.
        }

        int slotValue = currentSecond.get();
        var currentTracker = direction.get() ? latencyTrackerShadow : latencyTracker;

        currentTracker

            .computeIfAbsent(slotValue, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(path, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(status, _ -> new LatencyDistribution(breakpoints, outliersStartingBoundary))
            .update(elapsed)
        ;
    }

    ///..
    public void tryRollover(Consumer<Map<Integer, Map<String, Map<HttpStatus, LatencyDistribution>>>> action) {

        if(currentSecond.get() == rolloverTime) {

            boolean oldDirection = direction.get();
            var currentTracker = oldDirection ? latencyTrackerShadow : latencyTracker;
            direction.set(!oldDirection);
            currentSecond.set(0);

            action.accept(currentTracker);
            currentTracker.clear();
        }

        else {

            currentSecond.incrementAndGet();
        }
    }

    ///..
    public Map<Integer, Map<Integer, Map<HttpStatus, List<Map<String, Integer>>>>> getMetrics(Map<String, Integer> uriIdMap) {

        Map<Integer, Map<Integer, Map<HttpStatus, List<Map<String, Integer>>>>> metrics = new HashMap<>();
        var currentTracker = direction.get() ? latencyTrackerShadow : latencyTracker;

        for(Map.Entry<Integer, Map<String, Map<HttpStatus, LatencyDistribution>>> trackerEntry : currentTracker.entrySet()) {

            Map<Integer, Map<HttpStatus, List<Map<String, Integer>>>> pathMetrics = new HashMap<>();
            metrics.put(trackerEntry.getKey(), pathMetrics);

            for(Map.Entry<String, Map<HttpStatus, LatencyDistribution>> pathEntry : trackerEntry.getValue().entrySet()) {

                Map<HttpStatus, List<Map<String, Integer>>> statusMetrics = new EnumMap<>(HttpStatus.class);
                pathMetrics.put(uriIdMap.get(pathEntry.getKey()), statusMetrics);

                for(Map.Entry<HttpStatus, LatencyDistribution> statusEntry : pathEntry.getValue().entrySet()) {

                    statusMetrics.put(statusEntry.getKey(), statusEntry.getValue().getDistribution());
                }
            }
        }

        return metrics;
    }

    ///.
    private IllegalArgumentException fail(String message) {

        return new IllegalArgumentException(new ErrorDetails(ErrorCode.GENERIC, message));
    }

    ///
}
