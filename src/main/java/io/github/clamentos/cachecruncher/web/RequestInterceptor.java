package io.github.clamentos.cachecruncher.web;

///
import io.github.clamentos.cachecruncher.business.services.SessionService;

///..
import io.github.clamentos.cachecruncher.error.ErrorCode;
import io.github.clamentos.cachecruncher.error.ErrorFactory;

///..
import io.github.clamentos.cachecruncher.error.exceptions.AuthenticationException;
import io.github.clamentos.cachecruncher.error.exceptions.AuthorizationException;

///..
import io.github.clamentos.cachecruncher.persistence.entities.Session;

///.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.util.Map;
import java.util.Set;

///.
import org.springframework.beans.factory.annotation.Autowired;

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
    private static final int COOKIE_VALUE_LENGTH = 44;

    ///..
    private final SessionService sessionService;

    ///..
    private final Set<String> authenticationExcludedPaths;
    private final Map<String, Boolean> authorizationMappings;

    ///
    @Autowired
    public RequestInterceptor(

        SessionService sessionService,
        Set<String> authenticationExcludedPaths,
        Map<String, Boolean> authorizationMappings
    ) {

        this.sessionService = sessionService;
        this.authenticationExcludedPaths = authenticationExcludedPaths;
        this.authorizationMappings = authorizationMappings;
    }

    ///
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    throws AuthenticationException, AuthorizationException {

        String key = request.getMethod() + request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String header = request.getHeader("Cookie");

        if(header != null && header.length() >= (COOKIE_PREFIX_LENGTH + COOKIE_VALUE_LENGTH) && header.startsWith("sessionIdCookie")) {

            header = header.substring(COOKIE_PREFIX_LENGTH);
        }

        if(!authenticationExcludedPaths.contains(key)) {

            if(header != null) {

                Session session = sessionService.check(header, authorizationMappings.get(key), "Not enough privileges to call this API");
                request.setAttribute(ATTRIBUTE_NAME, session);
            }

            else {

                throw new AuthenticationException(ErrorFactory.create(

                    ErrorCode.INVALID_AUTH_HEADER,
                    "RequestInterceptor::preHandle -> Bad or missing auth header"
                ));
            }
        }

        else {

            request.removeAttribute(ATTRIBUTE_NAME);
        }

        return true;
	}

    ///
}
