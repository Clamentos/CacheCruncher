package io.github.clamentos.cachecruncher.web.interceptors;

///
import java.net.InetAddress;

///..
import java.util.Map;

///..
import java.util.concurrent.ConcurrentHashMap;

///..
import java.util.concurrent.atomic.AtomicInteger;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

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
    public RateLimiter(final Environment environment) {

        requestCountersByIp = new ConcurrentHashMap<>();
        requestCountersBySession = new ConcurrentHashMap<>();

        tokenCountByIp = environment.getProperty("cache-cruncher.rate-limiter.tokenCountByIp", Integer.class, 500);
        tokenCountBySession = environment.getProperty("cache-cruncher.rate-limiter.tokenCountBySession", Integer.class, 100);
    }

    ///
    public boolean consumeByIp(final InetAddress ipAddress) {

        final int count = requestCountersByIp.computeIfAbsent(ipAddress, _ -> new AtomicInteger(tokenCountByIp)).decrementAndGet();
        return count >= 0;
    }

    ///..
    public boolean consumeBySession(final String sessionId) {

        final int count = requestCountersBySession

            .computeIfAbsent(sessionId, _ -> new AtomicInteger(tokenCountBySession))
            .decrementAndGet()
        ;

        return count >= 0;
    }

    ///..
    public void replenishTask() {

        this.replenish(requestCountersByIp, tokenCountByIp);
        this.replenish(requestCountersBySession, tokenCountBySession);
    }

    ///.
    private <T> void replenish(final Map<T, AtomicInteger> counters, final int tokenCount) {

        for(final Map.Entry<T, AtomicInteger> entry : counters.entrySet()) {

            if(entry.getValue().get() < 0) counters.remove(entry.getKey());
            else entry.getValue().set(tokenCount);
        }
    }

    ///
}
