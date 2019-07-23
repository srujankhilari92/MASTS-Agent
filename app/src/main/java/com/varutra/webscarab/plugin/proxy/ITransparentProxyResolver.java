package com.varutra.webscarab.plugin.proxy;

import java.net.Socket;

public interface ITransparentProxyResolver {
    SiteData getSecureHost(Socket socket, int _destPort, boolean getCertificateData);
}
