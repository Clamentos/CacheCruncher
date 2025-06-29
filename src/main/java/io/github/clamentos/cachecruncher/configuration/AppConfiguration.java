package io.github.clamentos.cachecruncher.configuration;

///
import io.github.clamentos.cachecruncher.business.services.MaintenanceService;
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.monitoring.logging.DatabaseLogsWriter;

///..
import io.github.clamentos.cachecruncher.monitoring.status.ApplicationStatusService;

///..
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///..
import io.github.clamentos.cachecruncher.web.interceptors.AuthFilter;
import io.github.clamentos.cachecruncher.web.interceptors.RateLimiter;

///.
import java.time.Duration;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

///..
import org.springframework.core.Ordered;

///..
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

///..
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

///..
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

///..
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.PeriodicTrigger;

///..
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

///
@Configuration
@EnableAsync
@EnableScheduling

///
public class AppConfiguration implements WebMvcConfigurer {

    ///
    private static final String SCHEDULING = "cache-cruncher.scheduling.";

    ///..
    private final AuthFilter authFilter;

    ///
    @Autowired
    public AppConfiguration(final AuthFilter authFilter) {

        this.authFilter = authFilter;
    }

    ///
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {

		registry.addInterceptor(authFilter).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE);
	}

    ///
    @Bean
    public SimpleAsyncTaskScheduler taskScheduler(

        final SessionService sessionService,
        final DatabaseLogsWriter databaseLogsWriter,
        final ApplicationStatusService applicationStatusService,
        final MaintenanceService maintenanceService,
        final RateLimiter rateLimiter,
        final PropertyProvider propertyProvider

    ) throws BeanCreationException {

        final SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();

        scheduler.setTargetTaskExecutor(new SimpleAsyncTaskExecutor("CacheCruncherTask-"));
        scheduler.setVirtualThreads(true);

        final String logsTaskCron = propertyProvider.getString(SCHEDULING + "logsTaskCron", "0 */5 * * * *");
        final String maintenanceTaskCron = propertyProvider.getString(SCHEDULING + "maintenanceTaskCron", "0 0 0 * * *");
        final int metricsTaskRate = propertyProvider.getInteger(SCHEDULING + "metricsTaskRate", 1_000, 1_000, Integer.MAX_VALUE);
        final int replenishTaskRate = propertyProvider.getInteger(SCHEDULING + "replenishTaskRate", 10_000, 1_000, Integer.MAX_VALUE);
        final String sessionTaskCron = propertyProvider.getString(SCHEDULING + "sessionTaskCron", "0 */5 * * * *");

        scheduler.schedule(sessionService::cleanExpiredTask, new CronTrigger(sessionTaskCron));
        scheduler.schedule(databaseLogsWriter::dumpTask, new CronTrigger(logsTaskCron));
        scheduler.schedule(applicationStatusService::rolloverTask, new PeriodicTrigger(Duration.ofMillis(metricsTaskRate)));
        scheduler.schedule(maintenanceService::cleanByRetentionTask, new CronTrigger(maintenanceTaskCron));
        scheduler.schedule(rateLimiter::replenishTask, new PeriodicTrigger(Duration.ofMillis(replenishTaskRate)));

        return scheduler;
    }

    ///..
    @Bean
    public TaskExecutor simulationsExecutor(final PropertyProvider propertyProvider) throws BeanCreationException {

        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(propertyProvider.getInteger(

            "cache-cruncher.simulation.executorPool.minThreads",
            1, 1, Integer.MAX_VALUE
        ));

        executor.setMaxPoolSize(propertyProvider.getInteger(

            "cache-cruncher.simulation.executorPool.maxThreads",
            8, 1, Integer.MAX_VALUE
        ));

        executor.setQueueCapacity(propertyProvider.getInteger(

            "cache-cruncher.simulation.executorPool.maxQueueSize",
            1_000, 1, Integer.MAX_VALUE
        ));

        executor.setThreadNamePrefix("CacheCruncherSimulator-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setVirtualThreads(false);

        executor.setAwaitTerminationSeconds(propertyProvider.getInteger(

            "cache-cruncher.simulation.executorPool.terminationTimeout",
            60, 1, Integer.MAX_VALUE
        ));

        executor.initialize();
        return executor;
    }

    ///
}
