package io.github.clamentos.cachecruncher;

///
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import java.util.Map;

///.
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

///..
import org.junit.jupiter.params.ParameterizedTest;

///..
import org.junit.jupiter.params.provider.MethodSource;

///..
import org.springframework.beans.factory.annotation.Autowired;

///..
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

///..
import org.springframework.boot.test.context.SpringBootTest;

///..
import org.springframework.mock.web.MockHttpServletResponse;

///..
import org.springframework.test.context.TestPropertySource;

///..
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

///
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = {"/testing_schema.sql"}, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@TestInstance(value = TestInstance.Lifecycle.PER_METHOD)
@TestPropertySource(locations = "classpath:application_test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

///
class CacheCruncherApplicationTests extends TestSources {

	/*
		POST/cache-cruncher/user/login
		DELETE/cache-cruncher/user/logout
		DELETE/cache-cruncher/user/logout-all
		GET/cache-cruncher/user
		PATCH/cache-cruncher/user
		DELETE/cache-cruncher/user
		GET/cache-cruncher/status/metrics
		GET/cache-cruncher/status/metrics/history
		GET/cache-cruncher/status/logs
		GET/cache-cruncher/status/logs/count
		DELETE/cache-cruncher/status/metrics/history
		DELETE/cache-cruncher/status/logs
		GET/cache-cruncher/simulation
		POST/cache-cruncher/trace
		GET/cache-cruncher/trace
		GET/cache-cruncher/trace/search
		PATCH/cache-cruncher/trace
		DELETE/cache-cruncher/trace
	*/

	///
	private final ApplicationApis applicationApis;
	private final ObjectMapper objectMapper;

	///
	@Autowired
	public CacheCruncherApplicationTests(ApplicationApis applicationApis, ObjectMapper objectMapper) {

		this.applicationApis = applicationApis;
		this.objectMapper = objectMapper;
	}

	///
	@Order(0)
	@ParameterizedTest
    @MethodSource("registerSource")
    public void register(TestContext context) throws Exception {

		MockHttpServletResponse response = applicationApis.register(context.getParameters());
        this.checkResponse(context, response);
	}

	///..
	@Order(1)
	@ParameterizedTest
    @MethodSource("loginSource")
    public void login(TestContext context) throws Exception {

		MockHttpServletResponse response = applicationApis.login(context.getParameters());
        this.checkResponse(context, response);
	}

	///.
	private void checkResponse(TestContext context, MockHttpServletResponse response) throws Exception {

		Assertions.assertEquals(context.getExpectedStatus(), response.getStatus());
        Map<String, Object> expectedValues = context.getExpectedValues();

		if(expectedValues != null && !expectedValues.isEmpty()) {

			JsonNode responseJson = objectMapper.readTree(response.getContentAsString());

			for(Map.Entry<String, Object> entry : expectedValues.entrySet()) {

				JsonNode toCheck = responseJson.at(entry.getKey());
				Object expectedValue = entry.getValue();
				Object toCheckValue = null;

				if(toCheck != null && !toCheck.isMissingNode()) {

					if(expectedValue instanceof String) toCheckValue = toCheck.asText();
					else if(expectedValue instanceof Number) toCheckValue = toCheck.asLong();
					else if(expectedValue instanceof Boolean) toCheckValue = toCheck.asBoolean();
				}

				Assertions.assertEquals(toCheckValue, expectedValue);
			}
		}
	}

	///
}
