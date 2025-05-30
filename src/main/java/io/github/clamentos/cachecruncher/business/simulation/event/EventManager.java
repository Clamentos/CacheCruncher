package io.github.clamentos.cachecruncher.business.simulation.event;

///
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

///..
import java.util.function.Supplier;

///.
import lombok.Getter;

///
public final class EventManager {

    ///
    private final TreeMap<Long, Queue<Supplier<Long>>> events;

    ///..
    @Getter
    private long currentCycle;

    ///
    public EventManager() {

        events = new TreeMap<>();
        currentCycle = 0;
    }

    ///
    public void addEvent(final long atCycle, final Supplier<Long> event) {

        events.computeIfAbsent(atCycle, _ -> new LinkedList<>()).add(event);
    }

    ///..
    public long advance(final long cycles) {

        long cycleCounter = 0;
        long remainingCycles = cycles;
        final long stopAt = currentCycle + cycles;

        while(cycleCounter < cycles) {

            Map.Entry<Long, Queue<Supplier<Long>>> entry = events.firstEntry();

            if(entry != null) {

                long entryAtCycle = entry.getKey();
                Queue<Supplier<Long>> entryEvents = entry.getValue();

                if(entryAtCycle <= stopAt) {

                    long increment = entryAtCycle - currentCycle - 1;
                    cycleCounter += this.increment(increment);
                    remainingCycles -= increment;

                    while(!entryEvents.isEmpty()) {

                        increment = entryEvents.poll().get();
                        cycleCounter += this.increment(increment);
                        remainingCycles -= increment;
                    }

                    events.remove(entryAtCycle);
                }

                else {

                    cycleCounter += this.increment(remainingCycles);
                }
            }

            else {

                cycleCounter += this.increment(remainingCycles);
            }
        }

        return cycleCounter;
    }

    ///.
    private long increment(final long amount) {

        currentCycle += amount;
        return amount;
    }

    ///
}
