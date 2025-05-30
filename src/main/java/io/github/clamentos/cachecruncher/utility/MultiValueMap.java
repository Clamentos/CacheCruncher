package io.github.clamentos.cachecruncher.utility;

///
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

///
public class MultiValueMap<K, V> extends HashMap<K, List<V>> {

    ///
    public MultiValueMap() {

        super();
    }

    ///..
    public MultiValueMap(final int initialCapacity) {

        super(initialCapacity);
    }

    ///
    public void add(final K key, final V value) {

        super.computeIfAbsent(key, _ -> new ArrayList<>()).add(value);
    }

    ///..
    public void addAll(final K key, final Collection<V> values) {

        super.computeIfAbsent(key, _ -> new ArrayList<>()).addAll(values);
    }

    ///..
    public void addAll(final K key, final int index, final Collection<V> values) {

        super.computeIfAbsent(key, _ -> new ArrayList<>()).addAll(index, values);
    }

    ///..
    public void set(final K key, final int index, final V value) {

        final List<V> values = super.get(key);

        if(values != null) values.set(index, value);
        else throw new NoSuchElementException("Cannot set. The entry list is null");
    }

    ///..
    public V get(final K key, final int index) {

        final List<V> values = super.get(key);

        if(values != null) return values.get(index);
        return null;
    }

    ///..
    public List<V> getAll(final K key) {

        return super.get(key);
    }

    ///..
    public V remove(final K key, final int index) {

        final List<V> values = super.get(key);

        if(values != null) return values.remove(index);
        else throw new NoSuchElementException("Cannot remove. The entry list is null");
    }

    ///..
    public List<V> removeAll(final K key) {

        return super.remove(key);
    }

    ///
}
