package io.github.clamentos.cachecruncher.monitoring.status;

///
import java.util.concurrent.atomic.AtomicInteger;

///
public final class TimeSamples {

    ///
    private final AtomicInteger position;
    private final AtomicInteger[] samples;

    ///
    public TimeSamples(int size) throws IllegalArgumentException {

        if(size < 1) {

            throw new IllegalArgumentException("The size must be greater than or equal to 1");
        }

        position = new AtomicInteger();
        samples = new AtomicInteger[size];

        for(int i = 0; i < size; i++) {

            samples[i] = new AtomicInteger();
        }
    }

    ///
    public void put(int sample) {

        samples[position.getAndUpdate(val -> (val + 1) % samples.length)].set(sample);
    }

    ///..
    public int[] getAll() {

        int[] data = new int[samples.length];

        for(int i = 0; i < data.length; i++) {

            data[i] = samples[i].get();
        }

        return data;
    }

    ///
}
