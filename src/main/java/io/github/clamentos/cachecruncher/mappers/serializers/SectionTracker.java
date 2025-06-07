package io.github.clamentos.cachecruncher.mappers.serializers;

///
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;

///..
import io.github.clamentos.cachecruncher.utility.BinaryUtils;
import io.github.clamentos.cachecruncher.utility.MultiValueMap;

///.
import java.util.Iterator;
import java.util.List;
import java.util.Map;

///
public final class SectionTracker {

    ///
    private final MultiValueMap<String, CacheCommand> sections;

    ///..
    private final SectionEntryTracker sectionEntryTracker;
    private final Iterator<Map.Entry<String, List<CacheCommand>>> position;

    ///..
    private int globalSizeTracker;

    ///..
    private byte[] entryKey;
    private int entryKeySizeTracker;
    private int entryKeyTracker;

    ///..
    private Map.Entry<String, List<CacheCommand>> currentEntry;

    ///
    public SectionTracker(final MultiValueMap<String, CacheCommand> sections) {

        this.sections = sections;

        sectionEntryTracker = new SectionEntryTracker();
        position = sections.entrySet().iterator();
    }

    ///
    public int read() {

        if(globalSizeTracker != 4) {

            final int size = (sections.size() & BinaryUtils.intMask[globalSizeTracker]) >>> BinaryUtils.intMaskShift[globalSizeTracker];
            globalSizeTracker++;

            return size;
        }

        // Prime for the first time.
        if(currentEntry == null && !this.gotoNextEntry()) return -1;

        final int value = this.readEntry();

        if(value == -1 && !this.gotoNextEntry()) return -1;
        return value;
    }

    ///..
    private boolean gotoNextEntry() {

        boolean value = false;

        if(position.hasNext()) {

            currentEntry = position.next();
            entryKey = currentEntry.getKey().getBytes();
            sectionEntryTracker.initialize(currentEntry.getValue());

            value = true;
        }

        return value;
    }

    ///..
    private int readEntry() {

        final int value = this.readEntryKey();

        if(value == -1) return sectionEntryTracker.read();
        return value;
    }

    ///..
    private int readEntryKey() {

        if(entryKeySizeTracker != 4) return entryKey.length & (BinaryUtils.intMask[entryKeySizeTracker++]);
        if(entryKeyTracker != entryKey.length) return entryKey[entryKeyTracker++];

        return -1;
    }

    ///
}
