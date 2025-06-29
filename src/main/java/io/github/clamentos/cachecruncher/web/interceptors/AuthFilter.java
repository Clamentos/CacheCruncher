package io.github.clamentos.cachecruncher.web.interceptors;

///
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.UnprocessableRequestException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;

///..
import io.github.clamentos.cachecruncher.utility.ErrorMessages;
import io.github.clamentos.cachecruncher.utility.PropertyProvider;

///.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.net.InetAddress;

///.
import org.springframework.beans.factory.BeanCreationException;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.stereotype.Component;

///..
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

///
@Component

///
public class AuthFilter implements HandlerInterceptor {

    ///
    private static final String SESSION_ATTRIBUTE_NAME = "session";
    private static final int COOKIE_LENGTH = 60;
    private static final int COOKIE_PREFIX_LENGTH = 16;

    ///.
    private final SessionService sessionService;

    ///..
    private final RateLimiter rateLimiter;
    private final AuthMappings authMappings;

    ///..
    private final String gatewaySecret;
    private final boolean bypassAuth;
    private final boolean rateLimitingEnabled;
    private final long retryDelay;

    ///..
    private final boolean checkSecret;

    ///
    @Autowired
    public AuthFilter(

        final SessionService sessionService,
        final RateLimiter rateLimiter,
        final AuthMappings authMappings,
        final PropertyProvider propertyProvider

    ) throws BeanCreationException {

        this.sessionService = sessionService;
        this.rateLimiter = rateLimiter;
        this.authMappings = authMappings;

        gatewaySecret = propertyProvider.getString("cache-cruncher.auth.gatewaySecret", null);
        bypassAuth = propertyProvider.getBoolean("cache-cruncher.auth.bypass", false);
        rateLimitingEnabled = propertyProvider.getBoolean("cache-cruncher.rate-limiter.enabled", false);
        retryDelay = propertyProvider.getLong("cache-cruncher.rate-limiter.retryDelay", 60_000L, 100L, Long.MAX_VALUE);

        checkSecret = gatewaySecret != null && !gatewaySecret.isEmpty();
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws AuthenticationException, AuthorizationException, UnprocessableRequestException {

        final String path = request.getMethod() + request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if(!authMappings.exists(path)) throw new UnprocessableRequestException(ErrorCode.API_NOT_FOUND, path);

        if(checkSecret && !gatewaySecret.equals(request.getHeader("Authorization"))) {

            throw new AuthenticationException(ErrorCode.INVALID_AUTH_HEADER);
        }

        if(!authMappings.requiresAuthentication(path)) {

            this.rateLimitByIp(request.getHeader("X-Real-IP"));
            request.removeAttribute(SESSION_ATTRIBUTE_NAME);
        }

        else if(!bypassAuth) {

            final Session session = this.checkSession(request.getHeader("Cookie"), path);

            this.rateLimitBySession(session.getId());
            request.setAttribute(SESSION_ATTRIBUTE_NAME, session);
        }

        return true;
	}

    ///.
    private void rateLimitByIp(final String ipHeader) throws UnprocessableRequestException {

        if(rateLimitingEnabled && !rateLimiter.consumeByIp(this.parseIp(ipHeader))) {

            throw new UnprocessableRequestException(ErrorCode.TOO_MANY_REQUESTS, rateLimiter.getTokenCountByIp(), retryDelay);
        }
    }

    ///..
    private InetAddress parseIp(final String ipHeader) throws UnprocessableRequestException {

        if(ipHeader == null || ipHeader.isEmpty()) throw new UnprocessableRequestException(ErrorCode.REAL_IP_MISSING);

        try { return InetAddress.ofLiteral(ipHeader); }
        catch(final IllegalArgumentException exc) { throw new UnprocessableRequestException(ErrorCode.REAL_IP_MISSING, exc); }
    }

    ///..
    private Session checkSession(final String authCookie, final String path) throws AuthenticationException, AuthorizationException {

        if(authCookie == null || authCookie.length() != COOKIE_LENGTH || !authCookie.startsWith("sessionIdCookie")) {

            throw new AuthenticationException(ErrorCode.INVALID_AUTH_HEADER);
        }

        return sessionService.check(

            authCookie.substring(COOKIE_PREFIX_LENGTH),
            authMappings.getMinimumRole(path),
            ErrorMessages.NOT_ENOUGH_PRIVILEGES
        );
    }

    ///..
    private void rateLimitBySession(final String sessionId) throws UnprocessableRequestException {

        if(rateLimitingEnabled && !rateLimiter.consumeBySession(sessionId)) {

            throw new UnprocessableRequestException(ErrorCode.TOO_MANY_REQUESTS, rateLimiter.getTokenCountBySession(), retryDelay);
        }
    }

    ///
}
