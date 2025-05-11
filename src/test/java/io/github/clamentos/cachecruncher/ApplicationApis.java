package io.github.clamentos.cachecruncher;

///
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;

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
public final class ApplicationApis {

    ///
    private final MockMvc mockMvc;

    ///..
    private final String baseUrl;

    ///
    @Autowired
    public ApplicationApis(MockMvc mockMvc) {

        this.mockMvc = mockMvc;
        baseUrl = "http://localhost:8080/cache-cruncher";
    }

    ///
    public MockHttpServletResponse register(Map<String, String> parameters) throws Exception {

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders

            .post(baseUrl + "/user/register")
            .param("email", parameters.get("email"))
            .param("password", parameters.get("password"))
        ;

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///..
    public MockHttpServletResponse login(Map<String, String> parameters) throws Exception {

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders

            .post(baseUrl + "/user/login")
            .param("email", parameters.get("email"))
            .param("password", parameters.get("password"))
            .header("User-Agent", "junit-tests")
        ;

        return mockMvc.perform(builder).andReturn().getResponse();
    }

    ///
}
