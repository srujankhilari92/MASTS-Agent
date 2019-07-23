package com.varutra.webscarab.util;

import java.util.Comparator;

public class NullComparator implements Comparator<Object> {
    
    public NullComparator() {
    }
    
    @SuppressWarnings("unchecked")
	public int compare(Object o1, Object o2) {
        if (o1 == null && o2 == null) return 0;
        if (o1 == null && o2 != null) return 1;
        if (o1 != null && o2 == null) return -1;
        if (o1 instanceof Comparable) return ((Comparable<Object>)o1).compareTo(o2);
        throw new ClassCastException("Incomparable objects " + o1.getClass().getName() + " and " + o2.getClass().getName());
    }
    
}