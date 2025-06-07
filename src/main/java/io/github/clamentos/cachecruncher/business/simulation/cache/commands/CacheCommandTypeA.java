package io.github.clamentos.cachecruncher.business.simulation.cache.commands;

///
import lombok.Getter;

///
@Getter

///
public final class CacheCommandTypeA extends CacheCommand {

    ///
    private final long size;

    ///
    public CacheCommandTypeA(final CacheCommandType commandType, final long size) {

        super(commandType);
        this.size = size;
    }

    ///
    @Override
    public byte[] toByteArray() {

        return new byte[] {

            (byte)super.getType().ordinal(),
            (byte)((size & 0xFF00000000000000L) >>> 56),
            (byte)((size & 0x00FF000000000000L) >>> 48),
            (byte)((size & 0x0000FF0000000000L) >>> 40),
            (byte)((size & 0x000000FF00000000L) >>> 32),
            (byte)((size & 0x00000000FF000000L) >>> 24),
            (byte)((size & 0x0000000000FF0000L) >>> 16),
            (byte)((size & 0x000000000000FF00L) >>> 8),
            (byte)(size & 0x00000000000000FFL)
        };
    }

    ///..
    @Override
    public String toString() {

        return super.toString() + size;
    }

    ///
}
