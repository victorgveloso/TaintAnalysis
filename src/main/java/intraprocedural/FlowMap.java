package intraprocedural;

import soot.toolkits.scalar.FlowSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FlowMap<K, V> implements Map<K, FlowSet<V>> {
    private LinkedHashMap<K, FlowSet<V>> map;

    public FlowMap() {
        this.map = new LinkedHashMap<>();
    }

    public FlowMap(LinkedHashMap<K, FlowSet<V>> map) {
        this.map = new LinkedHashMap<>(map);
    }

    public FlowMap(FlowMap<K, V> flowMap) {
        this.map = flowMap.map;
    }

    @Override
    public int size() {
        return map.size();
    }
    public boolean isSubSet(K key, FlowSet<V> other) {
        if (!map.containsKey(key)) {
            return false;
        }

        FlowSet<V> match = map.get(key);

        if (other == match) {
            return true;
        } else {
            Iterator<V> iter = other.iterator();

            V v;
            do {
                if (!iter.hasNext()) {
                    return true;
                }

                v = iter.next();
            } while(match.contains(v));

            return false;
        }
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public K getContainingSet(V value) {
        for (Entry<K, FlowSet<V>> entry : map.entrySet()) {
            if (entry.getValue().contains(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public FlowSet<V> get(Object key) {
        return map.get(key);
    }

    @Override
    public FlowSet<V> put(K key, FlowSet<V> value) {
        return map.put(key, value);
    }

    public void add(K key, V value) {
        FlowSet<V> flowSet = map.get(key);
        flowSet.add(value);
    }

    public void add(V value) {
        if (map.keySet().size() == 1) {
            add(map.keySet().iterator().next(), value);
        }
    }

    @Override
    public FlowSet<V> remove(Object key) {
        return map.remove(key);
    }

    public void removeKey(K key) {
        remove(key);
    }

    public void removeValue(K key, V value) {
        if (!map.containsKey(key)) {
            return;
        }
        if (map.get(key).contains(value)) {
            map.get(key).remove(value);
        }
        if (map.get(key).isEmpty()) {
            removeKey(key);
        }
    }

    public void remove(K key, FlowMap<K, V> dest) {
        if (dest != this) {
            this.copy(dest);
        }
        dest.remove(key);
    }

    public void putAll(FlowMap<K, V> m) {
        map.putAll(m.map);
    }

    @Override
    public void putAll(Map<? extends K, ? extends FlowSet<V>> sourceMap) {
        map.putAll(sourceMap);
    }

    @Override
    public FlowMap<K, V> clone() {
        return new FlowMap<>((LinkedHashMap<K, FlowSet<V>>) map.clone());
    }

    public FlowMap<K, V> emptyMap() {
        FlowMap<K, V> flowMap = clone();
        this.clear();
        return flowMap;
    }

    public void copy(FlowMap<K, V> dest) {
        if (this != dest) {
            dest.clear();
            dest.putAll(this);
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    public void union(FlowMap<K, V> other) {
        forEach((k, v) -> {
            if (other.containsKey(k)) {
                v.union(other.get(k));
            }
        });
    }

    public void union(FlowMap<K, V> other, FlowMap<K, V> dest) {
        if (dest != this && dest != other) {
            copy(dest);
        }
        dest.union(other);
    }

    public void intersection(FlowMap<K, V> other) {
        forEach((k, v) -> {
            if (other.containsKey(k)) {
                v.intersection(other.get(k));
            }
        });
    }

    public void intersection(FlowMap<K, V> other, FlowMap<K, V> dest) {
        if (dest != this && dest != other) {
            copy(dest);
        }
        dest.intersection(other);
    }

    public void difference(FlowMap<K, V> other) {
        forEach((k, v) -> {
            if (other.containsKey(k)) {
                v.difference(other.get(k));
            }
        });
    }

    public void difference(FlowMap<K, V> other, FlowMap<K, V> dest) {
        if (dest != this && dest != other) {
            copy(dest);
        }
        dest.difference(other);
    }

    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<FlowSet<V>> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, FlowSet<V>>> entrySet() {
        return map.entrySet();
    }
}
