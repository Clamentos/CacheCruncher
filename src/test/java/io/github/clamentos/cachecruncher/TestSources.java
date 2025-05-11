package io.github.clamentos.cachecruncher;

///
import java.util.Map;

///..
import java.util.stream.Stream;

///.
import org.junit.jupiter.params.provider.Arguments;

///
public class TestSources {

    ///
    public static Stream<Arguments> registerSource() {

        return Stream.of(

            Arguments.of(new TestContext(Map.of("email", "testUser@testMail.com", "password", "Password123?!"), 200)),
            Arguments.of(new TestContext(Map.of("email", "testUser@testMail.com", "password", "Password123?!"), 409)),
            Arguments.of(new TestContext(Map.of("email", "testUser@testMail.com"), 400)),
            Arguments.of(new TestContext(Map.of("password", "Password123?!"), 400)),
            Arguments.of(new TestContext(400)),
            Arguments.of(new TestContext(Map.of("email", "testUser@testMail.com", "password", "Psw1"), 400)),
            Arguments.of(new TestContext(Map.of("email", "testUser@testMail", "password", "Password123?!"), 400))
        );
    }

    ///..
    public static Stream<Arguments> loginSource() {

        return Stream.of(

            Arguments.of(new TestContext(Map.of("email", "admin@test.com", "password", "Password123?!"), 200))
        );
    }

    ///
}
