package io.github.clamentos.cachecruncher.business.simulation.cache;

///
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public class CacheLine {

    ///
    private boolean valid;
    private boolean dirty;
    private int tag;

    ///
}
