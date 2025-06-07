package io.github.clamentos.cachecruncher.mappers.serializers;

///
import io.github.clamentos.cachecruncher.business.simulation.cache.commands.CacheCommand;

///..
import io.github.clamentos.cachecruncher.utility.BinaryUtils;

///.
import java.util.List;

///
public final class SectionEntryTracker {

    ///
    private final CommandTracker commandTracker;

    ///..
    private int sizeTracker;
    private List<CacheCommand> commands;
    private int position;

    ///
    public SectionEntryTracker() {

        commandTracker = new CommandTracker();
    }

    ///..
    public SectionEntryTracker(final List<CacheCommand> commands) {

        commandTracker = new CommandTracker();
        this.initialize(commands);
    }

    ///
    public void initialize(final List<CacheCommand> commands) {

        if(!commands.isEmpty()) {

            commandTracker.initialize(commands.get(0));
            position = 1;
        }

        this.commands = commands;
    }

    ///..
    public int read() {

        if(commands == null) return -1;

        if(sizeTracker != 4) {

            final int size = (commands.size() & BinaryUtils.intMask[sizeTracker]) >>> BinaryUtils.intMaskShift[sizeTracker];
            sizeTracker++;

            return size;
        }

        final int value = commandTracker.read();

        if(value == -1 && position < commands.size()) {

            commandTracker.initialize(commands.get(position++));
            return commandTracker.read();
        }

        return value;
    }

    ///
}
