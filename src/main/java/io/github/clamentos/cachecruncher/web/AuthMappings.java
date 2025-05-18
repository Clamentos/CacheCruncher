package io.github.clamentos.cachecruncher.web;

///
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
    private final Map<String, Boolean> authorizationMappings;

    ///
    @Autowired
    public AuthMappings() {

        authenticationExcludedPaths = new HashSet<>();

        authenticationExcludedPaths.add("POST/cache-cruncher/user/register");
        authenticationExcludedPaths.add("POST/cache-cruncher/user/login");

        authorizationMappings = new HashMap<>();

        authorizationMappings.put("DELETE/cache-cruncher/user/logout", false);
        authorizationMappings.put("DELETE/cache-cruncher/user/logout-all", false);
        authorizationMappings.put("GET/cache-cruncher/user", true);
        authorizationMappings.put("PATCH/cache-cruncher/user", true);
        authorizationMappings.put("DELETE/cache-cruncher/user", false);

        authorizationMappings.put("GET/cache-cruncher/status/metrics", true);
        authorizationMappings.put("GET/cache-cruncher/status/metrics/history", true);
        authorizationMappings.put("GET/cache-cruncher/status/logs", true);
        authorizationMappings.put("GET/cache-cruncher/status/logs/count", true);
        authorizationMappings.put("DELETE/cache-cruncher/status/metrics/history", true);
        authorizationMappings.put("DELETE/cache-cruncher/status/logs", true);

        authorizationMappings.put("GET/cache-cruncher/simulation", false);

        authorizationMappings.put("POST/cache-cruncher/cache-trace", false);
        authorizationMappings.put("GET/cache-cruncher/cache-trace", false);
        authorizationMappings.put("GET/cache-cruncher/cache-trace/search", false);
        authorizationMappings.put("PATCH/cache-cruncher/cache-trace", true);
        authorizationMappings.put("DELETE/cache-cruncher/cache-trace", true);
    }

    ///
    public boolean requiresAuthentication(String path) {

        return !authenticationExcludedPaths.contains(path);
    }

    ///..
    public boolean requiresAdminPrivilege(String path) {

        return authorizationMappings.getOrDefault(path, true);
    }

    ///
}
