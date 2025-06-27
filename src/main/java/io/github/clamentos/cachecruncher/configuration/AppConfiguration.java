package io.github.clamentos.cachecruncher.configuration;

///
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.monitoring.logging.DatabaseLogsWriter;

///..
import io.github.clamentos.cachecruncher.monitoring.status.ApplicationStatusService;

///..
import io.github.clamentos.cachecruncher.utility.MaintenanceService;

///..
import io.github.clamentos.cachecruncher.web.interceptors.AuthFilter;
import io.github.clamentos.cachecruncher.web.interceptors.RateLimiter;

///.
import java.time.Duration;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

///..
import org.springframework.core.Ordered;

///..
import org.springframework.core.env.Environment;

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
        final Environment environment
    ) {

        final SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();

        scheduler.setTargetTaskExecutor(new SimpleAsyncTaskExecutor("CacheCruncherTask-"));
        scheduler.setVirtualThreads(true);

        final String logsTaskCron = environment.getProperty(SCHEDULING + "logsTaskCron", String.class, "0 */5 * * * *");
        final String maintenanceTaskCron = environment.getProperty(SCHEDULING + "maintenanceTaskCron", String.class, "0 0 0 * * *");
        final int metricsTaskRate = environment.getProperty(SCHEDULING + "metricsTaskRate", Integer.class, 1_000);
        final int replenishTaskRate = environment.getProperty(SCHEDULING + "replenishTaskRate", Integer.class, 10_000);
        final String sessionTaskCron = environment.getProperty(SCHEDULING + "sessionTaskCron", String.class, "0 */5 * * * *");

        scheduler.schedule(sessionService::cleanExpiredTask, new CronTrigger(sessionTaskCron));
        scheduler.schedule(databaseLogsWriter::dumpTask, new CronTrigger(logsTaskCron));
        scheduler.schedule(applicationStatusService::rolloverTask, new PeriodicTrigger(Duration.ofMillis(metricsTaskRate)));
        scheduler.schedule(maintenanceService::cleanByRetentionTask, new CronTrigger(maintenanceTaskCron));
        scheduler.schedule(rateLimiter::replenishTask, new PeriodicTrigger(Duration.ofMillis(replenishTaskRate)));

        return scheduler;
    }

    ///..
    @Bean
    public TaskExecutor simulationsExecutor(final Environment environment) {

        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(environment.getProperty("cache-cruncher.simulation.executorPool.minThreads", Integer.class, 1));
        executor.setMaxPoolSize(environment.getProperty("cache-cruncher.simulation.executorPool.maxThreads", Integer.class, 8));
        executor.setQueueCapacity(environment.getProperty("cache-cruncher.simulation.executorPool.maxQueueSize", Integer.class, 1_024));
        executor.setThreadNamePrefix("CacheCruncherSimulator-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setVirtualThreads(false);

        executor.setAwaitTerminationSeconds(environment.getProperty(

            "cache-cruncher.simulation.executorPool.terminationTimeout",
            Integer.class,
            60
        ));

        executor.initialize();
        return executor;
    }

    ///
}
