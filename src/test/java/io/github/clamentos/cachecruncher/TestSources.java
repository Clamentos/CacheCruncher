package io.github.clamentos.cachecruncher;

///
import io.github.clamentos.cachecruncher.context.TestContext;

///.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

///..
import java.util.stream.Stream;

///.
import org.junit.jupiter.params.provider.Arguments;

///
public class TestSources {

    ///
    // method source name -> case name -> step name -> step
    private static final Map<String, Map<String, Map<String, TestContext>>> contexts = new HashMap<>();

    ///
    public static Stream<Arguments> registerSource() {

        return createStream("registerSource");
    }

    ///..
    public static Stream<Arguments> loginSource() {

        return createStream("loginSource");
    }

    ///..
    public static Stream<Arguments> getAllUsersSource() {

        return createStream("getAllUsersSource");
    }

    ///.
    protected static void fill(final Map<String, Map<String, Map<String, TestContext>>> inputContexts) {

        contexts.putAll(inputContexts);
    }

    ///.
    private static Stream<Arguments> createStream(final String sourceName) {

        final Map<String, Map<String, TestContext>> context = contexts.get(sourceName);
        final List<Arguments> arguments = new ArrayList<>(context.size());

        for(final Map.Entry<String, Map<String, TestContext>> run : context.entrySet()) {

            arguments.add(Arguments.of(run.getValue()));
        }

        return arguments.stream();
    }

    ///
}
