package com.varutra.masts.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class SnifferStartStop {

	private Context context;
	private String comando;
	private Process suProcess;
	private DataOutputStream os;
	private boolean retval;
	private Process process2;
	private SimpleDateFormat sdf;
	private String currentDateandTime;
	private static String currentPack;
	private File file;
	private Thread readThread;
	private DataInputStream datainputstream;
	private Process suProcessStop;
	private SharedPreferences prefs;

	private static int pid;

	public SnifferStartStop(Context context2) {
		// TODO Auto-generated constructor stub

		this.context = context2;
		prefs = PreferenceManager.getDefaultSharedPreferences(this.context);

	}

	@SuppressLint({ "NewApi", "SdCardPath", "SimpleDateFormat" })
	@SuppressWarnings({ "deprecation" })
	void TCPDumpChecking() {
		BufferedReader bufferedreader;

		String s;
		try {
			/**
			 * Get Current Date and Time Format.
			 */
			sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
			currentDateandTime = sdf.format(new Date());
			currentPack = "output_" + currentDateandTime + ".pcap";
			prefs.edit().putString("pcapFile", currentPack).commit();

			comando = "/data/local/tcpdump-arm -s 0 -w /data/local/output_"
					+ currentDateandTime + ".pcap\n";
			suProcess = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(suProcess.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			bufferedreader = new BufferedReader(new InputStreamReader(
					suProcess.getErrorStream()));

			s = bufferedreader.readLine();
			if (s == null) {
				try {
					bufferedreader.close();

				} catch (IOException ioexception) {
					ioexception.printStackTrace();
				}

			}


			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			process2 = Runtime.getRuntime().exec("ps tcpdump-arm");
			datainputstream = new DataInputStream(process2.getInputStream());

			String temp1 = datainputstream.readLine();
			Log.e("readLine()", "" + temp1);

			temp1 = datainputstream.readLine();


			temp1 = temp1.replaceAll("^root *([0-9]*).*", "$1");


			pid = Integer.parseInt(temp1);
			prefs.edit().putString("processPID", pid + "".trim()).commit();
			process2.destroy();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void ForceKill(String proc) {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException interruptedexception) {
			interruptedexception.printStackTrace();
		}

		try {

			comando = "kill -KILL " + proc;

			suProcessStop = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			os = new DataOutputStream(suProcessStop.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			BufferedReader bufferedreader = new BufferedReader(
					new InputStreamReader(suProcessStop.getErrorStream()));

			String line = bufferedreader.readLine();
			if (line == null) {
				try {
					bufferedreader.close();

				} catch (IOException ioexception) {
					ioexception.printStackTrace();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	String getProcessId() {
		try {

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			Process process1 = Runtime.getRuntime().exec("ps tcpdump-arm");
			datainputstream = new DataInputStream(process1.getInputStream());

			String temp = datainputstream.readLine();
			Log.e("readLine()", "" + temp);

			temp = datainputstream.readLine();
			temp = temp.replaceAll("^root *([0-9]*).*", "$1");

			if (temp == null)
				return null;

			Log.e("readLine() 2", "" + temp);

			while (temp != null) {
				temp = datainputstream.readLine();
				temp = temp.replaceAll("^root *([0-9]*).*", "$1");
				ForceKill(temp);
				if (temp.equals(prefs.getString("processPID", ""))) {
					return temp;
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return prefs.getString("processPID", "");

	}

	@SuppressLint("ShowToast")
	void TCPDumpStop(Socket socket) {
		try {
			BufferedReader bufferedreader;
			Process suProcessStop;
			String s;
			String processID = prefs.getString("processPID", "");
			if (!processID.isEmpty()) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException interruptedexception) {
					interruptedexception.printStackTrace();
				}


				comando = "kill " + processID;

				suProcessStop = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(suProcessStop.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				bufferedreader = new BufferedReader(new InputStreamReader(
						suProcessStop.getErrorStream()));

				s = bufferedreader.readLine();
				if (s == null) {
					try {
						bufferedreader.close();

					} catch (IOException ioexception) {
						ioexception.printStackTrace();
					}

				} else {
					System.out.println("Log Error: " + s);
				}

				if (getProcessId() == null) {
					Send_Result_Message_TO_Client(socket,
							"Process Stop Successfully");
				} else {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException interruptedexception) {
						interruptedexception.printStackTrace();
					}
					comando = "kill -KILL " + prefs.getString("processPID", "");

					suProcessStop = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					os = new DataOutputStream(suProcessStop.getOutputStream());
					os.writeBytes(comando + "\n");
					os.flush();
					os.writeBytes("exit\n");
					os.flush();
					os.close();

					if (getProcessId() == null) {
						Send_Result_Message_TO_Client(socket,
								"Process Stop Successfully");
					} else {
						Send_Result_Message_TO_Client(
								socket,
								"Error in Killing Process : "
										+ prefs.getString("processPID", ""));

					}
				}
			} else {
				Send_Result_Message_TO_Client(socket,
						"Sniffer has no Process Running");
			}
		} catch (Exception ex) {

			ex.printStackTrace();
		}

	}

	void TcpExport(Socket socket) {

		String fileName = prefs.getString("pcapFile", "").trim();
		if (!fileName.isEmpty()) {
			try {
				suProcess = Runtime.getRuntime().exec("su");

				comando = ("chmod 777 /data/local/" + fileName);
				os = new DataOutputStream(suProcess.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				try {
					if (255 == suProcess.waitFor()) {
						retval = true;
					} else {
						retval = false;
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				file = new File("/data/local/" + fileName);

				int count;
				byte[] buffer = new byte[1024];

				socket.setSendBufferSize(10000);
				socket.setTcpNoDelay(true);
				OutputStream out = socket.getOutputStream();
				BufferedInputStream in = new BufferedInputStream(
						new FileInputStream(file));
				while ((count = in.read(buffer)) >= 0) {
					out.write(buffer, 0, count);
					out.flush();
				}
				socket.close();
				Log.e("------------->", "File Send Complet");

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Send_Result_Message_TO_Client(socket, " .pcap file not available");
		}
	}

	void readTCPPacket() {
		try {
			suProcess = Runtime.getRuntime().exec("su");

			comando = ("chmod 777 /data/local/" + currentPack);
			os = new DataOutputStream(suProcess.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			try {
				if (255 == suProcess.waitFor()) {
					retval = true;
					Log.e("Retrival ", "" + retval);
				} else {
					retval = false;
					Log.e("Retrival ", "" + retval);

				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		// Init the reader thread
		readThread = new Thread() {
			private BufferedReader reader;

			public void run() {
				try {
					// ensure the file we will read exists
					boolean fileOK = false;
					while (!fileOK) {
						file = new File("/data/local/" + currentPack);

						if (file.exists())
							fileOK = true;
					}

					reader = new BufferedReader(new FileReader(file));
					String temp = new String();
					while (!Thread.interrupted()) {
						temp = reader.readLine();
						if (temp != null) {
							Log.e("READER", new String(temp));

						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		readThread.start();
	}

	void Send_Result_Message_TO_Client(Socket socket, String msg) {
		/**
		 * If Invalid Command Received from Client then Error Message the
		 * response back to the client.
		 */
		try {
			socket.setSendBufferSize(10000);
			socket.setTcpNoDelay(true);

			OutputStream os = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(Auth.getTokn() + "#" + msg);
			os.flush();
			bw.flush();
			socket.close();
			Log.e("Result : ", "Send Complet");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

}