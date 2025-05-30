package io.github.clamentos.cachecruncher.configuration;

///
import java.util.Map;
import java.util.TreeMap;

///..
import java.util.concurrent.ConcurrentHashMap;

///.
import lombok.Getter;

///.
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

///..
import org.springframework.stereotype.Component;

///..
import org.springframework.web.bind.annotation.RequestMethod;

///..
import org.springframework.web.method.HandlerMethod;

///..
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

///..
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

///
@Component
@Getter

///
public class StartupActions {

    ///
    private final Map<String, Integer> uriIdMap;

    ///
    public StartupActions() {

        uriIdMap = new ConcurrentHashMap<>();
    }

    ///
    @EventListener
    public void handleContextRefresh(final ContextRefreshedEvent contextRefreshedEvent) {

        final Map<RequestMappingInfo, HandlerMethod> mappings = contextRefreshedEvent

            .getApplicationContext()
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class)
            .getHandlerMethods()
        ;

        final TreeMap<String, Integer> sortedMap = new TreeMap<>();
        int counter = 0;

        for(final Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.entrySet()) {

            for(final RequestMethod method : entry.getKey().getMethodsCondition().getMethods()) {

                for(final String path : entry.getKey().getDirectPaths()) {

                    sortedMap.put(method.toString() + path, counter++);
                }
            }
        }

        uriIdMap.putAll(sortedMap);
    }

    ///
}
