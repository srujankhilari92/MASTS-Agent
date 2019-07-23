package com.varutra.webscarab.plugin.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.lang.NumberFormatException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;





import android.app.Service;
import android.content.SharedPreferences;
import android.util.Log;

import com.varutra.constants.Constants;
import com.varutra.masts.proxy.MyService;
import com.varutra.plugin.gui.*;
import com.varutra.masts.proxy.R;
import com.varutra.utils.preference.CheckOptionApp;
import com.varutra.utils.preference.CheckOptionAppList;
import com.varutra.webscarab.model.ConnectionDescriptor;
import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.FrameworkModel;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.MessageOutputStream;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.model.StoreException;
import com.varutra.webscarab.plugin.Framework;
import com.varutra.webscarab.plugin.Hook;
import com.varutra.webscarab.plugin.Plugin;
import com.varutra.webscarab.store.sql.SqlLiteStore;
import com.varutra.websockets.ExtensionWebSocket;
import com.varutraproxy.utils.PreferenceUtils;



import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Proxy implements Plugin {
	MyService Ms;	
	public SharedPreferences pref;
    private boolean _running = false;
    private ArrayList requests = new ArrayList();
    private Framework _framework = null;
    private ITransparentProxyResolver _transparentProxyResolver;
    private IClientResolver _clientResolver;
    private boolean _captureData = false;
    private boolean _useFakeCerts = false;
    private boolean _storeSslAsPcap = false;
    private Map<Integer, CheckOptionApp> _appOptions = null;
    
    private File storageDir = null;
    private File pcapStorageDir = null;

    private ProxyUI _ui = null;

    private ArrayList<ProxyPlugin> _plugins = new ArrayList<ProxyPlugin>();
    private TreeMap<ListenerSpec, Listener> _listeners = new TreeMap<ListenerSpec, Listener>();

    private Logger _logger = Logger.getLogger(getClass().getName());

    private String _status = "Stopped";
    private int _pending = 0;

    private static HashMap<String, SSLSocketFactory> _factoryMap = new HashMap<String, SSLSocketFactory>();

    private static char[] _keystorepass = "password".toCharArray();
    private static char[] _keypassword = "password".toCharArray();
    private SSLSocketFactoryFactory _certGenerator = null;
    private static String _certDir = "./certs/";

    private Proxy.ConnectionHook _allowConnection = new ConnectionHook(
            "Allow connection",
            "Called when a new connection is received from a browser\n"
                    + "use connection.getAddress() and connection.closeConnection() to decide and react");

    private Proxy.ConnectionHook _interceptRequest = new ConnectionHook(
            "Intercept request",
            "Called when a new request has been submitted by the browser\n"
                    + "use connection.getRequest() and connection.setRequest(request) to perform changes");

    private Proxy.ConnectionHook _interceptResponse = new ConnectionHook(
            "Intercept response",
            "Called when the request has been submitted to the server, and the response "
                    + "has been recieved.\n"
                    + "use connection.getResponse() and connection.setResponse(response) to perform changes");
    
    private static ExtensionWebSocket _webSocketManager;
    
    public Proxy(Framework framework, ITransparentProxyResolver transparentProxyResolver, IClientResolver clientResolver) {
        _logger.setLevel(Level.FINEST);
        _framework = framework;
        _transparentProxyResolver = transparentProxyResolver;
        _clientResolver = clientResolver;
        _captureData = Preferences.getPreferenceBoolean(PreferenceUtils.proxyCaptureData, false);
        _useFakeCerts = Preferences.getPreferenceBoolean(PreferenceUtils.proxyFakeCerts, false);
        _storeSslAsPcap = Preferences.getPreferenceBoolean(PreferenceUtils.proxyStoreSslAsPcap, false);
        _appOptions = CheckOptionAppList.CreateActiveObjectListFromPreferences(_framework.getAndroidContext());
        MessageOutputStream.resetActiveMemorySize();
        parseListenerConfig();
        _certGenerator = null;
        try {
            File dataStorageDir = PreferenceUtils.getDataStorageDir(_framework.getAndroidContext());
            
            if (dataStorageDir == null){
                String errorText = "ERROR \nNo external storage available...";
                _logger.fine(errorText);
                _logger.fine("Make external sdcard available to android");
                return;
            }
            _logger.fine("Using " + dataStorageDir.getAbsolutePath() + " for data storage");
            storageDir = dataStorageDir;
            if (storageDir.exists()){
                File pcapStorage = new File(dataStorageDir.getAbsoluteFile() + "/pcap");
                if (!pcapStorage.exists()){
                    pcapStorage.mkdir();
                }
                pcapStorageDir = pcapStorage;
                if (!_captureData && _storeSslAsPcap){
                    PcapWriter.init(pcapStorage.getAbsolutePath() + "/capture_" + System.currentTimeMillis() + ".pcap");
                }
            }
            String keystoreCAFullPath = PreferenceUtils.getCAFilePath(_framework.getAndroidContext());
            String keystoreCertFullPath = PreferenceUtils.getCertFilePath(_framework.getAndroidContext());
            String caPassword = PreferenceUtils.getCAFilePassword(_framework.getAndroidContext());
            String keyStoreType = "PKCS12";
            if (keystoreCAFullPath != null && keystoreCAFullPath.length() > 0 && 
                keystoreCertFullPath != null && keystoreCertFullPath.length() > 0){
                
                try{
                    _certGenerator = new SSLSocketFactoryFactory(keystoreCAFullPath, keystoreCertFullPath, keyStoreType, caPassword.toCharArray());
                    _certGenerator.setReuseKeys(true);
                    _logger.fine("Using CA from file: " + keystoreCAFullPath);
                }catch(Exception ex){
                    _logger.fine("Error getting custom CA certificate:" + ex.getMessage());
                }
            }else{
                _logger.fine("Error getting custom CA certificate: Invalid file path");
            }
            
            _webSocketManager = new ExtensionWebSocket(SqlLiteStore.getInstance(_framework.getAndroidContext(), dataStorageDir.getAbsolutePath()));
        } catch (NoClassDefFoundError e) {
            _certGenerator = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public Map<Integer, CheckOptionApp> getAppOptions(){
        return _appOptions;
    }
    
    public File getStorageDir(){
        return storageDir;
    }
    
    public File getPcapStorageDir(){
        return pcapStorageDir;
    }

    public Hook[] getScriptingHooks() {
        return new Hook[] { _allowConnection, _interceptRequest,
                _interceptResponse };
    }
    
    public ExtensionWebSocket getWebSocketManager(){
        return _webSocketManager;
    }

    public Object getScriptableObject() {
        return null;
    }
    
    public ITransparentProxyResolver getTransparentProxyResolver(){
        return _transparentProxyResolver;
    }
    
    public IClientResolver getClientResolver(){
        return _clientResolver;
    }

    void allowClientConnection(ScriptableConnection connection) {
        _allowConnection.runScripts(connection);
    }

    void interceptRequest(ScriptableConnection connection) {
        _interceptRequest.runScripts(connection);
    }

    void interceptResponse(ScriptableConnection connection) {
        _interceptResponse.runScripts(connection);
    }

    public void setUI(ProxyUI ui) {
        _ui = ui;
        if (_ui != null)
            _ui.setEnabled(_running);
    }

    public void addPlugin(ProxyPlugin plugin) {
        _plugins.add(plugin);
    }

    public ProxyPlugin getPlugin(String name) {
        ProxyPlugin plugin = null;
        Iterator<ProxyPlugin> it = _plugins.iterator();
        while (it.hasNext()) {
            plugin = it.next();
            if (plugin.getPluginName().equals(name))
                return plugin;
        }
        return null;
    }

    public String getPluginName() {
        return new String("Proxy");
    }

    public ListenerSpec[] getProxies() {
        if (_listeners.size() == 0) {
            return new ListenerSpec[0];
        }
        return (ListenerSpec[]) _listeners.keySet()
                .toArray(new ListenerSpec[0]);
    }

    protected ProxyPlugin[] getPlugins() {
        ProxyPlugin[] plugins = new ProxyPlugin[_plugins.size()];
        for (int i = 0; i < _plugins.size(); i++) {
            plugins[i] = _plugins.get(i);
        }
        return plugins;
    }


    public void addListener(ListenerSpec spec) {
        createListener(spec);
        startListener(_listeners.get(spec));

        String key = getKey(spec);
        Preferences.setPreference("Proxy.listener." + key + ".base", spec
                .getBase() == null ? "" : spec.getBase().toString());
        Preferences.setPreference("Proxy.listener." + key + ".primary", spec
                .isPrimaryProxy() == true ? "yes" : "no");

        String value = null;
        Iterator<ListenerSpec> i = _listeners.keySet().iterator();
        while (i.hasNext()) {
            key = getKey(i.next());
            if (value == null) {
                value = key;
            } else {
                value = value + ", " + key;
            }
        }
        Preferences.setPreference("Proxy.listeners", value);
    }

    private String getKey(ListenerSpec spec) {
        return spec.getAddress() + ":" + spec.getPort();
    }

    private void startListener(Listener l) {
        Thread t = new Thread(l, "Listener-" + getKey(l.getListenerSpec()));
        t.setDaemon(true);
        t.start();
        if (_ui != null)
            _ui.proxyStarted(l.getListenerSpec());
    }

    private boolean stopListener(Listener l) {
        boolean stopped = l.stop();
        if (stopped && _ui != null)
            _ui.proxyStopped(l.getListenerSpec());
        return stopped;
    }

    public boolean removeListener(ListenerSpec spec) {
        Listener l = _listeners.get(spec);
        if (l == null)
            return false;
        if (stopListener(l)) {
            _listeners.remove(spec);
            if (_ui != null)
                _ui.proxyRemoved(spec);
            String key = getKey(spec);
            Preferences.remove("Proxy.listener." + key + ".base");
            Preferences.remove("Proxy.listener." + key + ".simulator");
            Preferences.remove("Proxy.listener." + key + ".primary");
            String value = null;
            Iterator<ListenerSpec> i = _listeners.keySet().iterator();
            while (i.hasNext()) {
                key = getKey(i.next());
                if (value == null) {
                    value = key;
                } else {
                    value = value + ", " + key;
                }
            }
            if (value == null) {
                value = "";
            }
            Preferences.setPreference("Proxy.listeners", value);
            return true;
        } else {
            return false;
        }
    }
    public void run() {
        Iterator<ListenerSpec> it = _listeners.keySet().iterator();
        while (it.hasNext()) {
            ListenerSpec spec = it.next();
            try {
                spec.verifyAvailable();
                Listener l = _listeners.get(spec);
                if (l == null) {
                    createListener(spec);
                    l = _listeners.get(spec);
                }
                startListener(l);
            } catch (IOException ioe) {
            	_logger.warning("I m here");
            	ioe.printStackTrace();
                _logger.warning("Unable to start listener " + spec);
                if (_ui != null)
                    _ui.proxyStartError(spec, ioe);
                removeListener(spec);
            }
        }
        _running = true;
        if (_ui != null)
            _ui.setEnabled(_running);
        _status = "Started, Idle";
    }

    public boolean stop() {
        _running = false;
        Iterator<ListenerSpec> it = _listeners.keySet().iterator();
        while (it.hasNext()) {
            ListenerSpec spec = it.next();
            Listener l = _listeners.get(spec);
            if (l != null && !stopListener(l)) {
                _logger
                        .severe("Failed to stop Listener-"
                                + l.getListenerSpec());
                _running = true;
            }
        }
        if (_ui != null)
            _ui.setEnabled(_running);
        _status = "Stopped";
        _webSocketManager.unload();
        try {
            PcapWriter.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return !_running;
    }

    protected long gotRequest(Request request, ConnectionDescriptor connectionDescriptor) {
    	 Ms = new MyService();
    	  
    	StringBuffer bufferrequest = new StringBuffer(request.toString());    	
    	
    	String resultoflis = Ms.ListenerStatus;
    	Log.e("ListenerStatus Of Myservice ", ""+resultoflis);
    	
    	String ip = MyService.serverip;
    	
    	if(ip.equals("") || ip.equals(null)){
    		
    		Log.e("IP is not set in server send request", "fail");
    		
    	}else{
    	
    	if(resultoflis.equals("REQUEST_CAPTURE_START")){
       	
    		 try {	
    	         	InetAddress inet = InetAddress.getByName(ip);
    	     		Socket s=new Socket(inet,45000);
    	     		OutputStream os = s.getOutputStream();
    			    PrintWriter pw = new PrintWriter(os, true);
    				pw.write(bufferrequest.toString());
    	     		pw.close();
    	     		s.close();
    	     		bufferrequest.setLength(0);
    				    Log.e("Request Send", "Suceesdds");} catch (Exception e) {
    				    	e.printStackTrace();
    	         }
    		Log.e("Send Reequest to server ", "Success");
       	    
        }else{
       	 Log.e("Send Reequest to server ", "fail  "+resultoflis);
        }
    	/** End of send request **/
    	
    	
    	}//end of server ip validation.
    	
        long id = _framework.createConversation(request, new Date(System.currentTimeMillis()), FrameworkModel.CONVERSATION_TYPE_PROXY , connectionDescriptor);
        _framework.gotRequest(id, new Date(System.currentTimeMillis()), request);
        return id;
    }

  
    protected void gotResponse(long id, Request request, Response response, boolean dataModified) {
        _framework.gotResponse(id, new Date(System.currentTimeMillis()), request, response, dataModified);
        _framework.cleanConversation(request, response);
    }

    protected SSLSocketFactory getSocketFactory(SiteData hostData) {
        synchronized (_factoryMap) {
        	String certEntry = hostData.tcpAddress != null ? hostData.tcpAddress + "_" + hostData.destPort: hostData.name;
            if (_factoryMap.containsKey(certEntry))
                return (SSLSocketFactory) _factoryMap.get(certEntry);
            SSLSocketFactory factory;
            File p12 = new File(_certDir + certEntry + ".p12");
            factory = loadSocketFactory(p12, certEntry);
            if (factory != null) {
                _factoryMap.put(certEntry, factory);
                return factory;
            }
            factory = generateSocketFactory(hostData);
            if (factory != null) {
                _factoryMap.put(certEntry, factory);
                return factory;
            }
            if (_factoryMap.containsKey(null)) {
                _logger.info("Using default SSL keystore for " + hostData.name);
                return (SSLSocketFactory) _factoryMap.get(null);
            }
            p12 = new File(_certDir + "server.p12");
            factory = loadSocketFactory(p12, certEntry);
            if (factory != null) {
                _factoryMap.put(null, factory);
                return factory;
            }
            _logger.info("Loading default SSL keystore from internal resource");
            InputStream is = _framework.getAndroidContext().getResources()
                                            .openRawResource(R.raw.server_p12);
            if (is == null) {
                _logger
                        .severe("WebScarab JAR was built without a certificate!");
                _logger.severe("SSL Intercept not available!");
                return null;
            }
            factory = loadSocketFactory(is, "VarutraProxy JAR");
            _factoryMap.put(null, factory);
            return factory;
        }
    }

    private SSLSocketFactory loadSocketFactory(File p12, String host) {
        if (p12.exists() && p12.canRead()) {
            _logger.info("Loading SSL keystore for " + host + " from " + p12);
            try {
                InputStream is = new FileInputStream(p12);
                return loadSocketFactory(is, p12.getPath());
            } catch (IOException ioe) {
                _logger.severe("Error reading from " + p12 + ": "
                        + ioe.getLocalizedMessage());
            }
        }
        return null;
    }

    private SSLSocketFactory loadSocketFactory(InputStream is, String source) {
        try {
            KeyManagerFactory kmf = null;
            SSLContext sslcontext = null;
            KeyStore ks = null;
            ks = KeyStore.getInstance("PKCS12", "BC");
            ks.load(is, _keystorepass);
            kmf = KeyManagerFactory.getInstance("X509");
            kmf.init(ks, _keypassword);
            sslcontext = SSLContext.getInstance("TLS");
            
            TrustManager[] trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                        X509Certificate[] certs, String authType) {
                        _logger.fine("trust manager checkClientTrusted authType:" + authType);
                        if (certs != null){
                            for (int i = 0; i < certs.length; i++) {
                                _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                            }
                        }
                    }

                    public void checkServerTrusted(
                        X509Certificate[] certs, String authType) {
                        _logger.fine("trust manager checkClientTrusted authType:" + authType);
                        if (certs != null){
                            for (int i = 0; i < certs.length; i++) {
                                _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                            }
                        }
                    }
                }
            };
            
            
            sslcontext.init(kmf.getKeyManagers(), trustManagers, null);
            return sslcontext.getSocketFactory();

            
            
            
        } catch (IOException ioe) {
            _logger.info("Error reading SSL keystore from " + source + ": "
                    + ioe.getLocalizedMessage());
        } catch (GeneralSecurityException gse) {
            _logger.info("Error reading SSL keystore from " + source + ": "
                    + gse.getLocalizedMessage());
        }
        return null;
    }

    private SSLSocketFactory generateSocketFactory(SiteData hostData) {
        if (_certGenerator == null)
            return null;
        try {
            _logger.info("Generating custom SSL keystore for " + hostData.name);
            return _certGenerator.getSocketFactory(hostData);
        } catch (IOException ioe) {
            _logger.info("Error generating custom SSL keystore for " + hostData.name
                    + ": " + ioe);
        } catch (GeneralSecurityException gse) {
            _logger.info("Error generating custom SSL keystore for " + hostData.name
                    + ": " + gse);
        }
        return null;
    }
    protected void failedResponse(Request request, Response response, long id, String reason, boolean dataModified) {
        _framework.failedResponse(id, new Date(System.currentTimeMillis()), request, response, reason, dataModified);
        _framework.cleanConversation(request, response);
    }

    private void parseListenerConfig() {
        List<String> listListeners = _framework.getListeners();
        
        String[] listeners = new String[listListeners.size()];
        listListeners.toArray(listeners);

        String addr = "";
        String portAsString = null;
        int port = 0;
        HttpUrl base;
        boolean primary = false;

        for (int i = 0; i < listeners.length; i++) {
            addr = "";
            portAsString = null;
            String[] addrParts = listeners[i].split(":");
            for (int j = 0; j < addrParts.length; j++) {
                if (j < addrParts.length - 1){
                    addr += addrParts[j];
                    if (j < addrParts.length - 2){
                        addr += ":";
                    }
                }else{
                    portAsString = addrParts[j];
                }
            }
            try {
                port = Integer.parseInt(portAsString.trim());
            } catch (NumberFormatException nfe) {
                System.err.println("Error parsing port for " + listeners[i]
                        + ", skipping it!");
                continue;
            }
            base = null;
            if (!addr.equalsIgnoreCase("") && port != 0){
                _listeners.put(new ListenerSpec(addr, port, base, primary, false, false, _captureData, _useFakeCerts, _storeSslAsPcap), null);
                if (Preferences.getPreferenceBoolean("preference_proxy_transparent", false)){
                    _listeners.put(new ListenerSpec(addr, Constants.TRANSPARENT_PROXY_HTTP, base, primary, true, false, _captureData, _useFakeCerts, _storeSslAsPcap), null);
                    _listeners.put(new ListenerSpec(addr, Constants.TRANSPARENT_PROXY_HTTPS, base, primary, true, true, _captureData, _useFakeCerts, _storeSslAsPcap), null);
                }
            }else{
                _logger.fine("Warrning Skipping " + listeners[i]);
            }
            
        }
    }

    private void createListener(ListenerSpec spec) {
        Listener l = new Listener(this, spec);

        _listeners.put(spec, l);

        if (_ui != null)
            _ui.proxyAdded(spec);
    }

    public void flush() throws StoreException {
        // we do not run our own store, but our plugins might
        Iterator<ProxyPlugin> it = _plugins.iterator();
        while (it.hasNext()) {
            ProxyPlugin plugin = it.next();
            plugin.flush();
        }
    }

    public boolean isBusy() {
        return _pending > 0;
    }

    public String getStatus() {
        return _status;
    }

    public boolean isModified() {
        return false;
    }

    public void analyse(ConversationID id, Request request, Response response,
            String origin) {
    }

    public void setSession(String type, Object store, String session)
            throws StoreException {
        Iterator<ProxyPlugin> it = _plugins.iterator();
        while (it.hasNext()) {
            ProxyPlugin plugin = it.next();
            plugin.setSession(type, store, session);
        }
    }

    public boolean isRunning() {
        return _running;
    }

    private class ConnectionHook extends Hook {

        public ConnectionHook(String name, String description) {
            super(name, description);
        }

        public void runScripts(ScriptableConnection connection) {
            if (_bsfManager == null)
                return;
            synchronized (_bsfManager) {
                try {
                    _bsfManager.declareBean("connection", connection,
                            connection.getClass());
                    super.runScripts();
                    _bsfManager.undeclareBean("connection");
                } catch (Exception e) {
                    _logger
                            .severe("Declaring or undeclaring a bean should not throw an exception! "
                                    + e);
                }
            }
        }

    }

}
