package com.varutra.websockets;

import java.sql.SQLException;
import java.util.logging.Logger;


import com.varutra.webscarab.store.sql.SqlLiteStore;
import com.varutra.websockets.WebSocketProxy.State;

public class WebSocketStorage implements WebSocketObserver {

	private static final Logger logger = Logger
			.getLogger(WebSocketStorage.class.getSimpleName());

	public static final int WEBSOCKET_OBSERVING_ORDER = 100;

	private SqlLiteStore store;

	public WebSocketStorage(SqlLiteStore store) {
		this.store = store;
	}
	
	public boolean insertMessage(WebSocketMessageDTO message){
	    try {
            store.insertMessage(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info(e.getMessage());
        }
	    return false;
	}

	@Override
	public int getObservingOrder() {
		return WEBSOCKET_OBSERVING_ORDER;
	}

	@Override
	public boolean onMessageFrame(long channelId, WebSocketMessage wsMessage) {
		if (wsMessage.isFinished()) {
			WebSocketMessageDTO message = wsMessage.getDTO();

			try {
			    store.insertMessage(message);
			} catch (Exception e) {
			    e.printStackTrace();
				logger.info(e.getMessage());
			}
		}

		return true;
	}

	@Override
	public void onStateChange(State state, WebSocketProxy proxy) {
		if (state.equals(State.OPEN) || state.equals(State.CLOSED) || state.equals(State.INCLUDED)) {
			try {
				if (store != null) {
				    store.insertOrUpdateChannel(proxy.getDTO());
				} else if (!state.equals(State.CLOSED)) {
					logger.info("Could not update state of WebSocket channel to '" + state.toString() + "'!");
				}
			} catch (SQLException e) {
				logger.info(e.getMessage());
			}
		} else if (state.equals(State.EXCLUDED)) {
            try {
                store.purgeChannel(proxy.getChannelId());
			} catch (SQLException e) {
				logger.info(e.getMessage());
			}
		}
	}
}
