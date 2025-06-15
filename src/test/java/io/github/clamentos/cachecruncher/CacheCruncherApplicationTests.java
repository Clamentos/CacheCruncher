package io.github.clamentos.cachecruncher;

///
import com.fasterxml.jackson.core.type.TypeReference;

///..
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import io.github.clamentos.cachecruncher.context.TestContext;

///.
import java.io.IOException;

///..
import java.nio.file.Files;

///..
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
import org.springframework.core.io.ClassPathResource;

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
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application_test.properties")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

///
class CacheCruncherApplicationTests extends TestSources {

	///
	private final ApplicationApis applicationApis;
	private final ObjectMapper objectMapper;

	///
	@Autowired
	public CacheCruncherApplicationTests(final ApplicationApis applicationApis, final ObjectMapper objectMapper) throws IOException {

		this.applicationApis = applicationApis;
		this.objectMapper = objectMapper;

		final String testData = Files.readString(new ClassPathResource("test_data/TestData.json").getFile().toPath());
		super.fill(objectMapper.readValue(testData, new TypeReference<Map<String, Map<String, Map<String, TestContext>>>>(){}));
	}

	///
	@Order(0)
	@ParameterizedTest
    @MethodSource("registerSource") // Also test the email confirmation.
    public void register(final Map<String, TestContext> contexts) throws Exception {

		final TestContext registerAction = contexts.get("registerAction");
		final MockHttpServletResponse registrationResponse = applicationApis.register(registerAction.getBody());
		this.checkResponse(registerAction, registrationResponse);

		final TestContext confirmEmailAction = contexts.get("confirmEmailAction");

		if(registrationResponse.getStatus() == 200 && confirmEmailAction != null) {

			final String token = registrationResponse.getContentAsString();
			final MockHttpServletResponse validationResponse = applicationApis.confirmEmail(token);
			this.checkResponse(confirmEmailAction, validationResponse);
		}
	}

	///..
	@Order(1)
	@ParameterizedTest
    @MethodSource("loginSource")
    public void login(final Map<String, TestContext> contexts) throws Exception {

		final TestContext loginAction = contexts.get("loginAction");
		this.checkResponse(loginAction, applicationApis.login(loginAction.getBody()));
	}

	///..
	// Refresh session

	///..
	// logout
	// logout all
	// updatePrivilege
	// deleteUser

	///..
	@Order(2)
	@ParameterizedTest
    @MethodSource("getAllUsersSource")
    public void getAllUsers(final Map<String, TestContext> contexts) throws Exception {

		final TestContext loginAction = contexts.get("loginAction");
		String sessionCookie = null;

		if(loginAction != null) {

			final MockHttpServletResponse loginResponse = applicationApis.login(loginAction.getBody());
			this.checkResponse(loginAction, loginResponse);
			sessionCookie = loginResponse.getHeader("Set-Cookie");
		}

		this.checkResponse(contexts.get("getAllUsersAction"), applicationApis.getAllUsers(sessionCookie));
	}

	///.
	private void checkResponse(final TestContext context, final MockHttpServletResponse response) throws Exception {

		Assertions.assertEquals(context.getExpectedStatus(), response.getStatus());
        final Map<String, Object> expectedValues = context.getExpectedValues();

		if(expectedValues != null && !expectedValues.isEmpty()) {

			final JsonNode responseJson = objectMapper.readTree(response.getContentAsString());

			for(final Map.Entry<String, Object> entry : expectedValues.entrySet()) {

				final JsonNode toCheck = responseJson.at(entry.getKey());
				final Object expectedValue = entry.getValue();

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
