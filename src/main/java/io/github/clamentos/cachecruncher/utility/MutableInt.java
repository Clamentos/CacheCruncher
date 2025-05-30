package io.github.clamentos.cachecruncher.utility;

///
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

///
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Getter
@Setter

///
public final class MutableInt {

    ///
    private int value;

    ///
    public int incrementAndGet(final int amount) {

        value += amount;
        return value;
    }

    ///
}
