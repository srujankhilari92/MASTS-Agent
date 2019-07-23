package com.varutra.webscarab.plugin.proxy;



import com.varutra.webscarab.httpclient.HTTPClient;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.plugin.Framework;
import com.varutra.webscarab.plugin.proxy.BeanShellUI;
import com.varutra.webscarab.plugin.proxy.ProxyPlugin;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;

import java.util.logging.Logger;

import bsh.Interpreter;
import bsh.EvalError;
import bsh.TargetError;

public class BeanShell extends ProxyPlugin {
    
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    private String _scriptFile = "";
    private String _beanScript;
    private String _defaultScript =
    "/* Please read the JavaDoc and/or the source to understand what methods are available */\n" +
    "\n" +
    "import com.varutra.webscarab.model.Request;\n" +
    "import com.varutra.webscarab.model.Response;\n" +
    "import org.Varutrap.webscarab.httpclient.HTTPClient;\n" +
    "import java.io.IOException;\n" +
    "\n" +
    "public Response fetchResponse(HTTPClient nextPlugin, Request request) throws IOException {\n" +
    "   response = nextPlugin.fetchResponse(request);\n" +
    "   return response;\n" +
    "}\n";
    
    private Interpreter _interpreter;
    private Framework _framework = null;
    
    private boolean _enabled = false;
    
    private BeanShellUI _ui = null;
    
    public BeanShell(Framework framework) {
        _interpreter = new Interpreter();
        _framework = framework;
        try {
            _interpreter.set("framework", _framework);
        } catch (EvalError ee) {
            _logger.severe("Couldn't set framework: " + ee);
        }
        parseProperties();
    }
    
    public void setUI(BeanShellUI ui) {
        _ui = ui;
        PrintStream ps = _ui.getOut();
        if (ps != null) _interpreter.setOut(ps); 
        ps = _ui.getErr();
        if (ps != null) _interpreter.setErr(ps);
    }
    
    private void parseProperties() {
        String prop = "BeanShell.scriptFile";
        String value = Preferences.getPreference(prop, "");
        _scriptFile = value;
        if (!_scriptFile.equals("")) {
            loadScriptFile(_scriptFile);
        } else {
            try {
                setScript(_defaultScript);
            } catch (EvalError ee) {
                _logger.severe("Invalid default script string " + ee);
            }
        }
        
        prop = "BeanShell.enabled";
        value = Preferences.getPreference(prop, "");
        setEnabled(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    }
    
    public String getPluginName() {
        return new String("Bean Shell");
    }
    
    public void setEnabled(boolean bool) {
        _enabled = bool;
        String prop = "BeanShell.enabled";
        Preferences.setPreference(prop,Boolean.toString(bool));
    }
    
    public boolean getEnabled() {
        return _enabled;
    }
    
    private void loadScriptFile(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            setScript(sb.toString());
        } catch (Exception e) {
            _logger.severe("Error reading BeanShell script from '" + filename + "' : " + e);
            try {
                setScript(_defaultScript);
            } catch (EvalError ee) {
                _logger.severe("Invalid default script string: " + ee);
            }
        }
    }
    
    public void setScriptFile(String filename) throws EvalError {
        _scriptFile = filename;
        String prop = "BeanShell.scriptFile";
        Preferences.setPreference(prop,filename);
        if (!filename.equals("")) {
            loadScriptFile(filename);
        } else {
            setScript(_defaultScript);
        }
    }
    
    public String getScriptFile() {
        return _scriptFile;
    }
    
    public void setScript(String script) throws EvalError {
        _beanScript = script;
        _interpreter = new Interpreter();
        _interpreter.eval(_beanScript);
    }
    
    public String getScript() {
        return _beanScript;
    }
    
    public HTTPClient getProxyPlugin(HTTPClient in) {
        return new Plugin(in);
    }
    
    private class Plugin implements HTTPClient {
        
        private HTTPClient _in;
        
        public Plugin(HTTPClient in) {
            _in = in;
        }
        
        public Response fetchResponse(Request request) throws IOException {
            if (_enabled) {
                try {
                    synchronized(_interpreter) {
                        _interpreter.unset("response");
                        _interpreter.set("nextClient", _in);
                        _interpreter.set("request", request);
                        try {
                            _interpreter.eval("Response response = fetchResponse(nextClient, request);");
                        } catch (TargetError te) {
                            if (te.getTarget() instanceof IOException) {
                                throw (IOException) te.getTarget();
                            }
                            throw te;
                       }
                        Response response = (Response) _interpreter.get("response");
                        _interpreter.unset("model");
                        _interpreter.unset("response");
                        _interpreter.unset("nextClient");
                        _interpreter.unset("request");
                        response.setHeader("X-BeanShell", "possibly modified");
                        return response;
                    }
                } catch (EvalError e) {
                    System.err.println("e is a " + e.getClass());
                    if (_ui != null) _ui.getErr().println(e.toString());
                    throw new IOException("Error evaluating bean script : " + e);
                }
            } else {
                return _in.fetchResponse(request);
            }
        }
    }
    
}
