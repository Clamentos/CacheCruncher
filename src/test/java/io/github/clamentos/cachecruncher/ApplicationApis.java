package io.github.clamentos.cachecruncher;

///
import io.github.clamentos.cachecruncher.utility.JsonMapper;

///.
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.core.env.Environment;

///..
import org.springframework.mock.web.MockHttpServletResponse;

///..
import org.springframework.stereotype.Component;

///..
import org.springframework.test.web.servlet.MockMvc;

///..
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

///
@Component

///
public class ApplicationApis {

    ///
    private static final String JSON_TYPE = "application/json";

    ///.
    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    ///..
    private final String baseUrl;

    ///
    @Autowired
    public ApplicationApis(final MockMvc mockMvc, final JsonMapper jsonMapper, final Environment environment) {

        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;

        final String port = environment.getProperty("server.port", String.class);
        baseUrl = "http://localhost:" + port + "/cache-cruncher";
    }

    ///
    public MockHttpServletResponse register(final Map<String, Object> payload) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(baseUrl + "/user/register");

        this.prepare(builder, null, JSON_TYPE);
        builder.content(jsonMapper.serialize(payload));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    // resend email

    ///..
    public MockHttpServletResponse confirmEmail(final String token) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/user/confirm-email");

        this.prepare(builder, null, null);
        builder.param("token", token);

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse login(final Map<String, Object> payload) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(baseUrl + "/user/login");

        this.prepare(builder, null, JSON_TYPE);
        builder.content(jsonMapper.serialize(payload));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse refreshSession(final String sessionId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(baseUrl + "/user/refresh");
        this.prepare(builder, sessionId, null);

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse logout(final String sessionId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(baseUrl + "/user/logout");
        this.prepare(builder, sessionId, null);

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse logoutAll(final String sessionId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(baseUrl + "/user/logout-all");
        this.prepare(builder, sessionId, null);

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse getAllUsers(final String sessionId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/user");
        this.prepare(builder, sessionId, null);

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse updatePrivilege(final String sessionId, final long userId, boolean isAdmin) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch(baseUrl + "/user");

        this.prepare(builder, sessionId, null);
        builder.param("userId", Long.toString(userId));
        builder.param("isAdmin", Boolean.toString(isAdmin));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse deleteUser(final String sessionId, final long userId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(baseUrl + "/user");

        this.prepare(builder, sessionId, null);
        builder.param("userId", Long.toString(userId));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///.
    public MockHttpServletResponse createCacheTrace(final String sessionId, final Map<String, Object> payload) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(baseUrl + "/cache-trace");

        this.prepare(builder, sessionId, JSON_TYPE);
        builder.content(jsonMapper.serialize(payload));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse getCacheTrace(final String sessionId, final long traceId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/cache-trace");

        this.prepare(builder, sessionId, null);
        builder.param("traceId", Long.toString(traceId));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse searchCacheTraces(

        final String sessionId,
        final String name,
        final long createdAtStart,
        final long createdAtEnd,
        final Long updatedAtStart,
        final Long updatedAtEnd
    
    ) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/cache-trace/search");
        this.prepare(builder, sessionId, null);

        if(sessionId != null) builder.param("sessionId", sessionId);
        if(name != null) builder.param("name", name);
        builder.param("createdAtStart", Long.toString(createdAtStart));
        builder.param("createdAtEnd", Long.toString(createdAtEnd));
        if(updatedAtStart != null) builder.param("updatedAtStart", Long.toString(updatedAtStart));
        if(updatedAtEnd != null) builder.param("updatedAtEnd", Long.toString(updatedAtEnd));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse updateCacheTrace(final String sessionId, final Map<String, Object> payload) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch(baseUrl + "/cache-trace");

        this.prepare(builder, sessionId, JSON_TYPE);
        builder.content(jsonMapper.serialize(payload));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse deleteCacheTrace(final String sessionId, final long traceId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(baseUrl + "/cache-trace");

        this.prepare(builder, sessionId, null);
        builder.param("traceId", Long.toString(traceId));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///.
    public MockHttpServletResponse simulateCacheTrace(final String sessionId, final Map<String, Object> payload) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/cache-trace/simulate");

        this.prepare(builder, sessionId, JSON_TYPE);
        builder.content(jsonMapper.serialize(payload));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///.
    public MockHttpServletResponse getMetrics(

        final String sessionId,
        final boolean includeRuntimeInfo,
        final boolean includeMemoryInfo,
        final boolean includeThreadsInfo,
        final boolean includeResponsesInfo,
        final boolean includeSimulationInfo,
        final boolean includeSessionsInfo

    ) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/status/metrics");

        this.prepare(builder, sessionId, null);
        builder.param("includeRuntimeInfo", Boolean.toString(includeRuntimeInfo));
        builder.param("includeMemoryInfo", Boolean.toString(includeMemoryInfo));
        builder.param("includeThreadsInfo", Boolean.toString(includeThreadsInfo));
        builder.param("includeResponsesInfo", Boolean.toString(includeResponsesInfo));
        builder.param("includeSimulationInfo", Boolean.toString(includeSimulationInfo));
        builder.param("includeSessionsInfo", Boolean.toString(includeSessionsInfo));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse getMetricsHistory(final String sessionId, final Map<String, Object> payload) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/status/metrics/history");

        this.prepare(builder, sessionId, JSON_TYPE);
        builder.content(jsonMapper.serialize(payload));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse getLogs(final String sessionId, final Map<String, Object> payload) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/status/logs");

        this.prepare(builder, sessionId, JSON_TYPE);
        builder.content(jsonMapper.serialize(payload));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse countLogs(final String sessionId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(baseUrl + "/status/logs/count");
        this.prepare(builder, sessionId, null);

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse deleteMetricsHistory(final String sessionId, final long createdAtStart, final long createdAtEnd)
    throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(baseUrl + "/status/metrics/history");

        this.prepare(builder, sessionId, null);
        builder.param("createdAtStart", Long.toString(createdAtStart));
        builder.param("createdAtEnd", Long.toString(createdAtEnd));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse deleteLogs(final String sessionId, final long createdAtStart, final long createdAtEnd)
    throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(baseUrl + "/status/logs");

        this.prepare(builder, sessionId, null);
        builder.param("createdAtStart", Long.toString(createdAtStart));
        builder.param("createdAtEnd", Long.toString(createdAtEnd));

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///.
    private void prepare(final MockHttpServletRequestBuilder builder, final String setCookieHeader, final String contentType) {

        builder.header("User-Agent", "junit-tests");
        if(contentType != null) builder.header("Content-Type", contentType);

        if(setCookieHeader != null) {

            final String sessionId = setCookieHeader.split(";")[0];
            builder.header("Cookie", sessionId);
        }
    }

    ///
}
