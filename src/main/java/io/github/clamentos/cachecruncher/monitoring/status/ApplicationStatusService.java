package io.github.clamentos.cachecruncher.monitoring.status;

///
import io.github.clamentos.cachecruncher.business.services.CacheTraceService;

///..
import io.github.clamentos.cachecruncher.utility.Pair;

///..
import io.github.clamentos.cachecruncher.web.dtos.status.ApplicationStatusDto;
import io.github.clamentos.cachecruncher.web.dtos.status.LatencyDistribution;
import io.github.clamentos.cachecruncher.web.dtos.status.MemoryInfo;
import io.github.clamentos.cachecruncher.web.dtos.status.MemorySubInfo;
import io.github.clamentos.cachecruncher.web.dtos.status.ResponsesInfo;
import io.github.clamentos.cachecruncher.web.dtos.status.RuntimeInfo;
import io.github.clamentos.cachecruncher.web.dtos.status.SimulationStatusInfo;
import io.github.clamentos.cachecruncher.web.dtos.status.ThreadsInfo;

///.
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

///.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

///..
import java.util.concurrent.ConcurrentHashMap;

///..
import java.util.concurrent.atomic.AtomicLong;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.context.event.EventListener;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.core.task.TaskExecutor;

///..
import org.springframework.http.HttpStatus;

///..
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

///..
import org.springframework.stereotype.Service;

///..
import org.springframework.web.context.support.ServletRequestHandledEvent;

///
@Service
@Slf4j

///
public class ApplicationStatusService {

    ///
    private final ThreadPoolTaskExecutor simulationsExecutor;
    private final CacheTraceService cacheTraceService;

    ///..
    private final Map<String, Map<HttpStatus, AtomicLong>> responseStatusCounters;
    private final Map<String, TimeSamples> requestTimeSamples;

    ///..
    private final MemoryMXBean memoryBean;
    private final RuntimeMXBean runtimeBean;
    private final ThreadMXBean threadBean;

    ///..
    private final int numBuckets;
    private final int timeSampleSize;

    ///
    @Autowired
    public ApplicationStatusService(TaskExecutor simulationsExecutor, CacheTraceService cacheTraceService, Environment environment) {

        this.simulationsExecutor = (ThreadPoolTaskExecutor) simulationsExecutor;
        this.cacheTraceService = cacheTraceService;

        responseStatusCounters = new ConcurrentHashMap<>();
        requestTimeSamples = new ConcurrentHashMap<>();

        memoryBean = ManagementFactory.getMemoryMXBean();
        runtimeBean = ManagementFactory.getRuntimeMXBean();
        threadBean = ManagementFactory.getThreadMXBean();

        numBuckets = environment.getProperty("cache-cruncher.monitoring.status.numBuckets", Integer.class, 20);
        timeSampleSize = environment.getProperty("cache-cruncher.monitoring.status.timeSampleSize", Integer.class, 1024);
    }

    ///
    public ApplicationStatusDto getStatistics(

        Boolean includeRuntimeInfo,
        Boolean includeMemoryInfo,
        Boolean includeThreadsInfo,
        Boolean includeResponsesInfo,
        Boolean includeSimulationInfo
    ) {

        RuntimeInfo runtimeInfo = null;
        MemoryInfo memoryInfo = null;
        ThreadsInfo threadsInfo = null;
        ResponsesInfo responsesInfo = null;
        SimulationStatusInfo simulationInfo = null;

        if(includeRuntimeInfo != null && includeRuntimeInfo.booleanValue()) {

            runtimeInfo = new RuntimeInfo(

                runtimeBean.getStartTime(),
                runtimeBean.getUptime(),
                runtimeBean.getInputArguments(),
                runtimeBean.getSystemProperties()
            );
        }

        if(includeMemoryInfo != null && includeMemoryInfo.booleanValue()) {

            MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemoryUsage = memoryBean.getHeapMemoryUsage();

            memoryInfo = new MemoryInfo(

                new MemorySubInfo(

                    heapMemoryUsage.getInit(),
                    heapMemoryUsage.getUsed(),
                    heapMemoryUsage.getCommitted(),
                    heapMemoryUsage.getMax()
                ),

                new MemorySubInfo(

                    nonHeapMemoryUsage.getInit(),
                    nonHeapMemoryUsage.getUsed(),
                    nonHeapMemoryUsage.getCommitted(),
                    nonHeapMemoryUsage.getMax()
                )
            );
        }

        if(includeThreadsInfo != null && includeThreadsInfo.booleanValue()) {

            int threadCount = threadBean.getThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();

            threadsInfo = new ThreadsInfo(

                threadCount - daemonThreadCount,
                daemonThreadCount,
                threadBean.getPeakThreadCount(),
                threadBean.dumpAllThreads(false, false, 0)
            );
        }

        if(includeResponsesInfo != null && includeResponsesInfo.booleanValue()) {

            responsesInfo = new ResponsesInfo(

                this.extract(responseStatusCounters),
                this.calculateDistributions(requestTimeSamples)
            );
        }

        if(includeSimulationInfo != null && includeSimulationInfo.booleanValue()) {

            simulationInfo = new SimulationStatusInfo(

                simulationsExecutor.getQueueSize(),
                cacheTraceService.getCompletedSimulationCount(),
                cacheTraceService.getRejectedSimulationCount()
            );
        }

        return new ApplicationStatusDto(

            runtimeInfo,
            memoryInfo,
            threadsInfo,
            responsesInfo,
            simulationInfo
        );
    }

    ///..
    public void resetStatistics() {

        responseStatusCounters.clear();
        requestTimeSamples.clear();

        log.info("Statistics reset");
    }

    ///.
    @EventListener
    @SuppressWarnings(value = "unused")
    protected void handleRequestHandledEvent(ServletRequestHandledEvent event) {

        // FIXME: the uri returned includes actual path valiable values...
        String uri = event.getMethod() + event.getRequestUrl();
        HttpStatus status = HttpStatus.valueOf(event.getStatusCode());

        responseStatusCounters

            .computeIfAbsent(uri, key -> new ConcurrentHashMap<>())
            .computeIfAbsent(status, key -> new AtomicLong())
            .incrementAndGet()
        ;

        requestTimeSamples.computeIfAbsent(uri, key -> new TimeSamples(timeSampleSize)).put((int)event.getProcessingTimeMillis());
    }

    ///.
    private <T, B> Map<T, Map<B, Long>> extract(Map<T, Map<B, AtomicLong>> inputMap) throws NullPointerException {

        Map<T, Map<B, Long>> extractedMap = new HashMap<>();

        for(Map.Entry<T, Map<B, AtomicLong>> entry : inputMap.entrySet()) {

            Map<B, Long> innerMap = new HashMap<>();
            extractedMap.put(entry.getKey(), innerMap);

            for(Map.Entry<B, AtomicLong> innerEntry : entry.getValue().entrySet()) {

                innerMap.put(innerEntry.getKey(), innerEntry.getValue().get());
            }
        }

        return(extractedMap);
    }

    ///..
    private Map<String, List<LatencyDistribution>> calculateDistributions(Map<String, TimeSamples> requestTimeSamples) {

        Set<Map.Entry<String, TimeSamples>> entries = requestTimeSamples.entrySet();
        Map<String, List<LatencyDistribution>> distributions = HashMap.newHashMap(entries.size());

        for(Map.Entry<String, TimeSamples> entry : entries) {

            distributions.put(entry.getKey(), this.calculateDistribution(entry.getValue()));
        }

        return(distributions);
    }

    ///..
    private List<LatencyDistribution> calculateDistribution(TimeSamples requestTimeSamples) {

        int[] samples = requestTimeSamples.getAll();
        int max = 0;

        for(int i = 0; i < samples.length; i++) {

            max = Math.max(max, samples[i]);
        }

        // long[] always has 1 element, used as an "indirect" value.
        Map<Pair<Integer, Integer>, long[]> buckets = LinkedHashMap.newLinkedHashMap(numBuckets);
        int bucketSize = Math.ceilDiv(max, numBuckets);
        int start = 0;

        for(int i = 0; i < numBuckets; i++) {

            buckets.put(new Pair<>(start, (start + bucketSize)), new long[]{0});
            start += bucketSize;
        }

        for(int i = 0; i < samples.length; i++) {

            if(samples[i] > 0) {

                for(Map.Entry<Pair<Integer, Integer>, long[]> entry : buckets.entrySet()) {

                    if(this.contains(entry.getKey(), samples[i])) {
    
                        entry.getValue()[0]++;
                        break;
                    }
                }
            }
        }

        List<LatencyDistribution> distribution = new ArrayList<>(numBuckets);

        for(Map.Entry<Pair<Integer, Integer>, long[]> entry : buckets.entrySet()) {

            distribution.add(new LatencyDistribution(

                entry.getKey().getA(),
                entry.getKey().getB() - 1,
                entry.getValue()[0]
            ));
        }

        return(distribution);
    }

    ///..
    private boolean contains(Pair<Integer, Integer> pair, Integer target) {

        return pair.getA().compareTo(target) <= 0 && pair.getB().compareTo(target) > 0;
    }

    ///
}
