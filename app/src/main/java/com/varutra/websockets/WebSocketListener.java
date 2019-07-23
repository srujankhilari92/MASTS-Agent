package com.varutra.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;


public class WebSocketListener implements Runnable {

	private static final Logger logger = Logger.getLogger(WebSocketListener.class.getSimpleName());

	private final InputStream in;

	private final OutputStream out;

	private final WebSocketProxy wsProxy;

	private final String name;

	private boolean isFinished = false;

	public WebSocketListener(WebSocketProxy wsProxy, InputStream in, OutputStream out, String name) {
		this.wsProxy = wsProxy;
		this.in = in;
		this.out = out;
		this.name = name;
	}

	@Override
	public void run() {
		Thread.currentThread().setName(name);
		
		try {
			if (in != null) {
				byte[] buffer = new byte[1];
				while (in.read(buffer) != -1) {
					wsProxy.processRead(in, out, buffer[0]);
				}
			}
		} catch (IOException e) {
			stop();
		} finally {				
			isFinished = true;
			
			wsProxy.shutdown();
		}
	}

	private void closeReaderStream() {
		try {
			if (in != null) {
				in.close();
			}
		} catch (IOException e) {
			logger.info(e.getMessage());
		}
	}

	/**
	 * Properly close outgoing stream.
	 */
	private void closeWriterStream() {
		try {
			if (out != null) {
				out.close();
			}
		} catch (IOException e) {
			logger.info(e.getMessage());
		}
	}

	/**
	 * Interrupts current thread, stopping its execution.
	 */
	public void stop() {
		closeReaderStream();
		
		closeWriterStream();
	}

	/**
	 * Has this listener already stopped working?
	 * 
	 * @return True if listener stopped listening.
	 */
	public boolean isFinished() {
		return isFinished;
	}
	
	/**
	 * Use this stream to send custom messages.
	 * 
	 * @return outgoing stream
	 */
	public OutputStream getOutputStream() {
		return out;
	}
}
