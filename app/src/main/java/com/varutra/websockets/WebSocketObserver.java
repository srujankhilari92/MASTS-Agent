package com.varutra.websockets;

import com.varutra.websockets.WebSocketProxy.State;


public interface WebSocketObserver {

	int getObservingOrder();
	
	boolean onMessageFrame(long channelId, WebSocketMessage message);
	
	void onStateChange(State state, WebSocketProxy proxy);
}
