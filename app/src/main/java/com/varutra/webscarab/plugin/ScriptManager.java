package com.varutra.webscarab.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.BSFException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;


import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.util.EventListenerList;

public class ScriptManager {
    
    private BSFManager _bsfManager;
    private TreeMap<String, Hook[]> _hooks = new TreeMap<String, Hook[]>();
    private EventListenerList _listeners = new EventListenerList();
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public ScriptManager(Framework framework) {
        try {
            _bsfManager = new BSFManager();
            _bsfManager.declareBean("framework", framework, framework.getClass());
            _bsfManager.declareBean("out", System.out, System.out.getClass());
            _bsfManager.declareBean("err", System.err, System.out.getClass());
        } catch (BSFException bsfe) {
            _logger.severe("Declaring a bean should not throw an exception! " + bsfe);
        }
    }
    
    public void addScriptListener(ScriptListener listener) {
        synchronized(_listeners) {
            _listeners.add(ScriptListener.class, listener);
        }
    }
    
    public void removeScriptListener(ScriptListener listener) {
        synchronized(_listeners) {
            _listeners.remove(ScriptListener.class, listener);
        }
    }
    
    public void registerHooks(String pluginName, Hook[] hooks) {
        if (hooks != null && hooks.length > 0) {
            _hooks.put(pluginName, hooks);
            for (int i=0; i<hooks.length; i++) {
                hooks[i].setBSFManager(_bsfManager);
            }
            fireHooksChanged();
        }
    }
    
    public int getPluginCount() {
        return _hooks.size();
    }
    
    public String getPlugin(int i) {
        String[] plugins = _hooks.keySet().toArray(new String[0]);
        return plugins[i];
    }
    
    public int getHookCount(String plugin) {
        Hook[] hooks = _hooks.get(plugin);
        if (hooks == null) return 0;
        return hooks.length;
    }
    
    public Hook getHook(String plugin, int i) {
        Hook[] hooks = _hooks.get(plugin);
        if (hooks == null) return null;
        return hooks[i];
    }
    
    public void addScript(String plugin, Hook hook, Script script, int position) throws BSFException {
        String language = BSFManager.getLangFromFilename(script.getFile().getName());
        if (language != null) {
            script.setLanguage(language);
            script.setEnabled(true);
            hook.addScript(script, position);
            fireScriptAdded(plugin, hook, script);
        }
    }
    
    public void addScript(String plugin, Hook hook, Script script) throws BSFException {
        addScript(plugin, hook, script, hook.getScriptCount());
    }
    
    public void setEnabled(String plugin, Hook hook, Script script, boolean enabled) {
        script.setEnabled(enabled);
        fireScriptChanged(plugin, hook, script);
    }
    
    public void removeScript(String plugin, Hook hook, Script script) {
        int count = hook.getScriptCount();
        for (int i=0; i<count; i++) {
            Script s = hook.getScript(i);
            if (s == script) {
                hook.removeScript(i);
                fireScriptRemoved(plugin, hook, script);
                return;
            }
        }
    }
    
    public void loadScripts() {
        Iterator<Map.Entry<String, Hook[]>> hookIt = _hooks.entrySet().iterator();
        while (hookIt.hasNext()) {
            Map.Entry<String, Hook[]> entry = hookIt.next();
            String plugin =  entry.getKey();
            Hook[] hooks = entry.getValue();
            if (hooks != null) {
                for (int i=0; i<hooks.length; i++) {
                    for (int j=0; j<hooks[i].getScriptCount(); j++)
                        hooks[i].removeScript(j);
                    int j=0;
                    String scriptName = Preferences.getPreference(hooks[i].getName()+"."+j+".name");
                    while (scriptName != null) {
                        File f = new File(scriptName);
                        if (f.canRead()) {
                            try {
                                Script script = new Script(f);
                                String enabled = Preferences.getPreference(hooks[i].getName()+"."+j+".enabled", "false");
                                addScript(plugin, hooks[i], script);
                                setEnabled(plugin, hooks[i], script, Boolean.valueOf(enabled).booleanValue());
                            } catch (IOException ioe) {
                                _logger.warning("Error loading script '" + scriptName + "' : " + ioe.getLocalizedMessage());
                            } catch (BSFException bsfe) {
                                _logger.warning("Error loading script '" + scriptName + "' : " + bsfe.getLocalizedMessage());
                            }
                        }
                        j++;
                        scriptName = Preferences.getPreference(hooks[i].getName()+"."+j+".name");
                    }
                }
            }
        }
    }
    
    public void saveScripts() {
        Iterator<Map.Entry<String, Hook[]>> hookIt = _hooks.entrySet().iterator();
        while (hookIt.hasNext()) {
            Map.Entry<String, Hook[]> entry = hookIt.next();
            Hook[] hooks = entry.getValue();
            if (hooks != null) {
                for (int i=0; i<hooks.length; i++) {
                    for (int j=0; j<hooks[i].getScriptCount(); j++) {
                        Script script = hooks[i].getScript(j);
                        Preferences.setPreference(hooks[i].getName()+"."+j+".name", script.getFile().getAbsolutePath());
                        Preferences.setPreference(hooks[i].getName()+"."+j+".enabled", Boolean.toString(script.isEnabled()));
                    }
                    Preferences.remove(hooks[i].getName()+"."+hooks[i].getScriptCount()+".name");
                    Preferences.remove(hooks[i].getName()+"."+hooks[i].getScriptCount()+".enabled");
                }
            }
        }
    }
    
    protected void fireHooksChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = _listeners.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).hooksChanged();
            }
        }
    }
    
    protected void fireScriptAdded(String plugin, Hook hook, Script script) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).scriptAdded(plugin, hook, script);
            }
        }
    }
    protected void fireScriptRemoved(String plugin, Hook hook, Script script) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).scriptRemoved(plugin, hook, script);
            }
        }
    }
    
    protected void fireScriptStarted(String plugin, Hook hook, Script script) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).scriptStarted(plugin, hook, script);
            }
        }
    }
    protected void fireScriptEnded(String plugin, Hook hook, Script script) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).scriptEnded(plugin, hook, script);
            }
        }
    }
    protected void fireScriptChanged(String plugin, Hook hook, Script script) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).scriptChanged(plugin, hook, script);
            }
        }
    }
    protected void fireScriptError(String plugin, Hook hook, Script script, Throwable error) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).scriptError(plugin, hook, script, error);
            }
        }
    }
    protected void fireHookEnded(String plugin, Hook hook) {
        Object[] listeners = _listeners.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ScriptListener.class) {
                ((ScriptListener)listeners[i+1]).hookEnded(plugin, hook);
            }
        }
    }
    
}
