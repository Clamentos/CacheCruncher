package io.github.clamentos.cachecruncher.mappers.serializers;

///
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;

///
public final class CommandTracker {

    ///
    private byte[] rawData;
    private int position;

    ///
    public void initialize(final CacheCommand command) {

        rawData = command.toByteArray();
        position = 0;
    }

    ///..
    public int read() {

        if(rawData == null || position == rawData.length) return -1;
        return rawData[position++] & 0x000000FF;
    }

    ///
}
