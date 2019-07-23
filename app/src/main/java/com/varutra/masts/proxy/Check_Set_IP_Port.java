package com.varutra.masts.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import com.varutra.masts.proxy.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Check_Set_IP_Port extends Activity implements OnClickListener {

	private EditText IPEdit_View;
	private EditText PORTEdit_View;
	private Button done, cancel;
	private Validation valid;
	private TextView alertBoxTV;
	private boolean isInternetPresent = false;
	public ConnectionDetector cd;
	public static SharedPreferences prefs;
	private AlertDialogManager alert;
	private Vibrator vibrate;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log_twonew);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		cd = new ConnectionDetector(getApplicationContext());

		vibrate = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

		valid = new Validation();
		alert = new AlertDialogManager();
		IPEdit_View = (EditText) findViewById(R.id.ClientIP);
		PORTEdit_View = (EditText) findViewById(R.id.ClientPort);
		done = (Button) findViewById(R.id.submitBtn);
		cancel = (Button) findViewById(R.id.cancelBtn);

		if (prefs.getString("IP", "").length() != 0
				|| prefs.getString("PORT", "").length() != 0) {
			IPEdit_View.setText(prefs.getString("IP", ""));
			PORTEdit_View.setText(prefs.getString("PORT", ""));
		}
		done.setOnClickListener(this);
		cancel.setOnClickListener(this);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		Log.e("", "onRestoreInstanceState(Bundle savedInstanceState)");

		super.onRestoreInstanceState(savedInstanceState);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		Log.e("", "onSaveInstanceState(Bundle outState)");

		super.onSaveInstanceState(outState);

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onStart() {

		super.onStart();
		IPEdit_View.setText(prefs.getString("IP", ""));
		PORTEdit_View.setText(prefs.getString("PORT", ""));
		Log.e("", "onStart()");

	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		Log.e("", "onStop()");
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Log.e("", "onDestroy()");
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		
		vibrate.vibrate(50);

		if (v.getId() == R.id.submitBtn) {
			String ip = IPEdit_View.getText().toString().trim();
			String port = PORTEdit_View.getText().toString().trim();

			if (ip.length() != 0 && port.length() != 0 && ip != null
					&& port != null) {
				isInternetPresent = cd.isConnectingToInternet();
				if (isInternetPresent) {
		
					Check_Set_IP_Port.prefs.edit()
							.putString("IP", ip.toString().trim()).commit();
					Check_Set_IP_Port.prefs.edit()
							.putString("PORT", port.toString().trim()).commit();
		

					IP_PORT_Validation ip_port = new IP_PORT_Validation(this,
							ip, port);
					ip_port.execute(new String[] { "" });
				} else {
					alert.showAlertDialog(Check_Set_IP_Port.this,
							"No Internet Connection!",
							"Sorry, no internet connectivity available. Please reconnect and try again.", false);
				}

			} else {
				alertBoxTV = new TextView(getApplicationContext());
				alertBoxTV.setText(new String("\n Fields cannot be empty! Please input valid data. \n"));
				alertBoxTV.setTextSize(14);
				alertBoxTV.setGravity(Gravity.CENTER);
				alertBoxTV.setTextColor(Color.BLACK);

				AlertDialog dialog = new AlertDialog.Builder(
						Check_Set_IP_Port.this).create();
				dialog.setView(alertBoxTV);
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);

				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						});
				dialog.show();
			}

		}
		if (v.getId() == R.id.cancelBtn) {

			IPEdit_View.setText("");
			PORTEdit_View.setText("");
			finish();

		}
	}

	class IP_PORT_Validation extends AsyncTask<String, Void, String> {
		private String ip;
		private String port;
		private Context check_Set_IP_Port;
		public String result = "";
		private ProgressDialog loadingBar;

		public IP_PORT_Validation(Check_Set_IP_Port check_Set_IP_Port,
				String ip, String port) {
			// TODO Auto-generated constructor stub
			this.ip = ip;
			this.port = port;
			this.check_Set_IP_Port = check_Set_IP_Port;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			loadingBar = new ProgressDialog(this.check_Set_IP_Port);
			loadingBar.setMessage("Please wait! Validating IP address " + ip
					+ " and PORT number " + port);
			loadingBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			loadingBar.setIndeterminate(true);
			loadingBar.setCancelable(false);
			loadingBar.setCanceledOnTouchOutside(false);

			loadingBar.show();
		}

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			result = "MASS";

			if ((ip.length() != 0 && ip != null)
					&& (port.length() != 0 && port != null)) {
				if (valid.IP_Validation(ip.toString().trim())) {
					Get_SetIP.setIP(ip);

					if (valid.PORT_Validation(port.toString().trim())) {
						try {
							Socket sockets = new Socket();
							SocketAddress address = new InetSocketAddress(ip,
									Integer.parseInt(port));
							sockets.connect(address, 10000);
		
							Get_SetIP.setPORT(port);
							result = "SUCCESS";


							sockets.close();

						} catch (UnknownHostException e) {
		
							result = "WRONG ADDRESS";
						} catch (SocketTimeoutException e) {
		
							result = "TIMEOUT";
						} catch (IOException e) {
		
							result = "CLOSED"; 
						}
					} else {
						result = "PORT number is not valid!\n\nPlease set valid port number";
					}
				} else {
					result = "IP address is not valid!\n\nPlease set valid IP address";
				}
			}

			return result;

		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);


			if (result.contains("SUCCESS")) {
				for (int i = 0; i < 2000; i++)
					;
				loadingBar.dismiss();
				Toast.makeText(Check_Set_IP_Port.this, "IP and PORT is set",
						Toast.LENGTH_LONG).show();
				finish();
				startActivity(new Intent(Check_Set_IP_Port.this,
						LogKit_Authentication.class));

			} else if (result.contains("WRONG ADDRESS")) {
				for (int i = 0; i < 2000; i++)
					;

				loadingBar.dismiss();
				alertBoxTV = new TextView(getApplicationContext());
				alertBoxTV.setText(new String("\nPlease input valid IP address!"));
				alertBoxTV.setTextSize(14);
				alertBoxTV.setGravity(Gravity.CENTER);
				alertBoxTV.setTextColor(Color.BLACK);

				AlertDialog dialog = new AlertDialog.Builder(
						Check_Set_IP_Port.this).create();
				dialog.setView(alertBoxTV);
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);

				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

								dialog.cancel();
							}
						});
				dialog.show();

			} else if (result.contains("TIMEOUT")) {
				for (int i = 0; i < 2000; i++)
					;

				loadingBar.dismiss();
				alertBoxTV = new TextView(getApplicationContext());
				alertBoxTV.setText(new String("\n Please start MASTS Server Manager!"));
				alertBoxTV.setTextSize(14);
				alertBoxTV.setGravity(Gravity.CENTER);
				alertBoxTV.setTextColor(Color.BLACK);

				AlertDialog dialog = new AlertDialog.Builder(
						Check_Set_IP_Port.this).create();
				dialog.setView(alertBoxTV);
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);

				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

								dialog.cancel();
							}
						});
				dialog.show();

			} else if (result.contains("CLOSED")) {
				for (int i = 0; i < 2000; i++)
					;

				loadingBar.dismiss();
				alertBoxTV = new TextView(getApplicationContext());
				alertBoxTV.setText(new String("\nSorry! Cannot connect to MASTS Server Manager. Please restart MASTS agent."));
				alertBoxTV.setTextSize(14);
				alertBoxTV.setGravity(Gravity.CENTER);
				alertBoxTV.setTextColor(Color.BLACK);

				AlertDialog dialog = new AlertDialog.Builder(
						Check_Set_IP_Port.this).create();
				dialog.setView(alertBoxTV);
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);

				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

								dialog.cancel();
							}
						});
				dialog.show();

			} else {
				for (int i = 0; i < 2000; i++)
					;

				loadingBar.dismiss();
				alertBoxTV = new TextView(getApplicationContext());
				alertBoxTV.setText(new String("\n" + result));
				alertBoxTV.setTextSize(14);
				alertBoxTV.setGravity(Gravity.CENTER);
				alertBoxTV.setTextColor(Color.BLACK);

				AlertDialog dialog = new AlertDialog.Builder(
						Check_Set_IP_Port.this).create();
				dialog.setView(alertBoxTV);
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);

				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						});
				dialog.show();
			}

		}
	}
}