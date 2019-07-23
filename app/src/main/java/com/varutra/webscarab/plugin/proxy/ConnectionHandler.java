package com.varutra.webscarab.plugin.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.varutra.webscarab.model.ConnectionDescriptor;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.util.HtmlEncoder;
import com.varutraproxy.utils.DNSProxy;

import com.varutra.utils.preference.CheckOptionApp;
import com.varutra.webscarab.httpclient.HTTPClient;
import com.varutra.webscarab.httpclient.HTTPClientFactory;

import android.util.Log;

public class ConnectionHandler implements Runnable {
    
    private ProxyPlugin[] _plugins = null;
    private Proxy _proxy;
    private Socket _sock = null;
    private HttpUrl _base;
    private boolean _transparent = false;
    private boolean _transparentSecure = false;
    private boolean _captureData = true;
    private boolean _useFakeCerts = false;
    private boolean _storeSslAsPcap = false;
    private int _destPort = 0;
    private ITransparentProxyResolver _transparentResolver = null;
    private ConnectionDescriptor _connectionDescriptor = null;
    private String clientId = "device";

    private HTTPClient _httpClient = null;

    private Logger _logger = Logger.getLogger(getClass().getName());
    
    private static boolean LOGD = false;
    private static String TAG = ConnectionHandler.class.getSimpleName();
    private static int _socket_timeout_large = 1000 * 60 * 30;
    private static int _socket_timeout_normal = 1000 * 30;
    
    private InputStream _clientIn = null;
    private OutputStream _clientOut = null;

    public ConnectionHandler(Proxy proxy, Socket sock, HttpUrl base, boolean transparent, boolean transparentSecure, 
                                                            boolean captureData, boolean useFakeCerts, boolean storeSslAsPcap,
                                                            ITransparentProxyResolver transparentProxyResolver,
                                                            ConnectionDescriptor connectionDescriptor) {
        _logger.setLevel(Level.FINEST);
        _proxy = proxy;
        _sock = sock;
        _base = base;
        _transparent = transparent;
        _transparentSecure = transparentSecure;
        _transparentResolver = transparentProxyResolver;
        _connectionDescriptor = connectionDescriptor;
        _plugins = _proxy.getPlugins();
        _captureData = captureData;
        _useFakeCerts = useFakeCerts;
        _storeSslAsPcap = storeSslAsPcap;
        if (connectionDescriptor != null && connectionDescriptor.getId() > -1){
            int uid = connectionDescriptor.getId();
            int port = connectionDescriptor.getRemotePort();
            clientId = connectionDescriptor.getNamespace() + " <" + connectionDescriptor.getId() + ">";
            Map<Integer, CheckOptionApp> appOptions =  _proxy.getAppOptions();
            if (_transparent || _transparentSecure){
                _destPort = port;
                if(appOptions.containsKey(CheckOptionApp.ALL_UID)){
                    CheckOptionApp appOpt = appOptions.get(CheckOptionApp.ALL_UID);
                    if (appOpt != null && appOpt.CE && appOpt.CustomPortRules != null){
                        if (appOpt.CustomPortRules.containsKey(port) && appOpt.CustomPortRules.get(port).CPM != port){
                            _destPort = appOpt.CustomPortRules.get(port).CPM;
                        }
                    }
                }else if(appOptions.containsKey(uid)){
                    CheckOptionApp appOpt = appOptions.get(uid);
                    if (appOpt != null && appOpt.CE && appOpt.CustomPortRules != null){
                        if (appOpt.CustomPortRules.containsKey(port) && appOpt.CustomPortRules.get(port).CPM != port){
                            _destPort = appOpt.CustomPortRules.get(port).CPM;
                        }
                    }
                }
            }
            if (!_captureData){
                if (appOptions != null){
                    if(appOptions.containsKey(CheckOptionApp.ALL_UID)){
                        CheckOptionApp appOpt = appOptions.get(CheckOptionApp.ALL_UID);
                        if (appOpt != null && appOpt.CE && appOpt.CustomPortRules != null){
                            if (appOpt.CustomPortRules.containsKey(port) && appOpt.CustomPortRules.get(port).SF){
                                _storeSslAsPcap = true;
                            }
                        }
                    }
                    if(appOptions.containsKey(uid)){
                        CheckOptionApp appOpt = appOptions.get(uid);
                        if (appOpt != null && appOpt.CE && appOpt.CustomPortRules != null){
                            if (appOpt.CustomPortRules.containsKey(port) && appOpt.CustomPortRules.get(port).SF){
                                _storeSslAsPcap = true;
                            }
                        }
                    }
                }
            }
        }else{
            clientId = "<" + _sock.getInetAddress().getHostAddress() + ":" + _sock.getPort() + ">";
        }
        try {
            _sock.setTcpNoDelay(true);
            _sock.setSoTimeout(_socket_timeout_normal);
        } catch (SocketException se) {
            _logger.warning("Error setting socket parameters");
        }
        if (LOGD) Log.d(TAG, "Destination port is " + _destPort);
    }

    public void run() {
        ScriptableConnection connection = new ScriptableConnection(_sock);
        _proxy.allowClientConnection(connection);
        if (_sock.isClosed())
            return;

        try {
            _clientIn = _sock.getInputStream();
            _clientOut = _sock.getOutputStream();
        } catch (IOException ioe) {
            _logger.severe("Error getting socket input and output streams! "
                    + ioe);
            return;
        }
        long conversationId = -1;
        boolean httpDataModified = false;
        boolean switchProtocol = false;
        try {
            
            ConnectionDescriptor connectionDescriptor = _connectionDescriptor;
            Request request = null;
            // if we do not already have a base URL (i.e. we operate as a normal
            // proxy rather than a reverse proxy), check for a CONNECT
            if (_base == null && !_transparentSecure){
                try {
                    request = new Request(_transparent, _transparentSecure, _connectionDescriptor);
                    request.read(_clientIn);
                    HttpUrl requestUrl = request.getURL();
                    if (requestUrl != null){
                        String host = requestUrl.getHost();
                        String reverseHost = DNSProxy.getHostNameFromIp(host);
                        if (reverseHost != null){
                            host = reverseHost != null ? reverseHost : host;
                            requestUrl = new HttpUrl(requestUrl.getScheme() + "://" + host +":" +  requestUrl.getPort() + requestUrl.getPath());
                            request.setURL(requestUrl);
                            request.setHeader("Host", host);
                        }
                        if (_destPort != 0 && _destPort != requestUrl.getPort()){
                            requestUrl = new HttpUrl(requestUrl.getScheme() + "://" + requestUrl.getHost() +":" +  _destPort + requestUrl.getPath());
                            request.setURL(requestUrl);
                        }
                    }
                } catch (IOException ioe) {
                    _logger.severe("Error reading the initial request" + ioe);
                    return;
                }
            }
            String proxyAuth = null;
            if (request != null) {
                String method = request.getMethod();
                if (request.getURL() == null) {
                    return;
                } else if (method.equals("CONNECT")) {
                    if (_clientOut != null) {
                        try {
                            _clientOut.write(("HTTP/1.0 200 Ok\r\n\r\n")
                                    .getBytes());
                            _clientOut.flush();
                        } catch (IOException ioe) {
                            _logger
                                    .severe("IOException writing the CONNECT OK Response to the " + clientId
                                            + ioe);
                            return;
                        }
                    }
                    _base = request.getURL();
                    proxyAuth = request.getHeader("Proxy-Authorization");
                    request = null;
                }
            }
            if (_base != null || _transparentSecure) {
                if (_transparentSecure || _base.getScheme().equals("https")) {
                    SiteData hostData = null;
                    if (!_captureData){
                        if (_transparentSecure){
                            if (_transparentResolver != null){
                                hostData = _transparentResolver.getSecureHost(_sock, _destPort, false);
                            }else{
                                _logger.fine("!! Error Can not act as forwarder on transparent ssl, not knowing where to connect.");
                                _sock.close();
                                return;
                            }
                            String siteName = hostData.name == null ? hostData.tcpAddress : "";
                            String forwarderName = siteName + ":" + hostData.destPort;
                            _logger.fine("Acting as forwarder on " + forwarderName + " for " + clientId);
                            String hostName = hostData.hostName != null ? hostData.hostName : hostData.tcpAddress;
                            _base = new HttpUrl("https://" + hostName + ":" +  hostData.destPort);
                            Socket target;
                            if (_useFakeCerts){
                                _sock = negotiateSSL(_sock, hostData, true);
                                target = HTTPClientFactory.getValidInstance().getConnectedSocket(_base, true);
                            } else{
                                target = HTTPClientFactory.getValidInstance().getConnectedSocket(_base, false);
                            }
                            SocketForwarder.connect(forwarderName, _sock, target, _storeSslAsPcap, _proxy.getPcapStorageDir(), connectionDescriptor);
                            return;
                        }else{
                            String forwarderName = _base.getHost() + ":" + _base.getPort();
                            _logger.fine("Acting as forwarder on " + forwarderName + " for " + clientId);
                            Socket target;
                            if (_useFakeCerts){
                                hostData = new SiteData();
                                hostData.hostName = _base.getHost();
                                hostData.destPort = _base.getPort();
                                hostData.name = hostData.hostName;
                                _sock = negotiateSSL(_sock, hostData, true);
                                target = HTTPClientFactory.getValidInstance().getConnectedSocket(_base, true);
                            } else{
                                target = HTTPClientFactory.getValidInstance().getConnectedSocket(_base, false);
                            }
                            SocketForwarder.connect(forwarderName, _sock, target, _storeSslAsPcap, _proxy.getPcapStorageDir(), connectionDescriptor);
                            return;
                        }
                        
                    }
                    _logger.fine("Intercepting SSL connection!");
                    if (_transparentSecure){
                        if (_transparentResolver != null){
                            hostData = _transparentResolver.getSecureHost(_sock, _destPort, true);
                        }
                    }else{
                        hostData = new SiteData();
                        hostData.name = _base.getHost();
                    }
                    String host = "varutraproxy.untrusted";
                    if (hostData == null || hostData.name.trim().length() == 0){
                        hostData = new SiteData();
                        hostData.name = host;
                    }
                    
                    boolean isSSLPort = false;
                    boolean checkForSSL = true;
                    if (!_transparentSecure){
                        if (_base.getPort() == 443){
                            isSSLPort = true;
                            checkForSSL = false;
                        }else if (_base.getPort() == 80){
                            isSSLPort = false;
                            checkForSSL = false;
                        }
                    }else{
                        checkForSSL = false;
                        isSSLPort = true;
                    }
                    
                    if (checkForSSL){
                        try{
                            int testReadTimeout = 2000;
                            HTTPClient hc =  HTTPClientFactory.getValidInstance().getHTTPClient(-1, testReadTimeout);
                            Request testRequest = new Request();
                            testRequest.setURL(_base);
                            testRequest.setMethod("GET");
                            testRequest.setNoBody();
                            hc.fetchResponse(testRequest);
                            isSSLPort = true;
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                    }
                    
                    if (isSSLPort){
                        SSLSocket sslSocket = null;
                        try{
                            _sock = negotiateSSL(_sock, hostData, false);
                            sslSocket = (SSLSocket)_sock;
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }
                        if (sslSocket == null || sslSocket.getSession() == null){
                            _logger.finest("!!Error Check if " + clientId + " trust VarutraProxy CA certificate or ignore on ws:// protocol");
                            String oldHost = _base.getHost();
                            int oldPort = _base.getPort();
                            _base = new HttpUrl("http://" + oldHost + ":"+ oldPort);
                        }else{
                            
                            _clientIn = _sock.getInputStream();
                            PushbackInputStream pis = new PushbackInputStream(_clientIn);
                            int readBit;
                            try{
                                readBit = pis.read();
                            }catch (Exception ex){
                                _logger.finest("!!Error Check if " + clientId + " trust VarutraProxy CA certificate \n!! or could be using SSL pinning so mitm will not work");
                                return;
                            }
                            
                            if (readBit != -1){
                                pis.unread(readBit);
                            }else{
                                _logger.finest("!!Error Check if " + clientId + " trust VarutraProxy CA certificate \n!! or could be using SSL pinning so mitm will not work");
                                return;
                            }
                            _clientIn = pis;
                            _clientOut = _sock.getOutputStream();
                        }
                    }else{
                        String oldHost = _base.getHost();
                        int oldPort = _base.getPort();
                        _base = new HttpUrl("http://" + oldHost + ":"+ oldPort);
                    }
                }
            }
            
            if (_httpClient == null)
                _httpClient = HTTPClientFactory.getValidInstance().getHTTPClient();

            HTTPClient hc = _httpClient;

            if (_plugins != null) {
                for (int i = _plugins.length - 1; i >= 0; i--) {
                    ProxyPlugin plugin = _plugins[i];
                    if (plugin.getEnabled()){
                        httpDataModified = true;
                    }
                    hc = plugin.getProxyPlugin(hc);
                }
            }

            String keepAlive = null;
            String version = null;
            int reuseCount = 1;
            do {
                conversationId = -1;
                if (request == null) {
                    request = new Request(_transparent, _transparentSecure, _connectionDescriptor);
                    Log.e("Reading request from the  ", "clientId "+clientId);
                    _logger.fine("Reading request from the " + clientId);
                    _sock.setSoTimeout(_socket_timeout_large);
                    request.read(_clientIn, _base);
                    if (request.getMethod() == null || request.getURL() == null) {
                        return;
                    }
                    HttpUrl requestUrl = request.getURL();
                    String host = requestUrl.getHost();
                    String reverseHost = DNSProxy.getHostNameFromIp(host);
                    if (reverseHost != null){
                        host = reverseHost != null ? reverseHost : host;
                        requestUrl = new HttpUrl(requestUrl.getScheme() + "://" + host +":" +  requestUrl.getPort() + requestUrl.getPath());
                        request.setURL(requestUrl);
                        request.setHeader("Host", host);
                    }
                    if (proxyAuth != null) {
                        request.addHeader("Proxy-Authorization", proxyAuth);
                    }
                }
               
                if (request.getURL() == null){
                    return;
                }
                
                if (request.getMethod().equals("CONNECT")){
                    if (_clientOut != null) {
                        try {
                            if (LOGD) Log.d(TAG, "Having connect method so we send that we are already connected");
                            _clientOut.write(("HTTP/1.0 200 Ok\r\n\r\n")
                                    .getBytes());
                            _clientOut.flush();
                            
                            Response response = new Response();
                            response.setStatus("200");
                            response.setMessage("OK");
                            response.setHeader("X-VarutraProxy-Hack", "CONNECT_OVER_SSL_BUG http://code.google.com/p/android/issues/detail?id=55003");
                            response.setNoBody();
                            // store this conversation in store if enabled
                           
                            conversationId = _proxy.gotRequest(request, connectionDescriptor);
                            _proxy.gotResponse(conversationId, request, response, false);
                            request = null;
                            continue;
                        } catch (IOException ioe) {
                            _logger
                                    .severe("IOException writing the CONNECT OK Response to the " + clientId
                                            + ioe);
                            return;
                        }
                    }
                }
                
                String clientDesc = "";
                if (connectionDescriptor != null && connectionDescriptor.getNamespaces() != null && connectionDescriptor.getNamespaces().length > 0){
                    clientDesc = connectionDescriptor.getNamespaces()[0];
                }
                _logger.fine( clientDesc + " requested : " + request.getMethod() + " "+ request.getURL().toString());

                conversationId = _proxy.gotRequest(request, connectionDescriptor);

                connection.setRequest(request);
                connection.setResponse(null);
                _proxy.interceptRequest(connection);
                request = connection.getRequest();
                Response response = connection.getResponse();

                if (request == null)
                    throw new IOException("Request was cancelled");
                if (response != null) {
                    _proxy.failedResponse(request, response, conversationId, "Response provided by script", httpDataModified);
                    _proxy = null;
                } else {
                    try {
                        response = hc.fetchResponse(request);
                        if (response.getRequest() != null)
                            request = response.getRequest();
                    } catch (IOException ioe) {
                        _logger
                                .severe("IOException retrieving the response for "
                                        + request.getURL() + " : " + ioe);
                        ioe.printStackTrace();
                        if (ioe.getMessage() != null && ioe.getMessage().equals(Response.NO_DATA_FROM_SERVER)){
                            _logger.fine("Nothing to read from server.Closing connection");
                            _clientOut.close();
                            connection.closeConnection();
                            
                            return;
                        }else{
                            response = errorResponse(request, ioe);
                        }
                        
                        _proxy.failedResponse(request, response, conversationId, ioe.toString(), httpDataModified);
                        _proxy = null;
                    }
                    if (response == null) {
                        _logger.severe("Got a null response from the fetcher");
                        _proxy.failedResponse(request, response, conversationId, "Null response", httpDataModified);
                        return;
                    }
                }

                if (_proxy != null) {
                    connection.setResponse(response);
                    _proxy.interceptResponse(connection);
                    response = connection.getResponse();
                }

                if (response == null)
                    throw new IOException("Response was cancelled");

                try {
                    if (_clientOut != null) {
                        _logger.fine("Writing the response to the " + clientId);
                        if (response.getStatus().equalsIgnoreCase("101")){
                            switchProtocol = true;
                            _logger.fine("Switching protocols on 101 code");
                            _proxy.getWebSocketManager().addWebSocketsChannel(conversationId, response, _sock, response.getSocket(), response.getSocket().getInputStream());
                            response.writeSwitchProtocol(_clientOut);
                            _logger.fine("Finished writing headers to " + clientId);
                        }else{
                            response.write(_clientOut);
                        }
                        
                        _logger.fine("Finished writing the response to the " + clientId);
                    }
                } catch (IOException ioe) {
                    _logger
                            .severe("Error writing back to the " + clientId + " : "
                                    + ioe);
                } finally {
                    if (switchProtocol){
                        response.flushContentStream(); 
                    }
                }
                if (response.getRequest() == null) {
                    _logger.warning("Response had no associated request!");
                    response.setRequest(request);
                }
                if (_proxy != null && !request.getMethod().equals("CONNECT")) {
                    _proxy.gotResponse(conversationId, request, response, httpDataModified);
                }

                keepAlive = response.getHeader("Connection");
                version = response.getVersion();

                request = null;
                response = null;

                _logger.fine("Version: " + version + " keepAlive: " + keepAlive + " reuseCount:" + reuseCount);
                reuseCount++;
            } while (!switchProtocol && 
                    ((version.equals("HTTP/1.0") && "keep-alive".equalsIgnoreCase(keepAlive)) 
                    || (version.equals("HTTP/1.1") && !"close".equalsIgnoreCase(keepAlive)))
                    );
            _logger.fine("Finished handling connection");
        } catch (Exception e) {
            if (conversationId != -1)
                _proxy.failedResponse(null, null, conversationId, e.getMessage(), httpDataModified);
            _logger.severe("ConnectionHandler got an error : " + e);
            e.printStackTrace();
        } finally {
            try {
                if (!switchProtocol){
                    if (_clientIn != null)
                        _clientIn.close();
                    if (_clientOut != null)
                        _clientOut.close();
                    if (_sock != null && !_sock.isClosed()) {
                        _sock.close();
                    }
                }
                
            } catch (IOException ioe) {
                _logger.warning("Error closing " + clientId + "client socket : " + ioe);
            }
        }
    }

    
    private static String[] wiresharkSupportedCiphers = new String[]
    {
        "TLS_RSA_WITH_NULL_MD5",
        "TLS_RSA_WITH_NULL_SHA",
        "TLS_RSA_EXPORT_WITH_RC4_40_MD5",
        "TLS_RSA_WITH_RC4_128_MD5",
        "TLS_RSA_WITH_RC4_128_SHA",
        "TLS_RSA_EXPORT_WITH_RC2_CBC_40_MD5",
        "TLS_RSA_WITH_IDEA_CBC_SHA",
        "TLS_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_RSA_WITH_DES_CBC_SHA",
        "TLS_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_DH_DSS_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_DH_DSS_WITH_DES_CBC_SHA",
        "TLS_DH_DSS_WITH_3DES_EDE_CBC_SHA",
        "TLS_DH_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_DH_RSA_WITH_DES_CBC_SHA",
        "TLS_DH_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_DHE_DSS_WITH_DES_CBC_SHA",
        "TLS_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
        "TLS_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_DHE_RSA_WITH_DES_CBC_SHA",
        "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_DH_Anon_EXPORT_WITH_RC4_40_MD5",
        "TLS_DH_Anon_WITH_RC4_128_MD5",
        "TLS_DH_Anon_EXPORT_WITH_DES40_CBC_SHA",
        "TLS_DH_Anon_WITH_DES_CBC_SHA",
        "TLS_DH_Anon_WITH_3DES_EDE_CBC_SHA",
        "SSL_FORTEZZA_KEA_WITH_NULL_SHA",
        "SSL_FORTEZZA_KEA_WITH_FORTEZZA_CBC_SHA",
        "TLS_KRB5_WITH_DES_CBC_SHA",
        "TLS_KRB5_WITH_3DES_EDE_CBC_SHA",
        "TLS_KRB5_WITH_RC4_128_SHA",
        "TLS_KRB5_WITH_IDEA_CBC_SHA",
        "TLS_KRB5_WITH_DES_CBC_MD5",
        "TLS_KRB5_WITH_3DES_EDE_CBC_MD5",
        "TLS_KRB5_WITH_RC4_128_MD5",
        "TLS_KRB5_WITH_IDEA_CBC_MD5",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA",
        "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_SHA",
        "TLS_KRB5_EXPORT_WITH_RC4_40_SHA",
        "TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5",
        "TLS_KRB5_EXPORT_WITH_RC2_CBC_40_MD5",
        "TLS_KRB5_EXPORT_WITH_RC4_40_MD5",
        "TLS_PSK_WITH_NULL_SHA",
        "TLS_DHE_PSK_WITH_NULL_SHA",
        "TLS_RSA_PSK_WITH_NULL_SHA",
        "TLS_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DH_DSS_WITH_AES_128_CBC_SHA",
        "TLS_DH_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DH_Anon_WITH_AES_128_CBC_SHA",
        "TLS_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DH_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DH_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DH_Anon_WITH_AES_256_CBC_SHA",
        "TLS_RSA_WITH_NULL_SHA256",
        "TLS_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DH_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_DH_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_DH_DSS_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_DH_RSA_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_DH_Anon_WITH_CAMELLIA_128_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_NULL_SHA",
        "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
        "TLS_ECDH_ECDSA_WITH_DES_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_RSA_EXPORT1024_WITH_RC4_56_MD5",
        "TLS_RSA_EXPORT1024_WITH_RC2_CBC_56_MD5",
        "TLS_RSA_EXPORT1024_WITH_DES_CBC_SHA",
        "TLS_DHE_DSS_EXPORT1024_WITH_DES_CBC_SHA",
        "TLS_RSA_EXPORT1024_WITH_RC4_56_SHA",
        "TLS_DHE_DSS_EXPORT1024_WITH_RC4_56_SHA",
        "TLS_DHE_DSS_WITH_RC4_128_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DH_DSS_WITH_AES_256_CBC_SHA256",
        "TLS_DH_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DH_Anon_WITH_AES_128_CBC_SHA256",
        "TLS_DH_Anon_WITH_AES_256_CBC_SHA256",
        "TLS_GOSTR341094_WITH_28147_CNT_IMIT",
        "TLS_GOSTR341001_WITH_28147_CNT_IMIT",
        "TLS_GOSTR341094_WITH_NULL_GOSTR3411",
        "TLS_GOSTR341001_WITH_NULL_GOSTR3411",
        "TLS_RSA_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DH_DSS_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DH_RSA_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_DH_Anon_WITH_CAMELLIA_256_CBC_SHA",
        "TLS_PSK_WITH_RC4_128_SHA",
        "TLS_PSK_WITH_3DES_EDE_CBC_SHA",
        "TLS_PSK_WITH_AES_128_CBC_SHA",
        "TLS_PSK_WITH_AES_256_CBC_SHA",
        "TLS_DHE_PSK_WITH_RC4_128_SHA",
        "TLS_DHE_PSK_WITH_3DES_EDE_CBC_SHA",
        "TLS_DHE_PSK_WITH_AES_128_CBC_SHA",
        "TLS_DHE_PSK_WITH_AES_256_CBC_SHA",
        "TLS_RSA_PSK_WITH_RC4_128_SHA",
        "TLS_RSA_PSK_WITH_3DES_EDE_CBC_SHA",
        "TLS_RSA_PSK_WITH_AES_128_CBC_SHA",
        "TLS_RSA_PSK_WITH_AES_256_CBC_SHA",
        "TLS_RSA_WITH_SEED_CBC_SHA",
        "TLS_DH_DSS_WITH_SEED_CBC_SHA",
        "TLS_DH_RSA_WITH_SEED_CBC_SHA",
        "TLS_DHE_DSS_WITH_SEED_CBC_SHA",
        "TLS_DHE_RSA_WITH_SEED_CBC_SHA",
        "TLS_DH_Anon_WITH_SEED_CBC_SHA",
        "TLS_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DH_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DH_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
        "TLS_DH_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_DH_DSS_WITH_AES_256_GCM_SHA384",
        "TLS_DH_Anon_WITH_AES_128_GCM_SHA256",
        "TLS_DH_Anon_WITH_AES_256_GCM_SHA384",
        "TLS_PSK_WITH_AES_128_GCM_SHA256",
        "TLS_PSK_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_PSK_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_PSK_WITH_AES_256_GCM_SHA384",
        "TLS_RSA_PSK_WITH_AES_128_GCM_SHA256",
        "TLS_RSA_PSK_WITH_AES_256_GCM_SHA384",
        "TLS_PSK_WITH_AES_128_CBC_SHA256",
        "TLS_PSK_WITH_AES_256_CBC_SHA384",
        "TLS_PSK_WITH_NULL_SHA256",
        "TLS_PSK_WITH_NULL_SHA384",
        "TLS_DHE_PSK_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_PSK_WITH_AES_256_CBC_SHA384",
        "TLS_DHE_PSK_WITH_NULL_SHA256",
        "TLS_DHE_PSK_WITH_NULL_SHA384",
        "TLS_RSA_PSK_WITH_AES_128_CBC_SHA256",
        "TLS_RSA_PSK_WITH_AES_256_CBC_SHA384",
        "TLS_RSA_PSK_WITH_NULL_SHA256",
        "TLS_RSA_PSK_WITH_NULL_SHA384",
        "TLS_ECDH_ECDSA_WITH_NULL_SHA",
        "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
        "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_NULL_SHA",
        "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
        "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_RSA_WITH_NULL_SHA",
        "TLS_ECDH_RSA_WITH_RC4_128_SHA",
        "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_NULL_SHA",
        "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
        "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDH_Anon_WITH_NULL_SHA",
        "TLS_ECDH_Anon_WITH_RC4_128_SHA",
        "TLS_ECDH_Anon_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDH_Anon_WITH_AES_128_CBC_SHA",
        "TLS_ECDH_Anon_WITH_AES_256_CBC_SHA",
        "TLS_SRP_SHA_WITH_3DES_EDE_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_3DES_EDE_CBC_SHA",
        "TLS_SRP_SHA_DSS_WITH_3DES_EDE_CBC_SHA",
        "TLS_SRP_SHA_WITH_AES_128_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA",
        "TLS_SRP_SHA_DSS_WITH_AES_128_CBC_SHA",
        "TLS_SRP_SHA_WITH_AES_256_CBC_SHA",
        "TLS_SRP_SHA_RSA_WITH_AES_256_CBC_SHA",
        "TLS_SRP_SHA_DSS_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_PSK_WITH_RC4_128_SHA",
        "TLS_ECDHE_PSK_WITH_3DES_EDE_CBC_SHA",
        "TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_PSK_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_PSK_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_PSK_WITH_NULL_SHA",
        "TLS_ECDHE_PSK_WITH_NULL_SHA256",
        "TLS_ECDHE_PSK_WITH_NULL_SHA384",
        "SSL_RSA_FIPS_WITH_DES_CBC_SHA",
        "SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA",
        "SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA",
        "SSL_RSA_FIPS_WITH_DES_CBC_SHA",
        "SSL2_RC4_128_WITH_MD5",
        "SSL2_RC4_128_EXPORT40_WITH_MD5",
        "SSL2_RC2_CBC_128_CBC_WITH_MD5",
        "SSL2_RC2_CBC_128_CBC_WITH_MD5",
        "SSL2_IDEA_128_CBC_WITH_MD5",
        "SSL2_DES_64_CBC_WITH_MD5",
        "SSL2_DES_192_EDE3_CBC_WITH_MD5",
        "SSL2_RC4_64_WITH_MD5",
        "PCT_SSL_CERT_TYPE",
        
    };
    
    private static List<String> listWiresharkSupportedCiphers = Arrays.asList(wiresharkSupportedCiphers);
    private static String[] selectedCiphers = null;
    
    private String[] selectCiphers(String[] supportedCiphers){
        if (selectedCiphers == null){
            List<String> listSelectedCiphers = new ArrayList<String>();
            for (String supportedCipher : supportedCiphers) {
                if (listWiresharkSupportedCiphers.contains(supportedCipher)){
                    _logger.info("Cipher added to list " + supportedCipher);
                    listSelectedCiphers.add(supportedCipher);
                }else{
                    _logger.info("!!! Cipher removed from list " + supportedCipher);
                }
            }
            if (listSelectedCiphers.size() == 0){
                String msg = "!!!Error Cipher list is empty";
                _logger.info(msg);
                Log.e(TAG, msg);
            }
            Collections.reverse(listSelectedCiphers);
            selectedCiphers = new String[listSelectedCiphers.size()];
            for (int i = 0; i < selectedCiphers.length; i++) {
                String selectedCipher = listSelectedCiphers.get(i);
                _logger.info("adde cipher to pos " + i + " : " + selectedCipher);
                selectedCiphers[i] = selectedCipher;
            }
            return selectedCiphers;
        }else{
            return selectedCiphers;
        }
    }

    private Socket negotiateSSL(Socket sock, SiteData hostData, boolean useOnlyWiresharkDissCiphers) throws Exception {
        SSLSocketFactory factory = _proxy.getSocketFactory(hostData);
        if (factory == null)
            throw new RuntimeException(
                    "SSL Intercept not available - no keystores available");
        SSLSocket sslsock;
        try {
            int sockPort = sock.getPort();
            String hostName = hostData.tcpAddress != null ? hostData.tcpAddress : hostData.name;
            sslsock = (SSLSocket) factory.createSocket(sock, hostName, sockPort, false);
            if (useOnlyWiresharkDissCiphers){
                // force chiper that can be decrypted with wireshark
                String[] ciphers = selectCiphers(sslsock.getSupportedCipherSuites());
                sslsock.setEnabledCipherSuites(ciphers);
            }
            sslsock.setUseClientMode(false);
            _logger.info("Finished negotiating client SSL - algorithm is "
                    + sslsock.getSession().getCipherSuite());
            return sslsock;
        } catch (Exception e) {
            _logger.severe("Error layering SSL over the socket: " + e);
            throw e;
        }
    }

    private Response errorResponse(Request request, Exception e) {
        Response response = new Response();
        response.setRequest(request);
        response.setVersion("HTTP/1.0");
        response.setStatus("500");
        response.setMessage("VarutraProxy error");
        response.setHeader("Content-Type", "text/html");
        response.setHeader("Connection", "Close");
        String template = "<HTML><HEAD><TITLE>VarutraProxy Error</TITLE></HEAD>";
        template = template
                + "<BODY>VarutraProxy encountered an error trying to retrieve <P><pre>"
                + HtmlEncoder.encode(request.toString()) + "</pre><P>";
        template = template + "The error was : <P><pre>"
                + HtmlEncoder.encode(e.getLocalizedMessage()) + "\n";
        StackTraceElement[] trace = e.getStackTrace();
        if (trace != null) {
            for (int i = 0; i < trace.length; i++) {
                template = template + "\tat " + trace[i].getClassName() + "."
                        + trace[i].getMethodName() + "(";
                if (trace[i].getLineNumber() == -2) {
                    template = template + "Native Method";
                } else if (trace[i].getLineNumber() == -1) {
                    template = template + "Unknown Source";
                } else {
                    template = template + trace[i].getFileName() + ":"
                            + trace[i].getLineNumber();
                }
                template = template + ")\n";
            }
        }
        template = template + "</pre><P></HTML>";
        response.setContent(template.getBytes());
        return response;
    }

}