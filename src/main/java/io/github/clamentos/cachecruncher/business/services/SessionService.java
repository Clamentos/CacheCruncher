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
    private final AtomicInteger totalLoggedUsersCounter;

    ///..
    private final long sessionDuration;
    private final long sessionExpirationMargin;
    private final int sessionIdLength;
    private final int maxSessionsPerUser;
    private final int maxTotalLoggedUsers;

    ///
    @Autowired
    public SessionService(SessionDao sessionDao, Environment environment) {

        this.sessionDao = sessionDao;

        secureRandom = new SecureRandom();
        sessions = new ConcurrentHashMap<>();
        userSessionCounters = new ConcurrentHashMap<>();
        totalLoggedUsersCounter = new AtomicInteger();

        for(Session session : sessionDao.selectAll()) {

            sessions.put(session.getId(), session);
            AtomicInteger userSessionCount = userSessionCounters.get(session.getUserId());

            if(userSessionCount != null) userSessionCount.incrementAndGet();
            else userSessionCounters.put(session.getUserId(), new AtomicInteger(1));

            totalLoggedUsersCounter.incrementAndGet();
        }

        if(sessions.size() > 0) {

            log.info("Recovered {} sessions", sessions.size());
        }

        this.sessionDuration = environment.getProperty("cache-cruncher.auth.sessionDuration", Long.class, 3_600_000L);
        this.sessionExpirationMargin = environment.getProperty("cache-cruncher.auth.sessionExpirationMargin", Long.class, 5_000L);
        this.sessionIdLength = environment.getProperty("cache-cruncher.auth.sessionIdLength", Integer.class, 32);
        this.maxSessionsPerUser = environment.getProperty("cache-cruncher.auth.maxSessionsPerUser", Integer.class, 1);
        this.maxTotalLoggedUsers = environment.getProperty("cache-cruncher.auth.maxTotalLoggedUsers", Integer.class, 100_000);
    }

    ///
    @Transactional
    public Session generate(long userId, String username, boolean isAdmin, String device)
    throws AuthorizationException, DataAccessException {

        byte[] rawSessionId = new byte[sessionIdLength];

        secureRandom.nextBytes(rawSessionId);
        String sessionId = Base64.getEncoder().encodeToString(rawSessionId);
        Session session = new Session(userId, System.currentTimeMillis() + sessionDuration, username, device, sessionId, isAdmin);

        sessionDao.insert(session);

        if(totalLoggedUsersCounter.getAndUpdate(current -> this.updateCounter(current, maxTotalLoggedUsers)) >= maxTotalLoggedUsers) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.TOO_MANY_USERS, maxTotalLoggedUsers));
        }

        AtomicInteger userSessionCount = userSessionCounters.computeIfAbsent(userId, _ -> new AtomicInteger());

        if(userSessionCount.getAndUpdate(current -> this.updateCounter(current, maxSessionsPerUser)) >= maxSessionsPerUser) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.TOO_MANY_SESSIONS, maxSessionsPerUser));
        }

        return session;
    }

    ///..
    public Session check(String sessionId, boolean requiresAdmin, String message)
    throws AuthenticationException, AuthorizationException {

        Session session = sessions.get(sessionId);

        if(session == null) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.SESSION_NOT_FOUND));
        }

        long expiration = session.getExpiresAt() - sessionExpirationMargin;

        if(expiration < System.currentTimeMillis()) {

            throw new AuthenticationException(new ErrorDetails(ErrorCode.EXPIRED_SESSION, expiration));
        }

        if(requiresAdmin && !session.isAdmin()) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.NOT_ENOUGH_PRIVILEGES, message));
        }

        return session;
    }

    ///..
    public void remove(String sessionId) throws AuthenticationException, DataAccessException {

        Session sessionToBeRemoved = sessions.get(sessionId);

        if(sessionToBeRemoved == null) {

            throw new AuthorizationException(new ErrorDetails(ErrorCode.SESSION_NOT_FOUND));
        }

        sessionDao.delete(sessionToBeRemoved.getId());
        Session removed = sessions.remove(sessionId);

        if(removed != null && userSessionCounters.get(sessionToBeRemoved.getUserId()).decrementAndGet() <= 0) {

            userSessionCounters.remove(sessionToBeRemoved.getUserId());
        }
    }

    ///..
    @Transactional
    public void removeAll(long userId) {

        for(Session session : sessions.values()) {

            if(session.getUserId() == userId) {

                try {

                    this.remove(session.getId());
                }

                catch(AuthenticationException _) {

                    log.warn("Session not found");
                }

                catch(DataAccessException exc) {

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
    public int getCurrentlyLoggedUsersCount() {

        return totalLoggedUsersCounter.get();
    }

    ///.
    @Scheduled(cron = "0 */5 * * * *")
    public void removeAllExpired() {

        log.info("Starting expired session cleaning task...");

        Set<String> expiredSessionIds = new HashSet<>();
        long now = System.currentTimeMillis();

        int expiredCount = 0;
        int deletedFromDbCount = 0;
        int deletedFromSessionsCount = 0;

        for(Map.Entry<String, Session> session : sessions.entrySet()) {

            if(session.getValue().getExpiresAt() < now) {

                expiredSessionIds.add(session.getValue().getId());
                expiredCount++;
            }
        }

        try {

            deletedFromDbCount = sessionDao.deleteAll(expiredSessionIds);

            for(String expiredSessionId : expiredSessionIds) {

                Session removedSession = sessions.remove(expiredSessionId);

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

        catch(DataAccessException exc) {

            log.error("Could not complete the expired session task, {}: {}", exc.getClass().getSimpleName(), exc.getMessage());
        }
    }

    ///.
    private int updateCounter(int current, int limit) {

        int attempt = current + 1;
        return attempt <= limit ? attempt : current;
    }

    ///
}
