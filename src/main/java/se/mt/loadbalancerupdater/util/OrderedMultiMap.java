package se.mt.loadbalancerupdater.util;

import java.util.*;

public class OrderedMultiMap<K, V> {

    private Map<K, Collection<V>> map;

    public OrderedMultiMap(int initialCapacity) {
        map = new LinkedHashMap<K, Collection<V>>(initialCapacity);
    }

    public OrderedMultiMap() {
        // LinkedHashMap conserves insertion order
        map = new LinkedHashMap<K, Collection<V>>();
    }

    public Collection<V> get(K key) {
        return map.get(key);
    }

    public void put(K key, V value) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            ArrayList<V> list = new ArrayList<V>();
            map.put(key, list);
            list.add(value);
        }
    }

    public Set<K> keySet() {
        return map.keySet();
    }

    public Set<V> allValues() {
        Set<V> allValues = new HashSet<V>();
        Collection<Collection<V>> values = map.values();
        for (Collection<V> value : values) {
            allValues.addAll(value);
        }
        return allValues;
    }

    public int getNumKeys() {
        return map.size();
    }

    public Map<K, Collection<V>> getMap() {
        return Collections.unmodifiableMap(map);
    }

    public void clear() {
        map.clear();
    }
}