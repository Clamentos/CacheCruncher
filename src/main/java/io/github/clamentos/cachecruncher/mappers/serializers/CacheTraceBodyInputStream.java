package io.github.clamentos.cachecruncher.mappers.serializers;

///
import io.github.clamentos.cachecruncher.persistence.entities.CacheTraceBody;

///.
import java.io.IOException;
import java.io.InputStream;

///
public final class CacheTraceBodyInputStream extends InputStream {

    ///
    private final SectionTracker sectionTracker;
    private final SectionEntryTracker traceTracker;

    ///..
    private boolean doSections;

    ///
    public CacheTraceBodyInputStream(final CacheTraceBody cacheTraceBody) {

        sectionTracker = new SectionTracker(cacheTraceBody.getSections());
        traceTracker = new SectionEntryTracker(cacheTraceBody.getBody());

        doSections = true;
    }

    ///
    @Override
    public int read() throws IOException {

        if(doSections) {

            final int value = sectionTracker.read();

            if(value != -1) return value;
            else doSections = false;
        }

        return traceTracker.read();
    }

    ///..
    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {

        if(length == 0) return 0;
        int counter = 0;

        for(int i = 0; i < length; i++) {

            final int value = this.read();

            if(i == 0 && value == -1) return -1;
            if(value == -1) return counter;

            buffer[offset + i] = (byte)value;
            counter++;
        }

        return counter;
    }

    ///
}
