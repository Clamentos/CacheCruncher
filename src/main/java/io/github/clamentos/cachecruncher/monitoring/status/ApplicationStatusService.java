package io.github.clamentos.cachecruncher.monitoring.status;

///
import com.fasterxml.jackson.core.type.TypeReference;

///.
import io.github.clamentos.cachecruncher.business.services.CacheTraceService;
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.configuration.StartupActions;

///..
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;
import io.github.clamentos.cachecruncher.error.exceptions.SerializationException;
import io.github.clamentos.cachecruncher.error.exceptions.ValidationException;

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
import io.github.clamentos.cachecruncher.web.dtos.filters.LogSearchFilterDto;
import io.github.clamentos.cachecruncher.web.dtos.filters.RangeSearchFilterDto;

///..
import io.github.clamentos.cachecruncher.web.dtos.status.ApplicationStatusDto;
import io.github.clamentos.cachecruncher.web.dtos.status.MemoryInfoDto;
import io.github.clamentos.cachecruncher.web.dtos.status.MemorySubInfoDto;
import io.github.clamentos.cachecruncher.web.dtos.status.ResponsesInfoDto;
import io.github.clamentos.cachecruncher.web.dtos.status.RuntimeInfoDto;
import io.github.clamentos.cachecruncher.web.dtos.status.SimulationStatusInfoDto;
import io.github.clamentos.cachecruncher.web.dtos.status.ThreadsInfoDto;

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

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.context.event.ContextClosedEvent;

///..
import org.springframework.context.event.EventListener;

///..
import org.springframework.core.Ordered;

///..
import org.springframework.core.annotation.Order;

///..
import org.springframework.core.task.TaskExecutor;

///..
import org.springframework.dao.DataAccessException;
import org.springframework.dao.NonTransientDataAccessResourceException;

///..
import org.springframework.http.HttpStatus;

///..
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

///..
import org.springframework.stereotype.Service;

///..
import org.springframework.transaction.annotation.Transactional;

///..
import org.springframework.web.context.support.ServletRequestHandledEvent;

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
    private final StartupActions startupActions;

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

        final ResponseInfoSearchFilterValidator searchFilterValidator,
        final LogSearchFilterValidator logSearchFilterValidator,
        final TaskExecutor simulationsExecutor,
        final CacheTraceService cacheTraceService,
        final SessionService sessionService,
        final LogDao logDao,
        final MetricDao metricDao,
        final JsonMapper jsonMapper,
        final RequestsMetrics requestsMetrics,
        final StartupActions startupActions
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
        this.startupActions = startupActions;

        memoryBean = ManagementFactory.getMemoryMXBean();
        runtimeBean = ManagementFactory.getRuntimeMXBean();
        threadBean = ManagementFactory.getThreadMXBean();
        operatingSystemBean = ManagementFactory.getOperatingSystemMXBean();

        mapTypeRef = new TypeReference<>(){};
    }

    ///
    public ApplicationStatusDto getStatistics(

        final boolean includeRuntimeInfo,
        final boolean includeMemoryInfo,
        final boolean includeThreadsInfo,
        final boolean includeResponsesInfo,
        final boolean includeSimulationInfo,
        final boolean includeSessionsInfo
    ) {

        RuntimeInfoDto runtimeInfo = null;
        MemoryInfoDto memoryInfo = null;
        ThreadsInfoDto threadsInfo = null;
        ResponsesInfoDto responsesInfo = null;
        SimulationStatusInfoDto simulationInfo = null;

        if(includeRuntimeInfo) {

            runtimeInfo = new RuntimeInfoDto(

                runtimeBean.getStartTime(),
                runtimeBean.getUptime(),
                runtimeBean.getInputArguments(),
                runtimeBean.getSystemProperties()
            );
        }

        if(includeMemoryInfo) {

            final MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
            final MemoryUsage nonHeapMemoryUsage = memoryBean.getHeapMemoryUsage();

            memoryInfo = new MemoryInfoDto(

                new MemorySubInfoDto(

                    heapMemoryUsage.getInit(),
                    heapMemoryUsage.getUsed(),
                    heapMemoryUsage.getCommitted(),
                    heapMemoryUsage.getMax()
                ),

                new MemorySubInfoDto(

                    nonHeapMemoryUsage.getInit(),
                    nonHeapMemoryUsage.getUsed(),
                    nonHeapMemoryUsage.getCommitted(),
                    nonHeapMemoryUsage.getMax()
                )
            );
        }

        if(includeThreadsInfo) {

            final int threadCount = threadBean.getThreadCount();
            final int daemonThreadCount = threadBean.getDaemonThreadCount();

            threadsInfo = new ThreadsInfoDto(

                threadCount - daemonThreadCount,
                daemonThreadCount,
                threadBean.getPeakThreadCount(),
                operatingSystemBean.getSystemLoadAverage(),
                threadBean.dumpAllThreads(false, false, 0)
            );
        }

        if(includeResponsesInfo) {

            final Map<String, Integer> uriIdMap = startupActions.getUriIdMap();

            responsesInfo = new ResponsesInfoDto(

                uriIdMap,
                requestsMetrics.getMetrics(uriIdMap)
            );
        }

        if(includeSimulationInfo) {

            simulationInfo = new SimulationStatusInfoDto(

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
            simulationInfo,
            includeSessionsInfo ? sessionService.getSessionsCount() : null,
            includeSessionsInfo ? sessionService.getLoggedUsersCount() : null
        );
    }

    ///..
    public ResponsesInfoDto getResponsesInfoByFilter(final RangeSearchFilterDto responseInfoSearchFilter)
    throws DatabaseException, DeserializationException, ValidationException {

        responseInfoSearchFilterValidator.validate(responseInfoSearchFilter);

        final List<Metric> fetchedMetricEntities = metricDao.selectMetricsByFilter(

            responseInfoSearchFilter.getLastTimestamp(),
            responseInfoSearchFilter.getCount(),
            responseInfoSearchFilter.getCreatedAtStart(),
            responseInfoSearchFilter.getCreatedAtEnd()
        );

        final Map<Long, Map<Integer, Map<HttpStatus, Map<String, Integer>>>> metrics = new HashMap<>();
        final Map<String, Integer> uriIdMap = startupActions.getUriIdMap();

        for(final Metric fetchedMetric : fetchedMetricEntities) {

            metrics

                .computeIfAbsent(fetchedMetric.getTimestamp(), _ -> new HashMap<>())
                .computeIfAbsent(uriIdMap.get(fetchedMetric.getEndpoint()), _ -> new EnumMap<>(HttpStatus.class))
                .computeIfAbsent(HttpStatus.valueOf(fetchedMetric.getStatus()), _ -> new HashMap<>())
                .putAll(jsonMapper.deserialize(fetchedMetric.getData(), mapTypeRef))
            ;
        }

        return new ResponsesInfoDto(uriIdMap, metrics);
    }

    ///..
    public List<Log> getLogsByFilter(final LogSearchFilterDto logSearchFilter) throws DatabaseException, ValidationException {

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
    public Map<LogLevel, Long> getLogsCount() throws DatabaseException {

        return logDao.countByLevel();
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public int deleteMetrics(final long createdAtStart, final long createdAtEnd) throws DatabaseException {

        return metricDao.delete(createdAtStart, createdAtEnd);
    }

    ///..
    @Transactional(rollbackFor = DatabaseException.class)
    public int deleteLogs(final long createdAtStart, final long createdAtEnd) throws DatabaseException {

        return logDao.delete(createdAtStart, createdAtEnd);
    }

    ///..
    public void rolloverTask() {

        this.dumpMetrics(false);
    }

    ///.
    @EventListener
    protected void handleRequestHandledEvent(final ServletRequestHandledEvent requestHandledEvent) {

        final String url = requestHandledEvent.getMethod() + requestHandledEvent.getRequestUrl();
        final int queryStart = url.indexOf("?");
        final String path = queryStart > 0 ? url.substring(0, queryStart) : url;

        requestsMetrics.updateMetrics(

            path,
            HttpStatus.valueOf(requestHandledEvent.getStatusCode()),
            (int)requestHandledEvent.getProcessingTimeMillis()
        );
    }

    ///..
    @EventListener
    @Order(value = Ordered.HIGHEST_PRECEDENCE)
    protected void handleContextClosedEvent(final ContextClosedEvent contextClosedEvent) {

        this.dumpMetrics(true);
    }

    ///.
    private void dumpMetrics(final boolean force) {

        try {

            requestsMetrics.tryRollover(force, metrics -> {

                log.info("Starting metrics dumping task...");

                final List<Metric> metricEntities = new ArrayList<>();
                final long now = System.currentTimeMillis();

                for(final Map.Entry<Long, Map<String, Map<HttpStatus, LatencyDistribution>>> metricEntry : metrics.entrySet()) {

                    for(final Map.Entry<String, Map<HttpStatus, LatencyDistribution>> pathEntry : metricEntry.getValue().entrySet()) {

                        for(final Map.Entry<HttpStatus, LatencyDistribution> statusEntry : pathEntry.getValue().entrySet()) {

                            this.addMetricToDump(

                                metricEntities,
                                now,
                                metricEntry.getKey(),
                                pathEntry.getKey(),
                                statusEntry.getValue().getDistribution(),
                                statusEntry.getKey()
                            );
                        }
                    }
                }

                try {

                    metricDao.insert(metricEntities);
                    log.info("Metrics dumping task completed, {} metrics written", metricEntities.size());
                }

                catch(final DatabaseException exc) {

                    throw new NonTransientDataAccessResourceException("Unwrapped", exc.getCause());
                }
            });
        }

        catch(final DataAccessException exc) {

            log.error("Could not perform metrics rollover to database, this batch will be lost", exc);
        }
    }

    ///..
    private void addMetricToDump(

        final List<Metric> metricEntities,
        final long now,
        final Long metricEntryKey,
        final String pathEntryKey,
        final Map<String, Integer> distribution,
        final HttpStatus httpStatus
    ) {

        try {

            metricEntities.add(new Metric(

                -1L,
                now,
                metricEntryKey,
                pathEntryKey,
                jsonMapper.serialize(distribution),
                (short)httpStatus.value()
            ));
        }

        catch(final SerializationException exc) {

            log.error("Could not serialize metric, will skip this one", exc);
        }
    }

    ///
}
