package io.github.clamentos.cachecruncher.context;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import java.util.Map;

///.
import lombok.Getter;

///
@Getter

///
public final class TestContext {

    ///
    private final Map<String, Object> body;
    private final Map<String, Object> parameters;

    ///..
    private final int expectedStatus;
    private final Map<String, Object> expectedValues;

    ///
    @JsonCreator
    public TestContext(

        @JsonProperty("body") final Map<String, Object> body,
        @JsonProperty("parameters") final Map<String, Object> parameters,
        @JsonProperty("expectedStatus") final int expectedStatus,
        @JsonProperty("expectedValues") final Map<String, Object> expectedValues
    ) {

        this.body = body;
        this.parameters = parameters;
        this.expectedStatus = expectedStatus;
        this.expectedValues = expectedValues;
    }

    ///
}
