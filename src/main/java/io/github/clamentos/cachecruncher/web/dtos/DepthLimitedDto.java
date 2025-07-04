package io.github.clamentos.cachecruncher.web.dtos;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.DeserializationException;

///..
import io.github.clamentos.cachecruncher.utility.MutableInt;

///.
import java.util.Map;

///..
import java.util.concurrent.ConcurrentHashMap;

///
public abstract class DepthLimitedDto {

    ///
    private static final Map<Long, Map<Class<?>, MutableInt>> depthCounters = new ConcurrentHashMap<>();

    ///
    protected DepthLimitedDto(final Class<?> childClass, final int limit) throws DeserializationException {

        final long threadId = Thread.currentThread().threadId();

        final int depth = depthCounters

            .computeIfAbsent(threadId, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(childClass, _ -> new MutableInt())
            .incrementAndGet(1)
        ;

        if(depth >= limit) {

            this.clear(childClass, threadId);
            throw new DeserializationException(ErrorCode.JSON_TOO_DEEP, childClass.getSimpleName(), limit);
        }
    }

    ///
    protected void clear(final Class<?> childClass) {

        this.clear(childClass, Thread.currentThread().threadId());
    }

    ///.
    private void clear(final Class<?> childClass, final long threadId) {

        final Map<Class<?>, MutableInt> classes = depthCounters.get(threadId);

        classes.remove(childClass);
        if(classes.isEmpty()) depthCounters.remove(threadId);
    }

    ///
}
