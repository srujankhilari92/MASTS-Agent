package com.varutra.webscarab.plugin.proxy;

import java.net.Socket;

import com.varutra.webscarab.model.ConnectionDescriptor;

public interface IClientResolver {
    ConnectionDescriptor getClientDescriptorBySocket(Socket socket);
}
