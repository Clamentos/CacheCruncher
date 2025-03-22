package io.github.clamentos.cachecruncher.configuration;

///
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

///..
import org.springframework.core.task.SimpleAsyncTaskExecutor;

///..
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

///..
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

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

    ///
}
