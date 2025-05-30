package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;

///..
import io.github.clamentos.cachecruncher.persistence.daos.SessionDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;

///.
import java.security.SecureRandom;

///..
import java.util.Base64;
import java.util.HashSet;

///..
import java.util.Map;
import java.util.Set;

///..
import java.util.concurrent.ConcurrentHashMap;

///..
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.dao.DataAccessException;

///..
import org.springframework.scheduling.annotation.Scheduled;

///..
import org.springframework.stereotype.Service;

///..
import org.springframework.transaction.annotation.Transactional;

///
@Service
@Slf4j

///
public class SessionService {

    ///
    private final SessionDao sessionDao;

    ///..
    private final SecureRandom secureRandom;

    ///..
    private final Map<String, Session> sessions;
    private final Map<Long, AtomicInteger> userSessionCounters;
    private final AtomicInteger sessionsCounter;

    ///..
    private final long sessionDuration;
    private final long sessionExpirationMargin;
    private final int sessionIdLength;
    private final int maxSessionsPerUser;
    private final int maxTotalSessions;

    ///
    @Autowired
    public SessionService(final SessionDao sessionDao, final Environment environment) {

        this.sessionDao = sessionDao;

        secureRandom = new SecureRandom();
        sessions = new ConcurrentHashMap<>();
        userSessionCounters = new ConcurrentHashMap<>();
        sessionsCounter = new AtomicInteger();

        for(final Session session : sessionDao.selectAll()) {

            sessions.put(session.getId(), session);
            userSessionCounters.computeIfAbsent(session.getUserId(), _ -> new AtomicInteger()).incrementAndGet();
            sessionsCounter.incrementAndGet();
        }

        if(sessions.size() > 0) log.info("Recovered {} sessions", sessions.size());

        this.sessionDuration = environment.getProperty("cache-cruncher.auth.sessionDuration", Long.class, 3_600_000L);
        this.sessionExpirationMargin = environment.getProperty("cache-cruncher.auth.sessionExpirationMargin", Long.class, 5_000L);
        this.sessionIdLength = environment.getProperty("cache-cruncher.auth.sessionIdLength", Integer.class, 32);
        this.maxSessionsPerUser = environment.getProperty("cache-cruncher.auth.maxSessionsPerUser", Integer.class, 1);
        this.maxTotalSessions = environment.getProperty("cache-cruncher.auth.maxTotalSessions", Integer.class, 25_000);
    }

    ///
    @Transactional
    public Session generate(final long userId, final String username, final boolean isAdmin, final String device)
    throws AuthorizationException, DataAccessException {

        if(sessionsCounter.getAndUpdate(current -> this.updateCounter(current, maxTotalSessions)) >= maxTotalSessions) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.TOO_MANY_OVERALL_SESSIONS, maxTotalSessions));
        }

        final AtomicInteger userSessionCount = userSessionCounters.computeIfAbsent(userId, _ -> new AtomicInteger());

        if(userSessionCount.getAndUpdate(current -> this.updateCounter(current, maxSessionsPerUser)) >= maxSessionsPerUser) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.TOO_MANY_SESSIONS, maxSessionsPerUser));
        }

        final byte[] rawSessionId = new byte[sessionIdLength];
        secureRandom.nextBytes(rawSessionId);

        final String sessionId = Base64.getEncoder().encodeToString(rawSessionId);
        final Session session = new Session(userId, System.currentTimeMillis() + sessionDuration, username, device, sessionId, isAdmin);

        try {

            sessionDao.insert(session);
        }

        catch(final DataAccessException exc) {

            sessionsCounter.decrementAndGet();
            userSessionCount.decrementAndGet();

            throw exc;
        }

        sessions.put(sessionId, session);
        return session;
    }

    ///..
    public Session check(final String sessionId, final boolean requiresAdmin, final String message)
    throws AuthenticationException, AuthorizationException {

        final Session session = sessions.get(sessionId);
        if(session == null) throw new AuthorizationException(new ErrorDetails(ErrorCode.SESSION_NOT_FOUND));

        final long expiration = session.getExpiresAt() - sessionExpirationMargin;

        if(expiration < System.currentTimeMillis()) {

            throw new AuthenticationException(new ErrorDetails(ErrorCode.EXPIRED_SESSION, expiration));
        }

        if(requiresAdmin && !session.isAdmin()) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.NOT_ENOUGH_PRIVILEGES, message));
        }

        return session;
    }

    ///..
    public void remove(final String sessionId) throws AuthenticationException, DataAccessException {

        final Session sessionToBeRemoved = sessions.get(sessionId);
        if(sessionToBeRemoved == null) throw new AuthorizationException(new ErrorDetails(ErrorCode.SESSION_NOT_FOUND));

        sessionDao.delete(sessionToBeRemoved.getId());

        final Session removed = sessions.remove(sessionId);
        final long userId = sessionToBeRemoved.getUserId();

        if(removed != null && userSessionCounters.get(userId).decrementAndGet() <= 0) userSessionCounters.remove(userId);
        sessionsCounter.decrementAndGet();
    }

    ///..
    @Transactional
    public void removeAll(final long userId) {

        for(final Session session : sessions.values()) {

            if(session.getUserId() == userId) {

                try { this.remove(session.getId()); }
                catch(final AuthenticationException _) { log.warn("Session not found"); }

                catch(final DataAccessException exc) {

                    log.error(

                        "Could not remove session because of {}: {}, will skip this one",
                        exc.getClass().getSimpleName(),
                        exc.getMessage()
                    );
                }
            }
        }
    }

    ///..
    public int getSessionsCount() {

        return sessionsCounter.get();
    }

    ///..
    public int getLoggedUsersCount() {

        return userSessionCounters.size();
    }

    ///.
    @Scheduled(cron = "0 */5 * * * *")
    protected void removeAllExpired() {

        log.info("Starting expired session cleaning task...");

        final Set<String> expiredSessionIds = new HashSet<>();
        final long now = System.currentTimeMillis();

        int expiredCount = 0;
        int deletedFromDbCount = 0;
        int deletedFromSessionsCount = 0;

        for(final Map.Entry<String, Session> session : sessions.entrySet()) {

            if(session.getValue().getExpiresAt() < now) {

                expiredSessionIds.add(session.getValue().getId());
                expiredCount++;
            }
        }

        try {

            deletedFromDbCount = sessionDao.deleteAll(expiredSessionIds);

            for(final String expiredSessionId : expiredSessionIds) {

                final Session removedSession = sessions.remove(expiredSessionId);

                if(removedSession != null && userSessionCounters.get(removedSession.getUserId()).decrementAndGet() <= 0) {

                    userSessionCounters.remove(removedSession.getUserId());
                    deletedFromSessionsCount++;
                }
            }

            log.info(

                "Expired session cleaning task completed, expired: {}, deletedFromDb: {}, deletedFromSessions: {}",
                expiredCount, deletedFromDbCount, deletedFromSessionsCount
            );
        }

        catch(final DataAccessException exc) {

            log.error("Could not complete the expired session task, {}: {}", exc.getClass().getSimpleName(), exc.getMessage());
        }
    }

    ///.
    private int updateCounter(final int current, final int limit) {

        int attempt = current + 1;
        return attempt <= limit ? attempt : current;
    }

    ///
}
