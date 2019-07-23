package com.varutra.webscarab.plugin.fragments;

import com.varutra.webscarab.model.StoreException;

public interface FragmentsStore {
    
    int getFragmentTypeCount();
    
    String getFragmentType(int index);
    
    int getFragmentCount(String type);
    
    String getFragmentKeyAt(String type, int position);
    
    int indexOfFragment(String type, String key);
    
    int putFragment(String type, String key, String fragment);
    
    String getFragment(String key);
    
    void flush() throws StoreException;
    
}
