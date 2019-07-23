package com.varutra.webscarab.plugin;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.apache.bsf.BSFManager;
import org.apache.bsf.BSFException;

public class Hook {
    
    private String _name;
    private String _description;
    private List<Script> _scripts = new ArrayList<Script>();
    protected BSFManager _bsfManager = null;
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public Hook(String name, String description) {
        _name = name;
        _description = description;
    }
    
    public void setBSFManager(BSFManager bsfManager) {
        _bsfManager = bsfManager;
    }
    
    protected void runScripts() {
        if (_bsfManager == null) return;
        synchronized(_bsfManager) {
            for (int i=0; i<_scripts.size(); i++) {
                Script script = _scripts.get(i);
                if (script.isEnabled()) {
                    try {
                        _bsfManager.exec(script.getLanguage(), _name, 0, 0, script.getScript());
                    } catch (BSFException bsfe) {
                        _logger.warning("Script exception: " + bsfe);
                    }
                }
            }
        }
    }
    
    public String getName() {
        return _name;
    }
    
    public String getDescription() {
        return _description;
    }
    
    public int getScriptCount() {
        return _scripts.size();
    }
    
    public Script getScript(int i) {
        return _scripts.get(i);
    }
    
    public void addScript(Script script) {
        _scripts.add(script);
    }
    
    public void addScript(Script script, int position) {
        _scripts.add(position, script);
    }
    
    public Script removeScript(int position) {
        return _scripts.remove(position);
    }
    
}
