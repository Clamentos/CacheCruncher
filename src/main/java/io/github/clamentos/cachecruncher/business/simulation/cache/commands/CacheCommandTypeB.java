package io.github.clamentos.cachecruncher.business.simulation.cache.commands;

///
import lombok.Getter;

///
@Getter

///
public final class CacheCommandTypeB extends CacheCommand {

    ///
    private final short size;
    private final long value;

    ///
    public CacheCommandTypeB(final CacheCommandType commandType, final short size, final long value) {

        super(commandType);

        this.size = size;
        this.value = value;
    }

    ///
    @Override
    public byte[] toByteArray() {

        return new byte[] {

            (byte)super.getType().ordinal(),
            (byte)((size & 0xFF00) >> 8),
            (byte)(size & 0x00FF),
            (byte)((value & 0xFF00000000000000L) >>> 56),
            (byte)((value & 0x00FF000000000000L) >>> 48),
            (byte)((value & 0x0000FF0000000000L) >>> 40),
            (byte)((value & 0x000000FF00000000L) >>> 32),
            (byte)((value & 0x00000000FF000000L) >>> 24),
            (byte)((value & 0x0000000000FF0000L) >>> 16),
            (byte)((value & 0x000000000000FF00L) >>> 8),
            (byte)(value & 0x00000000000000FFL)
        };
    }

    ///..
    @Override
    public String toString() {

        return super.toString() + size + " " + Long.toHexString(value);
    }

    ///
}
