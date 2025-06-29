package io.github.clamentos.cachecruncher.business.services;

///
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.CacheCruncherException;
import io.github.clamentos.cachecruncher.error.exceptions.DatabaseException;
import io.github.clamentos.cachecruncher.persistence.UserRole;
///..
import io.github.clamentos.cachecruncher.persistence.daos.SessionDao;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;

///..
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import java.security.SecureRandom;

///..
import java.util.Base64;
import java.util.Collection;
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
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

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
    private final int sessionIdLength;
    private final int maxSessionsPerUser;
    private final int maxTotalSessions;

    ///
    @Autowired
    public SessionService(final SessionDao sessionDao, final PropertyProvider propertyProvider)
    throws BeanCreationException, DatabaseException {

        this.sessionDao = sessionDao;

        secureRandom = new SecureRandom();
        sessions = new ConcurrentHashMap<>();
        userSessionCounters = new ConcurrentHashMap<>();
        sessionsCounter = new AtomicInteger();

        this.sessionDuration = propertyProvider.getLong("cache-cruncher.auth.sessionDuration", 3_600_000L, 60_000L, Long.MAX_VALUE);
        this.sessionIdLength = propertyProvider.getInteger("cache-cruncher.auth.sessionIdLength", 32, 16, Integer.MAX_VALUE);
        this.maxSessionsPerUser = propertyProvider.getInteger("cache-cruncher.auth.maxSessionsPerUser", 2, 1, Integer.MAX_VALUE);
        this.maxTotalSessions = propertyProvider.getInteger("cache-cruncher.auth.maxTotalSessions", 25_000, 1, Integer.MAX_VALUE);

        sessionDao.deleteExpired();

        for(final Session session : sessionDao.selectAll()) {

            sessions.put(session.getId(), session);
            userSessionCounters.computeIfAbsent(session.getUserId(), _ -> new AtomicInteger()).incrementAndGet();
            sessionsCounter.incrementAndGet();
        }

        if(sessions.size() > 0) log.info("Recovered {} sessions", sessions.size());
    }

    ///
    @Transactional(rollbackFor = CacheCruncherException.class)
    public Session generate(final long userId, final String email, final UserRole role, final String device)
    throws AuthorizationException, DatabaseException {

        if(sessionsCounter.getAndUpdate(current -> this.updateCounter(current, maxTotalSessions)) >= maxTotalSessions) {

            throw new AuthorizationException(ErrorCode.TOO_MANY_OVERALL_SESSIONS, maxTotalSessions);
        }

        final AtomicInteger userSessionCount = userSessionCounters.computeIfAbsent(userId, _ -> new AtomicInteger());

        if(userSessionCount.getAndUpdate(current -> this.updateCounter(current, maxSessionsPerUser)) >= maxSessionsPerUser) {

            throw new AuthorizationException(ErrorCode.TOO_MANY_SESSIONS, maxSessionsPerUser);
        }

        final byte[] rawSessionId = new byte[sessionIdLength];
        secureRandom.nextBytes(rawSessionId);

        final String sessionId = Base64.getEncoder().encodeToString(rawSessionId);
        final Session session = new Session(userId, System.currentTimeMillis() + sessionDuration, email, device, sessionId, role);

        try {

            sessionDao.insert(session);
        }

        catch(final DatabaseException exc) {

            sessionsCounter.decrementAndGet();
            userSessionCount.decrementAndGet();

            throw exc;
        }

        sessions.put(sessionId, session);
        return session;
    }

    ///..
    public Session check(final String sessionId, final UserRole minimumRole, final String message)
    throws AuthenticationException, AuthorizationException {

        final Session session = sessions.get(sessionId);
        if(session == null) throw new AuthenticationException(ErrorCode.SESSION_NOT_FOUND);

        if(session.isExpired(System.currentTimeMillis())) {

            throw new AuthenticationException(ErrorCode.EXPIRED_SESSION, session.getExpiresAt());
        }

        if(session.getRole().ordinal() < minimumRole.ordinal()) {

            throw new AuthorizationException(ErrorCode.NOT_ENOUGH_PRIVILEGES, message);
        }

        return session;
    }

    ///..
    @Transactional(rollbackFor = CacheCruncherException.class)
    public void remove(final String sessionId) throws AuthenticationException, DatabaseException {

        final Session sessionToBeRemoved = sessions.get(sessionId);
        if(sessionToBeRemoved == null) throw new AuthenticationException(ErrorCode.SESSION_NOT_FOUND);

        sessionDao.delete(sessionToBeRemoved.getId());

        final Session removed = sessions.remove(sessionId);
        final long userId = sessionToBeRemoved.getUserId();

        if(removed != null && userSessionCounters.get(userId).decrementAndGet() <= 0) userSessionCounters.remove(userId);
        sessionsCounter.decrementAndGet();
    }

    ///..
    public Collection<Session> getSessions() {

        return sessions.values();
    }

    ///..
    public int getSessionsCount() {

        return sessionsCounter.get();
    }

    ///..
    public int getLoggedUsersCount() {

        return userSessionCounters.size();
    }

    ///..
    public void cleanExpiredTask() {

        log.info("Starting expired session cleaning task...");

        final Set<String> expiredSessionIds = new HashSet<>();
        final long now = System.currentTimeMillis();

        for(final Map.Entry<String, Session> session : sessions.entrySet()) {

            if(session.getValue().isExpired(now)) {

                expiredSessionIds.add(session.getValue().getId());
            }
        }

        try {

            int deletedFromSessionsCount = 0;
            final int deletedFromDbCount = sessionDao.deleteAll(expiredSessionIds);

            for(final String expiredSessionId : expiredSessionIds) {

                final Session removedSession = sessions.remove(expiredSessionId);
                deletedFromSessionsCount++;

                if(removedSession != null && userSessionCounters.get(removedSession.getUserId()).decrementAndGet() <= 0) {

                    userSessionCounters.remove(removedSession.getUserId());
                }
            }

            log.info(

                "Expired session cleaning task completed, expired: {}, deletedFromDb: {}, deletedFromSessions: {}",
                expiredSessionIds.size(), deletedFromDbCount, deletedFromSessionsCount
            );
        }

        catch(final DatabaseException exc) {

            log.error("Could not complete the expired session task, {}: {}", exc.getClass().getSimpleName(), exc.getMessage());
        }
    }

    ///.
    private int updateCounter(final int current, final int limit) {

        final int attempt = current + 1;
        return attempt <= limit ? attempt : current;
    }

    ///
}
