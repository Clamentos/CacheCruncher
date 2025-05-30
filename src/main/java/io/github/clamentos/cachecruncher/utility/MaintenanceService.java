package io.github.clamentos.cachecruncher.utility;

///
import io.github.clamentos.cachecruncher.persistence.daos.LogDao;
import io.github.clamentos.cachecruncher.persistence.daos.MetricDao;

///.
import java.time.ZonedDateTime;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
///..
import org.springframework.scheduling.annotation.Scheduled;

///..
import org.springframework.stereotype.Service;

///
@Service
@Slf4j

///
public class MaintenanceService {

    ///
    private final LogDao logDao;
    private final MetricDao metricDao;

    ///..
    private final long logsRetention;
    private final long metricsRetention;

    ///
    @Autowired
    public MaintenanceService(final LogDao logDao, final MetricDao metricDao, final Environment environment) {

        this.logDao = logDao;
        this.metricDao = metricDao;

        logsRetention = environment.getProperty("cache-cruncher.monitoring.status.logsRetention", Long.class, 604_800_000L);
        metricsRetention = environment.getProperty("cache-cruncher.monitoring.status.metricsRetention", Long.class, 604_800_000L);
    }

    ///
    @Scheduled(cron = "0 0 0 * * *", scheduler = "taskScheduler")
    protected void dump() {

        log.info("Starting maintenance task...");

        try {

            final long logsRetentionLimit = ZonedDateTime.now().minusDays(logsRetention).toInstant().toEpochMilli();
            final long metricsRetentionLimit = ZonedDateTime.now().minusDays(metricsRetention).toInstant().toEpochMilli();

            final int deletedLogs = logDao.delete(Long.MIN_VALUE, logsRetentionLimit);
            final int deletedMetrics = metricDao.delete(Long.MIN_VALUE, metricsRetentionLimit);

            log.info("Maintenance task completed, {} logs deleted, {} metrics deleted", deletedLogs, deletedMetrics);
        }

        catch(final DataAccessException exc) {

            log.error("Could not perform maintenance, will abort the job", exc);
        }
    }

    ///
}
