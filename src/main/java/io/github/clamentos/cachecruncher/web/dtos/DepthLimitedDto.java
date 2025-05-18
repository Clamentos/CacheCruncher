package io.github.clamentos.cachecruncher.web.dtos;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///.
import java.util.Map;

///..
import java.util.concurrent.ConcurrentHashMap;

///..
import java.util.concurrent.atomic.AtomicInteger;

///
public abstract class DepthLimitedDto {

    ///
    private static final Map<Long, Map<Class<?>, AtomicInteger>> depthCounters = new ConcurrentHashMap<>();

    ///
    protected DepthLimitedDto(Class<?> childClass, int limit) {

        long threadId = Thread.currentThread().threadId();

        int depth = depthCounters

            .computeIfAbsent(threadId, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(childClass, _ -> new AtomicInteger())
            .incrementAndGet()
        ;

        if(depth >= limit) {

            this.clear(childClass, threadId);
            throw new IllegalStateException(new ErrorDetails(ErrorCode.JSON_TOO_DEEP, childClass.getSimpleName(), limit));
        }
    }

    ///
    protected void clear(Class<?> childClass) {

        this.clear(childClass, Thread.currentThread().threadId());
    }

    ///.
    private void clear(Class<?> childClass, long threadId) {

        Map<Class<?>, AtomicInteger> classes = depthCounters.get(threadId);
        classes.remove(childClass);

        if(classes.isEmpty()) {

            depthCounters.remove(threadId);
        }
    }

    ///
}
