/** 
 * @Class Name :  LolCat.java
 */

package com.varutra.masts.proxy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author : Varutra Consulting Pvt. Ltd.
 * @Create On : Aug 11, 2014 2:17:52 PM
 * @License : Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights
 *          reserved.
 */
public class LolCat {
	private Process proc;
	private LogcatOut logcatOut;

	public LolCat(LogcatOut logcatOut) {
		this.logcatOut = logcatOut;
	}

	private InputStream inStd;

	private InputStream inErr;

	private LogcatProcessStreamReader streamReader;
	private LogcatProcessStreamReader errStreamReader;

	public void start() {
		try {
			proc = Runtime.getRuntime().exec("logcat");
			OutputStream os = proc.getOutputStream();

			this.inStd = proc.getInputStream();
			this.inErr = proc.getErrorStream();

			startReaders();

			os.flush();
		} catch (IOException e) {
			
		} catch (Exception e1) {
			
		}
	}

	private void startReaders() throws FileNotFoundException {
		this.streamReader = new LogcatProcessStreamReader(this.inStd, logcatOut);
		this.errStreamReader = new LogcatProcessStreamReader(this.inErr, null);

		streamReader.start();
		errStreamReader.start();
	}

	public void kill() {
		proc.destroy();
		if (this.streamReader != null)
			this.streamReader.finish();
		if (this.errStreamReader != null)
			this.errStreamReader.finish();
	}

	public abstract class LogcatOut {
		public abstract void writeLogData(byte[] data, int read)
				throws IOException;

		protected void cleanUp() {

		}
	}

	class LogcatProcessStreamReader extends Thread {
		private InputStream in;
		private boolean done = false;
		private LogcatOut logcatOut;

		public LogcatProcessStreamReader(InputStream in, LogcatOut logcatOut) {
			this.in = in;
			this.logcatOut = logcatOut;
		}

		@Override
		public void run() {
			byte[] b = new byte[8 * 1024];
			int read;

			try {
				while (!done && ((read = in.read(b)) != -1)) {
					if (logcatOut != null)
						logcatOut.writeLogData(b, read);
				}

				if (logcatOut != null)
					logcatOut.cleanUp();
			} catch (IOException e) {
			}
		}

		public synchronized void finish() {
			done = true;
		}
	}
}