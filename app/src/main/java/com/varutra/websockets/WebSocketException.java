package com.varutra.websockets;

import java.io.IOException;

public class WebSocketException extends IOException {
	private static final long serialVersionUID = -4708303277965511632L;

	public WebSocketException() {
		super();
	}

	public WebSocketException(String msg) {
		super(msg);
	}

	public WebSocketException(Exception e) {
		super(e);
	}

	public WebSocketException(String msg, Exception e) {
		super(msg, e);
	}
}
