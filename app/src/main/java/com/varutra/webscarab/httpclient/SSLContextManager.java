package com.varutra.webscarab.httpclient;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

import com.varutra.webscarab.util.Encoding;
import com.varutra.webscarab.util.NullComparator;
public class SSLContextManager {
    
    
    private static Logger _logger = Logger.getLogger(SSLContextManager.class.getName());
    
    private Map _contextMaps = new TreeMap(new NullComparator());
    private SSLContext _noClientCertContext;
    private String _defaultKey = null;
    private Map _aliasPasswords = new HashMap();
    private List _keyStores = new ArrayList();
    private Map _keyStoreDescriptions = new HashMap();
    private TrustManager[] _trustManagers;
    
    static{
        try{
            _logger.setLevel(Level.FINEST);
        }catch (Exception e){
            
        }
    }
    
    private static TrustManager[] _trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                _logger.fine("trust manager checkClientTrusted authType:" + authType);
                if (certs != null){
                    for (int i = 0; i < certs.length; i++) {
                        _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                    }
                }
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                _logger.fine("trust manager checkClientTrusted authType:" + authType);
                if (certs != null){
                    for (int i = 0; i < certs.length; i++) {
                        _logger.fine("trust manager checkClientTrusted:" + certs[i]);
                    }
                }
            }
        }
    };
    
    
    
    /** Creates a new instance of SSLContextManager */
    public SSLContextManager(TrustManager[] trustManagers) {
        try {
            _noClientCertContext = SSLContext.getInstance("TLS");
            if (trustManagers != null){
                _trustManagers = trustManagers;
            }else{
                _trustManagers = _trustAllCerts;
            }
            _noClientCertContext.init(null, _trustManagers, new SecureRandom());
            
        } catch (NoSuchAlgorithmException nsao) {
            _logger.severe("Could not get an instance of the SSL algorithm: " + nsao.getMessage());
        } catch (KeyManagementException kme) {
            _logger.severe("Error initialising the SSL Context: " + kme);
        }
    }
    
    private int addKeyStore(KeyStore ks, String description) {
        int index = _keyStores.indexOf(ks);
        if (index == -1) {
            _keyStores.add(ks);
            index = _keyStores.size() - 1;
        }
        _keyStoreDescriptions.put(ks, description);
        return index;
    }
    
    public int getKeyStoreCount() {
        return _keyStores.size();
    }
    
    public String getKeyStoreDescription(int keystoreIndex) {
        return (String) _keyStoreDescriptions.get(_keyStores.get(keystoreIndex));
    }
    
    public int getAliasCount(int keystoreIndex) {
        return getAliases((KeyStore) _keyStores.get(keystoreIndex)).length;
    }
    
    public String getAliasAt(int keystoreIndex, int aliasIndex) {
        return getAliases((KeyStore) _keyStores.get(keystoreIndex))[aliasIndex];
    }
    
    private String[] getAliases(KeyStore ks) {
        List aliases = new ArrayList();
        try {
            Enumeration en = ks.aliases();
            while (en.hasMoreElements()) {
                String alias = (String) en.nextElement();
                if (ks.isKeyEntry(alias))
                    aliases.add(alias);
            }
        } catch (KeyStoreException kse) {
            kse.printStackTrace();
        }
        return (String[]) aliases.toArray(new String[0]);
    }
    
    public Certificate getCertificate(int keystoreIndex, int aliasIndex) {
        try {
            KeyStore ks = (KeyStore) _keyStores.get(keystoreIndex);
            String alias = getAliasAt(keystoreIndex, aliasIndex);
            return ks.getCertificate(alias);
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getFingerPrint(Certificate cert) throws KeyStoreException {
        if (!(cert instanceof X509Certificate)) return null;
        StringBuffer buff = new StringBuffer();
        X509Certificate x509 = (X509Certificate) cert;
        try {
            String fingerprint = Encoding.hashMD5(cert.getEncoded());
            for (int i=0; i<fingerprint.length(); i+=2) {
                buff.append(fingerprint.substring(i, i+1)).append(":");
            }
            buff.deleteCharAt(buff.length()-1);
        } catch (CertificateEncodingException e) {
            throw new KeyStoreException(e.getMessage());
        }
        String dn = x509.getSubjectDN().getName();
        _logger.info("Fingerprint is " + buff.toString().toUpperCase());
        return buff.toString().toUpperCase() + " " + dn;
    }
    
    public boolean isKeyUnlocked(int keystoreIndex, int aliasIndex) {
        KeyStore ks = (KeyStore) _keyStores.get(keystoreIndex);
        String alias = getAliasAt(keystoreIndex, aliasIndex);
        
        Map pwmap = (Map) _aliasPasswords.get(ks);
        if (pwmap == null) return false;
        return pwmap.containsKey(alias);
    }
    
    public void setDefaultKey(String fingerprint) {
        _defaultKey = fingerprint;
    }
    
    public String getDefaultKey() {
        return _defaultKey;
    }

    public int loadPKCS12Certificate(InputStream filenameStream, String alias, String ksPassword)
    throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        InputStream is = filenameStream;

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, ksPassword == null ? null : ksPassword.toCharArray());
        return addKeyStore(ks, "PKCS#12 - " + alias);
    }
    
    
    public int loadPKCS12Certificate(String filename, String ksPassword)
    throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        InputStream is = new FileInputStream(filename);
        if (is == null)
            throw new FileNotFoundException(filename + " could not be found");
        
        
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(is, ksPassword == null ? null : ksPassword.toCharArray());
        return addKeyStore(ks, filename);
    }
    
    public void unlockKey(int keystoreIndex, int aliasIndex, String keyPassword) throws KeyStoreException, KeyManagementException {
        KeyStore ks = (KeyStore) _keyStores.get(keystoreIndex);
        String alias = getAliasAt(keystoreIndex, aliasIndex);
        
        AliasKeyManager akm = new AliasKeyManager(ks, alias, keyPassword);
        
        String fingerprint = getFingerPrint(getCertificate(keystoreIndex, aliasIndex));
        
        if (fingerprint == null) {
            _logger.severe("No fingerprint found");
            return;
        }
        
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException nsao) {
            _logger.severe("Could not get an instance of the SSL algorithm: " + nsao.getMessage());
            return;
        }
        sc.init(new KeyManager[] { akm }, _trustManagers, new SecureRandom());
        
        String key = fingerprint;
        if (key.indexOf(" ")>0)
            key = key.substring(0, key.indexOf(" "));
        _contextMaps.put("key", sc);
    }
    
    public void invalidateSessions() {
        invalidateSession(_noClientCertContext);
        Iterator it = _contextMaps.keySet().iterator();
        while (it.hasNext()) {
            invalidateSession((SSLContext)_contextMaps.get(it.next()));
        }
    }
    
    private void invalidateSession(SSLContext sc) {
        SSLSessionContext sslsc = sc.getClientSessionContext();
        if (sslsc != null) {
            int timeout = sslsc.getSessionTimeout();
            sslsc.setSessionTimeout(1);
            sslsc.setSessionTimeout(timeout);
        }
        sslsc = sc.getServerSessionContext();
        if (sslsc != null) {
            int timeout = sslsc.getSessionTimeout();
            sslsc.setSessionTimeout(1);
            sslsc.setSessionTimeout(timeout);
        }
    }
    
    public SSLContext getSSLContext(String fingerprint) {
    	Log.e("SSLContext getSSLContext", ""+fingerprint);
        _logger.info("Requested SSLContext for " + fingerprint);
        
        if (fingerprint == null || fingerprint.equals("none"))
            return _noClientCertContext;
        if (fingerprint.indexOf(" ")>0)
            fingerprint = fingerprint.substring(0, fingerprint.indexOf(" "));
        return (SSLContext) _contextMaps.get(fingerprint);
    }
    
}
