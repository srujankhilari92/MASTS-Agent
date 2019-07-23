package com.varutra.webscarab.util;


import java.util.List;
import java.util.Iterator;

public class LevenshteinDistance<T> {
    
    private List<T> _baseline;
    private int[] _current, _previous;
    
    public LevenshteinDistance(List<T> baseline) {
        _baseline = baseline;
        _current = new int[_baseline.size()+1];
        _previous = new int[_baseline.size()+1];
    }
    
    public synchronized int getDistance(List<T> target) {
        if (_baseline.size() == 0)
            return target.size();
        if (target.size() == 0)
            return _baseline.size();
        
        for (int i = 0; i < _current.length; i++) {
            _current[i] = i;
        }
        
        Iterator<T> targIt = target.iterator();
        int j=0;
        while(targIt.hasNext()) {
            T targObj = targIt.next();
            j++;
            
            int[] t = _previous;
            _previous = _current;
            _current = t;
            
            _current[0] = _previous[0]+1;
            
            Iterator<T> baseIt = _baseline.iterator();
            int i=0;
            while(baseIt.hasNext()) {
                T baseObj = baseIt.next();
                i++;
                
                int cost;
                if (baseObj.equals(targObj)) {
                  cost = 0;
                } else {
                  cost = 1;
                }
                _current[i] = Math.min(Math.min(_previous[i]+1, _current[i-1]+1), _previous[i-1] + cost);
            }
        }
        return _current[_baseline.size()];
    }


}