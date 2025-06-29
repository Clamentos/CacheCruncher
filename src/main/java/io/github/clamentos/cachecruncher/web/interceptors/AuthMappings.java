package io.github.clamentos.cachecruncher.web.interceptors;

///
import io.github.clamentos.cachecruncher.persistence.UserRole;

///.
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.stereotype.Component;

///
@Component

///
public class AuthMappings {

    ///
    private final Set<String> authenticationExcludedPaths;
    private final Map<String, UserRole> authorizationMappings;

    ///
    @Autowired
    public AuthMappings() {

        authenticationExcludedPaths = new HashSet<>();

        authenticationExcludedPaths.add("POST/cache-cruncher/user/register");
        authenticationExcludedPaths.add("GET/cache-cruncher/user/resend-email");
        authenticationExcludedPaths.add("GET/cache-cruncher/user/confirm-email");
        authenticationExcludedPaths.add("POST/cache-cruncher/user/login");

        authorizationMappings = new HashMap<>();

        authorizationMappings.put("POST/cache-cruncher/user/refresh", UserRole.DEFAULT);
        authorizationMappings.put("DELETE/cache-cruncher/user/logout", UserRole.DEFAULT);
        authorizationMappings.put("DELETE/cache-cruncher/user/logout-all", UserRole.DEFAULT);
        authorizationMappings.put("GET/cache-cruncher/user", UserRole.ADMIN);
        authorizationMappings.put("PATCH/cache-cruncher/user", UserRole.ADMIN);
        authorizationMappings.put("DELETE/cache-cruncher/user", UserRole.DEFAULT);

        authorizationMappings.put("GET/cache-cruncher/status/metrics", UserRole.ADMIN);
        authorizationMappings.put("GET/cache-cruncher/status/metrics/history", UserRole.ADMIN);
        authorizationMappings.put("GET/cache-cruncher/status/logs", UserRole.ADMIN);
        authorizationMappings.put("GET/cache-cruncher/status/logs/count", UserRole.ADMIN);
        authorizationMappings.put("DELETE/cache-cruncher/status/metrics/history", UserRole.ADMIN);
        authorizationMappings.put("DELETE/cache-cruncher/status/logs", UserRole.ADMIN);

        authorizationMappings.put("GET/cache-cruncher/cache-trace/simulation", UserRole.DEFAULT);

        authorizationMappings.put("POST/cache-cruncher/cache-trace", UserRole.UPLOADER);
        authorizationMappings.put("GET/cache-cruncher/cache-trace", UserRole.DEFAULT);
        authorizationMappings.put("GET/cache-cruncher/cache-trace/search", UserRole.DEFAULT);
        authorizationMappings.put("PATCH/cache-cruncher/cache-trace", UserRole.UPLOADER);
        authorizationMappings.put("DELETE/cache-cruncher/cache-trace", UserRole.UPLOADER);
    }

    ///
    public boolean exists(final String path) {

        return authenticationExcludedPaths.contains(path) || authorizationMappings.containsKey(path);
    }

    ///..
    public boolean requiresAuthentication(final String path) {

        return !authenticationExcludedPaths.contains(path);
    }

    ///..
    public UserRole getMinimumRole(final String path) {

        return authorizationMappings.getOrDefault(path, UserRole.ADMIN);
    }

    ///
}
