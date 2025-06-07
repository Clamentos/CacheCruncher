package io.github.clamentos.cachecruncher.business.simulation.cache.commands;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///..
@AllArgsConstructor
@Getter

///
public sealed class CacheCommand permits CacheCommandTypeA, CacheCommandTypeB, CacheCommandTypeC {

    ///
    private final CacheCommandType type;

    ///
    public byte[] toByteArray() {

        return new byte[] {(byte)type.ordinal()};
    }

    ///..
    @Override
    public String toString() {

        return type.getType();
    }

    ///
}
