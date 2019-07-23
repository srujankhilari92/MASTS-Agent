package com.varutra.webscarab.util;

import java.util.Map;
import java.util.LinkedHashMap;

public class MRUCache<K, V> extends LinkedHashMap<K, V> {
    
	private static final long serialVersionUID = 147426251266610197L;
	private int _maxSize;
    
    public MRUCache(int maxSize) {
        this(16, maxSize, 0.75f);
    }
    
    public MRUCache(int initialCapacity, int maxSize) {
        this(initialCapacity, maxSize, 0.75f);
    }
    
    public MRUCache(int initialCapacity, int maxSize, float loadFactor) {
        super(initialCapacity, loadFactor, true);
        _maxSize = maxSize;
    }
    
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > _maxSize;
    }
    
}