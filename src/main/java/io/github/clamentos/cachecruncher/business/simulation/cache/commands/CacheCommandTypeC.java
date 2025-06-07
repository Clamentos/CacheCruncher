package io.github.clamentos.cachecruncher.business.simulation.cache.commands;

///
import lombok.Getter;

///
@Getter

///
public final class CacheCommandTypeC extends CacheCommand {

    ///
    private final int repeats;
    private final String sectionName;

    ///
    public CacheCommandTypeC(final CacheCommandType commandType, final int repeats, final String sectionName) {

        super(commandType);

        this.repeats = repeats;
        this.sectionName = sectionName;
    }

    ///
    @Override
    public byte[] toByteArray() {

        final int stringLength = sectionName.length();
        final byte[] stringData = sectionName.getBytes();
        final byte[] data = new byte[1 + 4 + 4 + stringLength];

        data[0] = (byte)super.getType().ordinal();
        data[1] = (byte)((repeats & 0xFF000000) >>> 24);
        data[2] = (byte)((repeats & 0x00FF0000) >>> 16);
        data[3] = (byte)((repeats & 0x0000FF00) >>> 8);
        data[4] = (byte)(repeats & 0x000000FF);
        data[5] = (byte)((stringLength & 0xFF000000) >>> 24);
        data[6] = (byte)((stringLength & 0x00FF0000) >>> 16);
        data[7] = (byte)((stringLength & 0x0000FF00) >>> 8);
        data[8] = (byte)(stringLength & 0x000000FF);

        for(int i = 0; i < stringLength; i++) {

            data[i + 9] = stringData[i];
        }

        return data;
    }

    ///..
    @Override
    public String toString() {

        return super.toString() + repeats + "#" + sectionName;
    }

    ///
}
