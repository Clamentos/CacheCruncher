package io.github.clamentos.cachecruncher;

///
import java.util.Map;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class TestContext {

    ///
    private final String body;
    private final Map<String, String> parameters;

    ///..
    private final int expectedStatus;
    private final Map<String, Object> expectedValues;

    ///
    public TestContext(int expectedStatus) {

        this(null, Map.of(), expectedStatus, Map.of());
    }

    ///..
    public TestContext(Map<String, String> parameters, int expectedStatus) {

        this(null, parameters, expectedStatus, Map.of());
    }

    ///..
    public TestContext(Map<String, String> parameters, int expectedStatus, Map<String, Object> expectedValues) {

        this(null, parameters, expectedStatus, expectedValues);
    }

    ///
}
