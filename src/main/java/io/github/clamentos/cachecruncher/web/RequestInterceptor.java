package io.github.clamentos.cachecruncher.web;

///
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorDetails;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;

///..
import io.github.clamentos.cachecruncher.utility.ErrorMessages;

///.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
public class RequestInterceptor implements HandlerInterceptor {

    ///
    private static final String ATTRIBUTE_NAME = "session";
    private static final int COOKIE_LENGTH = 60;
    private static final int COOKIE_PREFIX_LENGTH = 16;

    ///.
    private final SessionService sessionService;

    ///..
    private final AuthMappings authMappings;

    ///..
    private final String gatewaySecret;
    private final boolean bypass;

    ///..
    private final boolean checkSecret;

    ///
    @Autowired
    public RequestInterceptor(final SessionService sessionService, final AuthMappings authMappings, final Environment environment) {

        this.sessionService = sessionService;
        this.authMappings = authMappings;

        gatewaySecret = environment.getProperty("cache-cruncher.gateway.secret", String.class);
        bypass = environment.getProperty("cache-cruncher.auth.bypass", Boolean.class, false);

        checkSecret = gatewaySecret != null && !gatewaySecret.isEmpty();
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws AuthenticationException, AuthorizationException {

        final String path = request.getMethod() + request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        if(!bypass && !path.contains("/**")) {

            if(checkSecret && !gatewaySecret.equals(request.getHeader("Authorization"))) throw this.fail();

            if(authMappings.requiresAuthentication(path)) {

                String authCookie = request.getHeader("Cookie");

                if(authCookie == null || authCookie.length() != COOKIE_LENGTH || !authCookie.startsWith("sessionIdCookie")) {

                    throw this.fail();
                }

                final Session session = sessionService.check(

                    authCookie.substring(COOKIE_PREFIX_LENGTH),
                    authMappings.requiresAdminPrivilege(path),
                    ErrorMessages.NOT_ENOUGH_PRIVILEGES
                );

                request.setAttribute(ATTRIBUTE_NAME, session);
            }

            else {

                request.removeAttribute(ATTRIBUTE_NAME);
            }
        }

        return true;
	}

    ///..
    private AuthenticationException fail() {

        return new AuthenticationException(new ErrorDetails(ErrorCode.INVALID_AUTH_HEADER));
    }

    ///
}
