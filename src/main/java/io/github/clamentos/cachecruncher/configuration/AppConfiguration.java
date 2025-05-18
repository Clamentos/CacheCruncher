package io.github.clamentos.cachecruncher.configuration;

///
import io.github.clamentos.cachecruncher.web.RequestInterceptor;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
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
    private final RequestInterceptor requestInterceptor;

    ///
    @Autowired
    public AppConfiguration(RequestInterceptor requestInterceptor) {

        this.requestInterceptor = requestInterceptor;
    }

    ///
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

		registry.addInterceptor(requestInterceptor).addPathPatterns("/**");
	}

    ///
    @Bean
    public SimpleAsyncTaskScheduler taskScheduler() {

        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();

        scheduler.setTargetTaskExecutor(new SimpleAsyncTaskExecutor("CacheCruncherTask-"));
        scheduler.setVirtualThreads(true);

        return scheduler;
    }

    ///..
    @Bean
    public TaskExecutor simulationsExecutor(Environment environment) {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(environment.getProperty("cache-cruncher.simulation.executorPool.minThreads", Integer.class, 4));
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
