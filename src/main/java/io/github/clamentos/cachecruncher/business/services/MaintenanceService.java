package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.LogDao;
import io.github.clamentos.cachecruncher.persistence.daos.MetricDao;

///..
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.time.ZonedDateTime;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

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
    public MaintenanceService(final LogDao logDao, final MetricDao metricDao, final PropertyProvider propertyProvider)
    throws BeanCreationException {

        this.logDao = logDao;
        this.metricDao = metricDao;

        logsRetention = propertyProvider.getLong("cache-cruncher.monitoring.status.logsRetention", 604_800_000L, 0L, Long.MAX_VALUE);

        metricsRetention = propertyProvider.getLong(

            "cache-cruncher.monitoring.status.metricsRetention",
            604_800_000L, 0L, Long.MAX_VALUE
        );
    }

    ///
    public void cleanByRetentionTask() {

        log.info("Starting maintenance task...");

        try {

            final long logsRetentionLimit = ZonedDateTime.now().minusDays(logsRetention).toInstant().toEpochMilli();
            final long metricsRetentionLimit = ZonedDateTime.now().minusDays(metricsRetention).toInstant().toEpochMilli();

            final int deletedLogs = logDao.delete(Long.MIN_VALUE, logsRetentionLimit);
            final int deletedMetrics = metricDao.delete(Long.MIN_VALUE, metricsRetentionLimit);

            log.info("Maintenance task completed, {} logs deleted, {} metrics deleted", deletedLogs, deletedMetrics);
        }

        catch(final DatabaseException exc) {

            log.error("Could not perform maintenance, will abort the job", exc);
        }
    }

    ///
}
