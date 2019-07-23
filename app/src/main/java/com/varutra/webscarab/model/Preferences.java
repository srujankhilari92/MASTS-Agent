package com.varutra.webscarab.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Preferences {
    
    private static Context mContext;
    
    private Preferences() {
    }
    
    public static void init(Context context){
        mContext = context;
    }

    public static void setPreference(String key, String value) {
       SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
       Editor prefEditor = pref.edit();
       prefEditor.putString(key, value);
       prefEditor.commit();
    }
    
    public static String getPreference(String key) {
        return getPreference(key, null);
    }
    
    public static String getPreference(String key, String defaultValue) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        String value = pref.getString(key, defaultValue);
        return value;
    }
    
    public static boolean getPreferenceBoolean(String key, boolean defaultValue) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean value = pref.getBoolean(key, defaultValue);
        return value;
    }
    
    

    public static void remove(String key) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContext);
        Editor prefEditor = pref.edit();
        prefEditor.remove(key);
        prefEditor.commit();
        
    }
}