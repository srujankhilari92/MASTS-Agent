
package com.varutra.webscarab.httpclient;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509KeyManager;


public class AliasKeyManager implements X509KeyManager {
    
    private KeyStore _ks;
    private String _alias;
    private String _keyPassword;
    
    public AliasKeyManager(KeyStore ks, String alias, String keyPassword) {
        _ks = ks;
        _alias = alias;
        _keyPassword = keyPassword;
    }
    
    public String chooseClientAlias(String[] str, Principal[] principal, Socket socket) {
        return _alias;
    }

    public String chooseServerAlias(String str, Principal[] principal, Socket socket) {
        return _alias;
    }

    public X509Certificate[] getCertificateChain(String alias) {
        try {
            Certificate[] certs = _ks.getCertificateChain(alias);
            if (certs == null) return null;
            X509Certificate[] x509certs = new X509Certificate[certs.length];
            for (int i=0; i<certs.length; i++) {
                x509certs[i]=(X509Certificate) certs[i];
            }
            return x509certs;
        } catch (KeyStoreException kse) {
            kse.printStackTrace();
            return null;
        }
    }

    public String[] getClientAliases(String str, Principal[] principal) {
        return new String[] { _alias };
    }

    public PrivateKey getPrivateKey(String alias) {
        try {
            return (PrivateKey) _ks.getKey(alias, _keyPassword.toCharArray());
        } catch (KeyStoreException kse) {
            kse.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException nsao) {
            nsao.printStackTrace();
            return null;
        } catch (UnrecoverableKeyException uke) {
            uke.printStackTrace();
            return null;
        }
    }

    public String[] getServerAliases(String str, Principal[] principal) {
        return new String[] { _alias };
    }
    
}
