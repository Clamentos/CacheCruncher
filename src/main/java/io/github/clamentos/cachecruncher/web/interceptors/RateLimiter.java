package io.github.clamentos.cachecruncher.web.interceptors;

///
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.net.InetAddress;

///..
import java.util.Map;

///..
import java.util.concurrent.ConcurrentHashMap;

///..
import java.util.concurrent.atomic.AtomicInteger;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.stereotype.Component;

///.
import lombok.Getter;

///
@Component

///
public final class RateLimiter {

    ///
    private final Map<InetAddress, AtomicInteger> requestCountersByIp;
    private final Map<String, AtomicInteger> requestCountersBySession;

    @Getter
    private final int tokenCountByIp;

    @Getter
    private final int tokenCountBySession;

    ///
    @Autowired
    public RateLimiter(final PropertyProvider propertyProvider) throws BeanCreationException {

        requestCountersByIp = new ConcurrentHashMap<>();
        requestCountersBySession = new ConcurrentHashMap<>();

        tokenCountByIp = propertyProvider.getInteger("cache-cruncher.rate-limiter.tokenCountByIp", 500, 1, Integer.MAX_VALUE);
        tokenCountBySession = propertyProvider.getInteger("cache-cruncher.rate-limiter.tokenCountBySession", 100, 1, Integer.MAX_VALUE);
    }

    ///
    public boolean consumeByIp(final InetAddress ipAddress) {

        return this.consume(requestCountersByIp, ipAddress, tokenCountByIp);
    }

    ///..
    public boolean consumeBySession(final String sessionId) {

        return this.consume(requestCountersBySession, sessionId, tokenCountBySession);
    }

    ///..
    public void replenishTask() {

        this.replenish(requestCountersByIp, tokenCountByIp);
        this.replenish(requestCountersBySession, tokenCountBySession);
    }

    ///.
    private <T> boolean consume(final Map<T, AtomicInteger> counters, final T key, final int tokenCount) {

        return counters.computeIfAbsent(key, _ -> new AtomicInteger(tokenCount)).decrementAndGet() >= 0;
    }

    ///..
    private <T> void replenish(final Map<T, AtomicInteger> counters, final int tokenCount) {

        for(final Map.Entry<T, AtomicInteger> entry : counters.entrySet()) {

            if(entry.getValue().get() < 0) counters.remove(entry.getKey());
            else entry.getValue().set(tokenCount);
        }
    }

    ///
}
