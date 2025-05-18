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
    private static final int COOKIE_PREFIX_LENGTH = 16;
    private static final int COOKIE_LENGTH = 60;

    ///.
    private final SessionService sessionService;
    private final AuthMappings authMappings;

    ///..
    private final String gatewaySecret;
    private final boolean bypass;
    private final boolean checkSecret;

    ///
    @Autowired
    public RequestInterceptor(SessionService sessionService, AuthMappings authMappings, Environment environment) {

        this.sessionService = sessionService;
        this.authMappings = authMappings;

        gatewaySecret = environment.getProperty("cache-cruncher.gateway.secret", String.class);
        bypass = environment.getProperty("cache-cruncher.auth.bypass", Boolean.class, false);

        checkSecret = gatewaySecret != null && !gatewaySecret.isEmpty();
    }

    ///
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws AuthenticationException, AuthorizationException {

        if(!bypass) {

            String path = request.getMethod() + request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String gatewaySecretHeader = request.getHeader("Authorization");
            String authCookie = request.getHeader("Cookie");

            if(checkSecret && !gatewaySecret.equals(gatewaySecretHeader)) {

                this.fail();
            }

            if(authCookie != null && authCookie.length() >= COOKIE_LENGTH && authCookie.startsWith("sessionIdCookie")) {

                authCookie = authCookie.substring(COOKIE_PREFIX_LENGTH);
            }

            if(authMappings.requiresAuthentication(path)) {

                if(authCookie != null) {

                    Session session = sessionService.check(

                        authCookie,
                        authMappings.requiresAdminPrivilege(path),
                        "Not enough privileges to call this API"
                    );

                    request.setAttribute(ATTRIBUTE_NAME, session);
                }

                else {

                    this.fail();
                }
            }

            else {

                request.removeAttribute(ATTRIBUTE_NAME);
            }
        }

        return true;
	}

    ///..
    private void fail() throws AuthenticationException {

        throw new AuthenticationException(new ErrorDetails(ErrorCode.INVALID_AUTH_HEADER));
    }

    ///
}
