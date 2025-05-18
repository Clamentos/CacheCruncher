package io.github.clamentos.cachecruncher.utility;

///
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

///
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter

///
public final class Triple<A, B, C> {

    ///
    private A a;
    private B b;
    private C c;

    ///
}
