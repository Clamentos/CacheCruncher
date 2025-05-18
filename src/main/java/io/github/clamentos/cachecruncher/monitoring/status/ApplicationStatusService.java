package io.github.clamentos.cachecruncher.monitoring.status;

///
import com.fasterxml.jackson.core.type.TypeReference;

///.
import io.github.clamentos.cachecruncher.business.services.CacheTraceService;
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.monitoring.logging.LogLevel;

///..
import io.github.clamentos.cachecruncher.monitoring.status.validation.LogSearchFilterValidator;
import io.github.clamentos.cachecruncher.monitoring.status.validation.ResponseInfoSearchFilterValidator;

///..
import io.github.clamentos.cachecruncher.persistence.daos.LogDao;
import io.github.clamentos.cachecruncher.persistence.daos.MetricDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Log;
import io.github.clamentos.cachecruncher.persistence.entities.Metric;

///..
import io.github.clamentos.cachecruncher.utility.JsonMapper;

///..
import io.github.clamentos.cachecruncher.web.dtos.filters.LogSearchFilter;
import io.github.clamentos.cachecruncher.web.dtos.filters.ResponseInfoSearchFilter;

///..
import io.github.clamentos.cachecruncher.web.dtos.status.ApplicationStatusDto;
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
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

///.
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

///..
import java.util.concurrent.ConcurrentHashMap;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

///..
import org.springframework.core.task.TaskExecutor;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.http.HttpStatus;

///..
import org.springframework.scheduling.annotation.Scheduled;

///..
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

///..
import org.springframework.stereotype.Service;

///..
import org.springframework.transaction.annotation.Transactional;

///..
import org.springframework.web.bind.annotation.RequestMethod;

///..
import org.springframework.web.context.support.ServletRequestHandledEvent;

///..
import org.springframework.web.method.HandlerMethod;

///..
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

///..
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

///
@Service
@Slf4j

///
public class ApplicationStatusService {

    ///
    private final ResponseInfoSearchFilterValidator responseInfoSearchFilterValidator;
    private final LogSearchFilterValidator logSearchFilterValidator;

    ///..
    private final ThreadPoolTaskExecutor simulationsExecutor;
    private final CacheTraceService cacheTraceService;
    private final SessionService sessionService;

    ///..
    private final LogDao logDao;
    private final MetricDao metricDao;

    ///..
    private final JsonMapper jsonMapper;

    ///..
    private final RequestsMetrics requestsMetrics;
    private final Map<String, Integer> uriIdMap;

    ///..
    private final MemoryMXBean memoryBean;
    private final RuntimeMXBean runtimeBean;
    private final ThreadMXBean threadBean;
    private final OperatingSystemMXBean operatingSystemBean;

    ///..
    private final TypeReference<Map<String, Integer>> mapTypeRef;

    ///
    @Autowired
    public ApplicationStatusService(

        ResponseInfoSearchFilterValidator searchFilterValidator,
        LogSearchFilterValidator logSearchFilterValidator,
        TaskExecutor simulationsExecutor,
        CacheTraceService cacheTraceService,
        SessionService sessionService,
        LogDao logDao,
        MetricDao metricDao,
        JsonMapper jsonMapper,
        RequestsMetrics requestsMetrics
    ) {

        this.responseInfoSearchFilterValidator = searchFilterValidator;
        this.logSearchFilterValidator = logSearchFilterValidator;

        this.simulationsExecutor = (ThreadPoolTaskExecutor)simulationsExecutor;
        this.cacheTraceService = cacheTraceService;
        this.sessionService = sessionService;

        this.logDao = logDao;
        this.metricDao = metricDao;

        this.jsonMapper = jsonMapper;

        this.requestsMetrics = requestsMetrics;
        uriIdMap = new ConcurrentHashMap<>();

        memoryBean = ManagementFactory.getMemoryMXBean();
        runtimeBean = ManagementFactory.getRuntimeMXBean();
        threadBean = ManagementFactory.getThreadMXBean();
        operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();

        mapTypeRef = new TypeReference<>(){};
    }

    ///
    public ApplicationStatusDto getStatistics(

        boolean includeRuntimeInfo,
        boolean includeMemoryInfo,
        boolean includeThreadsInfo,
        boolean includeResponsesInfo,
        boolean includeSimulationInfo,
        boolean includeSessionsInfo
    ) {

        RuntimeInfo runtimeInfo = null;
        MemoryInfo memoryInfo = null;
        ThreadsInfo threadsInfo = null;
        ResponsesInfo responsesInfo = null;
        SimulationStatusInfo simulationInfo = null;

        if(includeRuntimeInfo) {

            runtimeInfo = new RuntimeInfo(

                runtimeBean.getStartTime(),
                runtimeBean.getUptime(),
                runtimeBean.getInputArguments(),
                runtimeBean.getSystemProperties()
            );
        }

        if(includeMemoryInfo) {

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

        if(includeThreadsInfo) {

            int threadCount = threadBean.getThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();

            threadsInfo = new ThreadsInfo(

                threadCount - daemonThreadCount,
                daemonThreadCount,
                threadBean.getPeakThreadCount(),
                operatingSystemBean.getSystemLoadAverage(),
                threadBean.dumpAllThreads(false, false, 0)
            );
        }

        if(includeResponsesInfo) {

            responsesInfo = new ResponsesInfo(

                uriIdMap,
                requestsMetrics.getMetrics(uriIdMap)
            );
        }

        if(includeSimulationInfo) {

            simulationInfo = new SimulationStatusInfo(

                simulationsExecutor.getQueueSize(),
                cacheTraceService.getCompletedSimulationCount(),
                cacheTraceService.getRejectedSimulationCount()
            );
        }

        Integer loggedUsersCount = includeSessionsInfo ? sessionService.getCurrentlyLoggedUsersCount() : null;

        return new ApplicationStatusDto(

            runtimeInfo,
            memoryInfo,
            threadsInfo,
            responsesInfo,
            simulationInfo,
            loggedUsersCount
        );
    }

    ///..
    public ResponsesInfo getResponsesInfoByFilter(ResponseInfoSearchFilter responseInfoSearchFilter)
    throws DataAccessException, IllegalArgumentException {

        Map<Integer, Map<Integer, Map<HttpStatus, List<Map<String, Integer>>>>> metrics = new HashMap<>();
        responseInfoSearchFilterValidator.validate(responseInfoSearchFilter);

        List<Metric> fetchedMetricEntities = metricDao.selectMetricsByFilter(

            responseInfoSearchFilter.getCreatedAtStart(),
            responseInfoSearchFilter.getCreatedAtEnd(),
            responseInfoSearchFilter.getLastTimestamp(),
            responseInfoSearchFilter.getCount()
        );

        for(Metric fetchedMetric : fetchedMetricEntities) {

            metrics

                .computeIfAbsent(fetchedMetric.getSecond(), _ -> new HashMap<>())
                .computeIfAbsent(uriIdMap.get(fetchedMetric.getEndpoint()), _ -> new EnumMap<>(HttpStatus.class))
                .computeIfAbsent(HttpStatus.valueOf(fetchedMetric.getStatus()), _ -> new ArrayList<>())
                .add(jsonMapper.deserialize(fetchedMetric.getData(), mapTypeRef))
            ;
        }

        return new ResponsesInfo(uriIdMap, metrics);
    }

    ///..
    public List<Log> getLogsByFilter(LogSearchFilter logSearchFilter) throws DataAccessException, IllegalArgumentException {

        logSearchFilterValidator.validate(logSearchFilter);

        return logDao.selectLogsByFilter(

            logSearchFilter.getCreatedAtStart(),
            logSearchFilter.getCreatedAtEnd(),
            logSearchFilter.getLevels(),
            logSearchFilter.getThreadLike() + "%",
            logSearchFilter.getLoggerLike() + "%",
            logSearchFilter.getMessageLike() + "%",
            logSearchFilter.getLastTimestamp(),
            logSearchFilter.getCount()
        );
    }

    ///..
    public Map<LogLevel, Long> getLogsCount() throws DataAccessException {

        return logDao.countByLevel();
    }

    ///..
    @Transactional
    public int deleteMetrics(long createdAtStart, long createdAtEnd) throws DataAccessException {

        return metricDao.delete(createdAtStart, createdAtEnd);
    }

    ///..
    @Transactional
    public int deleteLogs(long createdAtStart, long createdAtEnd) throws DataAccessException {

        return logDao.delete(createdAtStart, createdAtEnd);
    }

    ///.
    @EventListener
    protected void handleRequestHandledEvent(ServletRequestHandledEvent requestHandledEvent) {

        String url = requestHandledEvent.getMethod() + requestHandledEvent.getRequestUrl();
        int queryStart = url.indexOf("?");
        String path = queryStart > 0 ? url.substring(0, queryStart) : url;

        requestsMetrics.updateMetrics(

            path,
            HttpStatus.valueOf(requestHandledEvent.getStatusCode()),
            (int)requestHandledEvent.getProcessingTimeMillis()
        );
    }

    ///..
    @EventListener
    protected void handleContextRefresh(ContextRefreshedEvent contextRefreshedEvent) {

        Map<RequestMappingInfo, HandlerMethod> mappings = contextRefreshedEvent

            .getApplicationContext()
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class)
            .getHandlerMethods()
        ;

        int counter = 0;

        for(Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.entrySet()) {

            for(RequestMethod method : entry.getKey().getMethodsCondition().getMethods()) {

                for(String path : entry.getKey().getDirectPaths()) {

                    uriIdMap.put(method.toString() + path, counter++);
                }
            }
        }
    }

    ///..
    @Scheduled(fixedRate = 1_000, scheduler = "taskScheduler")
    protected void dump() {

        try {

            requestsMetrics.tryRollover(metrics -> {

                log.info("Starting metrics dumping task...");

                List<Metric> metricEntities = new ArrayList<>();
                long now = System.currentTimeMillis();

                for(Map.Entry<Integer, Map<String, Map<HttpStatus, LatencyDistribution>>> metricEntry : metrics.entrySet()) {

                    for(Map.Entry<String, Map<HttpStatus, LatencyDistribution>> pathEntry : metricEntry.getValue().entrySet()) {

                        for(Map.Entry<HttpStatus, LatencyDistribution> statusEntry : pathEntry.getValue().entrySet()) {

                            metricEntities.add(new Metric(

                                -1L,
                                now,
                                metricEntry.getKey(),
                                pathEntry.getKey(),
                                (short)statusEntry.getKey().value(),
                                jsonMapper.serialize(statusEntry.getValue().getDistribution())
                            ));
                        }
                    }
                }

                metricDao.insert(metricEntities);
                log.info("Metrics dumping task completed, {} metrics written", metricEntities.size());
            });
        }

        catch(DataAccessException | IllegalArgumentException exc) {

            log.error("Could not perform metrics rollover to database, this batch will be lost", exc);
        }
    }

    ///
}
