package io.github.clamentos.cachecruncher.web.interceptors;

///
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;
import io.github.clamentos.cachecruncher.error.exceptions.UnprocessableRequestException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;

///..
import io.github.clamentos.cachecruncher.utility.ErrorMessages;

///.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.net.InetAddress;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

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
    private static final String ATTRIBUTE_NAME = "session";
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
        final Environment environment
    ) {

        this.sessionService = sessionService;
        this.rateLimiter = rateLimiter;
        this.authMappings = authMappings;

        gatewaySecret = environment.getProperty("cache-cruncher.gateway.secret", String.class);
        bypassAuth = environment.getProperty("cache-cruncher.auth.bypass", Boolean.class, false);
        rateLimitingEnabled = environment.getProperty("cache-cruncher.rate-limiter.enabled", Boolean.class, false);
        retryDelay = environment.getProperty("cache-cruncher.rate-limiter.retryDelay", Long.class, 60_000L);

        checkSecret = gatewaySecret != null && !gatewaySecret.isEmpty();
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws AuthenticationException, AuthorizationException, UnprocessableRequestException {

        final String path = request.getMethod() + request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if(!authMappings.exists(path)) throw new UnprocessableRequestException(new ErrorDetails(ErrorCode.API_NOT_FOUND));

        if(checkSecret && !gatewaySecret.equals(request.getHeader("Authorization"))) {

            throw new AuthenticationException(new ErrorDetails(ErrorCode.INVALID_AUTH_HEADER));
        }

        if(!authMappings.requiresAuthentication(path)) {

            this.rateLimitByIp(request.getHeader("X-Real-IP"));
            request.removeAttribute(ATTRIBUTE_NAME);
        }

        else if(!bypassAuth) {

            final Session session = this.checkSession(request.getHeader("Cookie"), path);
            this.rateLimitBySession(session.getId());
            request.setAttribute(ATTRIBUTE_NAME, session);
        }

        return true;
	}

    ///.
    private void rateLimitByIp(final String ipHeader) throws UnprocessableRequestException {

        if(rateLimitingEnabled && !rateLimiter.consumeByIp(this.parseIp(ipHeader))) {

            throw this.failRateLimit(rateLimiter.getTokenCountByIp());
        }
    }

    ///..
    private InetAddress parseIp(final String ipHeader) throws UnprocessableRequestException {

        if(ipHeader == null || ipHeader.isEmpty()) throw new UnprocessableRequestException(new ErrorDetails(ErrorCode.REAL_IP_MISSING));

        try { return InetAddress.ofLiteral(ipHeader); }
        catch(IllegalArgumentException _) { throw new UnprocessableRequestException(new ErrorDetails(ErrorCode.REAL_IP_MISSING)); }
    }

    ///..
    private UnprocessableRequestException failRateLimit(final int count) {

        return new UnprocessableRequestException(new ErrorDetails(ErrorCode.TOO_MANY_REQUESTS, count, retryDelay));
    }

    ///..
    private Session checkSession(final String authCookie, final String path) throws AuthenticationException, AuthorizationException {

        if(authCookie == null || authCookie.length() != COOKIE_LENGTH || !authCookie.startsWith("sessionIdCookie")) {

            throw new AuthenticationException(new ErrorDetails(ErrorCode.INVALID_AUTH_HEADER));
        }

        return sessionService.check(

            authCookie.substring(COOKIE_PREFIX_LENGTH),
            authMappings.requiresAdminPrivilege(path),
            ErrorMessages.NOT_ENOUGH_PRIVILEGES
        );
    }

    ///..
    private void rateLimitBySession(final String sessionId) throws UnprocessableRequestException {

        if(rateLimitingEnabled && !rateLimiter.consumeBySession(sessionId)) {

            throw this.failRateLimit(rateLimiter.getTokenCountBySession());
        }
    }

    ///
}
