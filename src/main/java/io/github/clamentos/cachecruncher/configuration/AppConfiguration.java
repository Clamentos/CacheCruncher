package io.github.clamentos.cachecruncher.configuration;

///
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

///
@Configuration
@EnableAsync
@EnableScheduling

///
public class AppConfiguration {

    ///
    @Bean
    public SimpleAsyncTaskScheduler taskScheduler() {

        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();

        scheduler.setTargetTaskExecutor(new SimpleAsyncTaskExecutor("CacheCruncherScheduledTask-"));
        scheduler.setVirtualThreads(true);

        return scheduler;
    }

    ///..
    @Bean
    public TaskExecutor simulationsExecutor(Environment environment) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(environment.getProperty("cache-cruncher.simulation.executor_pool.min_threads", Integer.class, 4));
        executor.setMaxPoolSize(environment.getProperty("cache-cruncher.simulation.executor_pool.max_threads", Integer.class, 8));
        executor.setQueueCapacity(environment.getProperty("cache-cruncher.simulation.executor_pool.max_queue_size", Integer.class, 4096));
        executor.setThreadNamePrefix("CacheCruncherSimulator-");
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.setAwaitTerminationSeconds(environment.getProperty(

            "cache-cruncher.simulation.executor_pool.termination_timeout",
            Integer.class,
            60
        ));

        executor.initialize();
        return executor;
    }

    ///
}
