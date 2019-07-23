package com.varutra.masts.proxy;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import com.varutra.masts.proxy.Logout.Logout_session_stop;
import com.varutra.masts.proxy.R.color;
import com.varutra.plugin.gui.Proxy_main_activity;
import com.varutra.masts.proxy.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	public boolean wakeFlags = false;
	private TextView alertBoxTV;
	public static MainActivity mainActivity;
	public SharedPreferences prefs;
	Proxy_main_activity pro;
	private int progressStatus = 0;
	AlertDialogManager alert = new AlertDialogManager();
	SessionManager session;
	private WakeLock wakeLock;
	private PowerManager mgr;
	private RelativeLayout Four;
	private SharedPreferences settings;
	private boolean auth;
	public Button start, stop, proxy;
	private int transitionTime = 1000;
	private TransitionDrawable transition;
	private SimpleDateFormat sdf;
	private String currentDateandTime;
	static public ArrayList<String> planetList;
	static public ListView status_list;
	static public ArrayAdapter<String> listAdapter;

	private Spinner spinner;

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.e("In", "onCreate(Bundle savedInstanceState)");
		setContentView(R.layout.mass_status_two);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mainActivity = this;
		// Three = (RelativeLayout) findViewById(R.id.bottom_layout2);
		status_list = (ListView) findViewById(R.id.status_list);

		Four = (RelativeLayout) findViewById(R.id.status_progress_layout);
		transition = (TransitionDrawable) status_list.getBackground();

		start = (Button) findViewById(R.id.start_service);
		
		pro = new Proxy_main_activity();
		planetList = new ArrayList<String>();

		planetList.add("WELCOME TO MASTS");
		int mastsagentid = android.os.Process.myPid();
        Log.e("MASTS Agent MainActivity process id:", "id-"+mastsagentid);
		listAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_activated_1, planetList);

		status_list.setAdapter(listAdapter);

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		auth = settings.getBoolean("isEnabled", false);

		if (isMyServiceRunning(this.getApplicationContext()))
			transition.startTransition(transitionTime);
		else
			transition.reverseTransition(transitionTime);

	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		auth = settings.getBoolean("isEnabled", false);

		if (isMyServiceRunning(this.getApplicationContext()))
			transition.startTransition(transitionTime);
		else
			transition.reverseTransition(transitionTime);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onRestoreInstanceState(savedInstanceState);
		Log.e("In", "onRestoreInstanceState(Bundle savedInstanceState)");
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		auth = settings.getBoolean("isEnabled", false);
		if (isMyServiceRunning(this.getApplicationContext()))
			transition.startTransition(transitionTime);
		else
			transition.reverseTransition(transitionTime);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		auth = settings.getBoolean("isEnabled", false);

		if (isMyServiceRunning(this.getApplicationContext()))
			transition.startTransition(transitionTime);
		else
			transition.reverseTransition(transitionTime);

	}

	public void onClickVProxy(View V) {

		if (!isMyServiceRunning(this)) {

			alertBoxTV = new TextView(this.getApplicationContext());
			alertBoxTV.setText(new String("\n Warning! Please Start service.\n"));
			alertBoxTV.setTextSize(14);
			alertBoxTV.setGravity(Gravity.CENTER);
			alertBoxTV.setTextColor(Color.BLACK);

			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setView(alertBoxTV);
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);

			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

							dialog.cancel();
						}
					});
			dialog.show();

		} else {

			finish();
			startActivity(new Intent(this, Proxy_main_activity.class));


		}

	}

	@SuppressWarnings("deprecation")
	public void onClickStartServie(View V) {

		boolean wakeFlg = prefs.getBoolean("WAKEFLAG", false);
		try {
			if (!isMyServiceRunning(this.getApplicationContext())) {
				MainActivity.planetList.add(0, "SERVICE STARTED");
				MainActivity.listAdapter.notifyDataSetChanged();
				
				start.setBackgroundDrawable(getResources().getDrawable(R.drawable.custom_btn_breaker_bay));
				startService(new Intent(this.getApplicationContext(),
						MyService.class));
				if (isMyServiceRunning(this.getApplicationContext()))
					transition.startTransition(transitionTime);
				else
					transition.reverseTransition(transitionTime);
			} else {
				Toast.makeText(this, "Alert! Service is already running.",
						Toast.LENGTH_SHORT).show();

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	public void onClickStopService(View V) {

		boolean wakeFlg = prefs.getBoolean("WAKEFLAG", true);

		try {
			if (isMyServiceRunning(this.getApplicationContext())) {
				Proxy_main_activity pro = new Proxy_main_activity();

				if (pro.proxyStarted) {

					alertBoxTV = new TextView(this.getApplicationContext());
					alertBoxTV.setText(new String(
							"\n Warning! Please Stop VProxy.\n"));
					alertBoxTV.setTextSize(14);
					alertBoxTV.setGravity(Gravity.CENTER);
					alertBoxTV.setTextColor(Color.BLACK);

					AlertDialog dialog = new AlertDialog.Builder(this).create();
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

					MainActivity.planetList.add(0, "SERVICE STOPPED");
					MainActivity.listAdapter.notifyDataSetChanged();
					
					start.setBackgroundDrawable(getResources().getDrawable(R.drawable.custom_btn_arsenic));

					stopService(new Intent(this.getApplicationContext(),
							MyService.class));
					if (isMyServiceRunning(this.getApplicationContext()))
						transition.startTransition(transitionTime);
					else
						transition.reverseTransition(transitionTime);

				}
			} else {
				Toast.makeText(this, "Alert! Service already Stopped.",
						Toast.LENGTH_SHORT).show();
			}
		} catch (IllegalStateException ex) {

		}
	}

	private boolean isMyServiceRunning(Context mContext) {
		ActivityManager manager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (com.varutra.masts.proxy.MyService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	public void onStartProxyingService(View V) {
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.action_Hide: {
			alertBoxTV = new TextView(this);
			alertBoxTV
					.setText(new String(
							"\n Login Session keeps you active in background while application will be minimized.\n"));
			alertBoxTV.setTextSize(14);
			alertBoxTV.setGravity(Gravity.CENTER);
			alertBoxTV.setTextColor(Color.BLACK);
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setView(alertBoxTV);
			dialog.setCancelable(false);
			dialog.setCanceledOnTouchOutside(false);

			dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Hide",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							moveTaskToBack(true);
							dialog.cancel();
						}
					});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
			dialog.show();

			break;
		}
		case R.id.action_Logout:

			if (!isMyServiceRunning(this) && pro.proxyStarted == false) {

				alertBoxTV = new TextView(this.getApplicationContext());
				alertBoxTV.setText(new String("\n Are you sure you want to log out?\n"));
				alertBoxTV.setTextSize(14);
				alertBoxTV.setTextColor(Color.BLACK);
				alertBoxTV.setGravity(Gravity.CENTER);
				alertBoxTV.setTextColor(Color.BLACK);
				AlertDialog dialog = new AlertDialog.Builder(this).create();
				dialog.setView(alertBoxTV);
				dialog.setCancelable(false);
				dialog.setCanceledOnTouchOutside(false);
				dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								dialog.cancel();
							}
						});
				dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

								startActivity(new Intent(MainActivity.this,
										Logout.class));
								dialog.cancel();
							}
						});
				dialog.show();

			} else {

				alertBoxTV = new TextView(this.getApplicationContext());
				alertBoxTV.setText(new String(
						"\n Warning! Please stop MASTS Service and VProxy prior logging out.\n"));
				alertBoxTV.setTextSize(14);
				alertBoxTV.setGravity(Gravity.CENTER);
				alertBoxTV.setTextColor(Color.BLACK);

				AlertDialog dialog = new AlertDialog.Builder(this).create();
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
			break;
		case R.id.about_us:
			finish();
			startActivity(new Intent(MainActivity.this,
					AboutUs.class));
			
			
			break;
			
		case R.id.action_privacypolicy:
			finish();
			startActivity(new Intent(MainActivity.this,
					PrivacyPolicy.class));
			
			
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public void showNotificationDialog() {
		System.out.println("In showNotificationDialog()");
		alertBoxTV = new TextView(this);
		alertBoxTV.setText(new String(
				"\nWarning! Session expired.\n\nPlease login again."));
		alertBoxTV.setTextColor(Color.BLACK);
		alertBoxTV.setTextSize(14);
		alertBoxTV.setGravity(Gravity.CENTER);
		alertBoxTV.setTextColor(Color.BLACK);
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setView(alertBoxTV);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);

		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(MainActivity.this,
								Logout.class));
						dialog.cancel();
					}
				});

		if (!dialog.isShowing())
			dialog.show();
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub

		alertBoxTV = new TextView(this.getApplicationContext());
		alertBoxTV
				.setText(new String(
						"\n Login Session keeps you active in background while application will be minimized.\n"));
		alertBoxTV.setTextSize(14);
		alertBoxTV.setTextColor(Color.BLACK);
		alertBoxTV.setGravity(Gravity.CENTER);
		alertBoxTV.setTextColor(Color.BLACK);
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setView(alertBoxTV);
		dialog.setCancelable(false);
		dialog.setCanceledOnTouchOutside(false);

		dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Hide",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						moveTaskToBack(true);
						dialog.cancel();
					}
				});
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		dialog.show();

	}

}