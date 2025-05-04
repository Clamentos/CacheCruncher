package io.github.clamentos.cachecruncher.configuration;

///
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.web.RequestInterceptor;

///.
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private final SessionService sessionService;

    ///..
    private final Set<String> authenticationExcludedPaths;
    private final Map<String, Boolean> authorizationMappings;

    ///
    @Autowired
    public AppConfiguration(SessionService sessionService) {

        this.sessionService = sessionService;

        authenticationExcludedPaths = new HashSet<>();

        authenticationExcludedPaths.add("POST/cache-cruncher/user/register");
        authenticationExcludedPaths.add("POST/cache-cruncher/user/login");

        authorizationMappings = new HashMap<>();

        authorizationMappings.put("DELETE/cache-cruncher/user/logout", false);
        authorizationMappings.put("DELETE/cache-cruncher/user/logout-all", false);
        authorizationMappings.put("GET/cache-cruncher/user", true);
        authorizationMappings.put("PATCH/cache-cruncher/user", true);
        authorizationMappings.put("DELETE/cache-cruncher/user", false);

        authorizationMappings.put("GET/cache-cruncher/status/metrics", true);
        authorizationMappings.put("GET/cache-cruncher/status/metrics/history", true);
        authorizationMappings.put("GET/cache-cruncher/status/logs", true);
        authorizationMappings.put("GET/cache-cruncher/status/logs/count", true);
        authorizationMappings.put("DELETE/cache-cruncher/status/metrics/history", true);
        authorizationMappings.put("DELETE/cache-cruncher/status/logs", true);

        authorizationMappings.put("GET/cache-cruncher/simulation", false);

        authorizationMappings.put("POST/cache-cruncher/trace", false);
        authorizationMappings.put("GET/cache-cruncher/trace", false);
        authorizationMappings.put("GET/cache-cruncher/trace/search", false);
        authorizationMappings.put("PATCH/cache-cruncher/trace", true);
        authorizationMappings.put("DELETE/cache-cruncher/trace", true);
    }

    ///
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

		registry

            .addInterceptor(new RequestInterceptor(

                sessionService,
                authenticationExcludedPaths,
                authorizationMappings
            ))
            .addPathPatterns("/**")
        ;
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
        executor.setQueueCapacity(environment.getProperty("cache-cruncher.simulation.executorPool.maxQueueSize", Integer.class, 1024));
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
