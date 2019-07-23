package com.varutra.plugin.gui;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;


import com.varutra.logger.Logger;
import com.varutra.masts.proxy.Auth;
import com.varutra.masts.proxy.Check_Set_IP_Port;
import com.varutra.masts.proxy.LogKit_Authentication;
import com.varutra.masts.proxy.Logout;
import com.varutra.masts.proxy.MainActivity;
import com.varutra.masts.proxy.MyService;
import com.varutra.masts.proxy.R;
import com.varutra.proxy.plugin.CustomPlugin;
import com.varutra.utils.network.ClientResolver;
import com.varutra.web.VarutraProxyWebService;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.StoreException;
import com.varutra.webscarab.plugin.Framework;
import com.varutra.webscarab.plugin.proxy.IClientResolver;
import com.varutra.webscarab.plugin.proxy.Proxy;
import com.varutra.webscarab.plugin.proxy.ProxyPlugin;
import com.varutra.webscarab.store.sql.SqlLiteStore;
import com.varutraproxy.utils.NetworkHostNameResolver;
import com.varutraproxy.utils.PreferenceUtils;


import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint("NewApi") public class Proxy_main_activity extends Activity {
    
    private static Framework framework = null;
    private static String TAG = Proxy_main_activity.class.getName();
    private static boolean LOGD = false;
    public SharedPreferences pref;
    private ArrayList requests = new ArrayList();
    public static String serverip="";
    public static String serverport="";
    public StringBuffer bufferrequest;
    public static boolean proxyStarted = false;
    private boolean Requeststatus = false;
    	
    private static Handler mHandlerLog = null;
    
    private static Logger mLogger;
    private static int MAX_LOG_SIZE = 20000;
    private static int MAX_MSG_SIZE = 3000;
    private static String mLogWindowMessage = "";
    
    private static boolean mInitChecked = false;
    
    NetworkHostNameResolver networkHostNameResolver = null;
    IClientResolver clientResolver = null;
    
    private static String ACTION_INSTALL = "android.credentials.INSTALL";
    private static String EXTRA_CERTIFICATE = "CERT";
   
    MyService Ms;
    public final Socket sendsocket = null;
    
    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Proxy_main_activity.class.getName());
    Button DownloadCA;
    int preport,postport;
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	try{

            super.onCreate(savedInstanceState);
            setContentView(R.layout.vproxy_activity_new);
            logger.setLevel(Level.FINEST);
            pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            serverip = pref.getString("IP","").trim();        
            serverport = pref.getString("PORT", "").trim();
            ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButtonOnOff);
            DownloadCA = (Button) findViewById(R.id.donwloadCA);
            
            Ms = new MyService();
            Log.e("Proxy Status : ", ""+proxyStarted);
            if(proxyStarted){
            	
            	toggleButton.setChecked(true);
            	
            }
            DownloadCA.setOnClickListener(new OnClickListener() {
    			
    			@Override
    			public void onClick(View v) {
    				// TODO Auto-generated method stub
    				    Intent intent = new Intent(ACTION_INSTALL);
    			        intent.setClassName("com.android.certinstaller","com.android.certinstaller.CertInstallerMain");
    			        try {
    			            String keystoreCAExportFullPath = PreferenceUtils.getCAExportFilePath(getApplicationContext());
    			            File caExportFile = new File(keystoreCAExportFullPath);
    			            byte[] result = new byte[(int) caExportFile.length()];
    			            FileInputStream in = new FileInputStream(caExportFile);
    			            in.read(result);
    			            in.close();
    			            intent.putExtra(EXTRA_CERTIFICATE, result);
    			            startActivity(intent);
    			            Log.e("Activity start ","CA activity start");
    			        }catch (Exception ex){
    			            ex.printStackTrace();
    			        }
    			}
    		});
           
            toggleButton.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                	try{

                        boolean value = ((ToggleButton)v).isChecked();
                        if (value && !proxyStarted){
                        	Log.e("Start Proxy", "First time");
                        	proxyStarted = true;
                        	
                        	preport = Integer.parseInt(serverport)-1;
                            postport = Integer.parseInt(serverport)+1;
                        	
                        	
                            Thread thread = new Thread()
                            {
                                @Override
                                public void run() {
                                    Preferences.init(getApplicationContext());
                                    if (isDeviceRooted()){
                                        ipTablesForTransparentProxy(true);
                                    }
                                    
                                    framework = new Framework(getApplicationContext());
                                    setStore(getApplicationContext());
                                    networkHostNameResolver = new NetworkHostNameResolver(getApplicationContext());
                                    clientResolver = new ClientResolver(getApplicationContext());
                                    
                                    Proxy proxy = new Proxy(framework, networkHostNameResolver, clientResolver);
                                    framework.addPlugin(proxy);
                                    if (true){
                                        ProxyPlugin plugin = new CustomPlugin();
                                        proxy.addPlugin(plugin);
                                    }
                                    proxy.run();
                                    proxyStarted = true;
                                    
                                    logger.fine("Android os proxy should point to localhost 9008");
                                }
                            };
                            thread.setName("Starting proxy");
                            thread.start();
                            
                            
                            
                        }else if (proxyStarted){
                        	proxyStarted = false;
                        	Thread thread = new Thread()
                            {
                                @Override
                                public void run() {
                                    if (isDeviceRooted()){
                                        ipTablesForTransparentProxy(false);
                                    }
                                    if (framework != null){
                                        framework.stop();
                                    }
                                    if (networkHostNameResolver != null){
                                        networkHostNameResolver.cleanUp();
                                    }
                                    networkHostNameResolver = null;
                                    framework = null;
                                    
                                    File file = PreferenceUtils.getDataStorageDir(getApplicationContext());
                                    String rootDirName = null;
                                    if (file != null){
                                        rootDirName = file.getAbsolutePath() + "/content";
                                    }
                                    File rootDir = new File(rootDirName);
                                    File[] contentFiles = null;
                                    if (file.exists()) {
                                       
                                        contentFiles = rootDir.listFiles();
                                    }
                                    SqlLiteStore database = SqlLiteStore.getInstance(getApplicationContext(), rootDirName);
                                    database.clearHttpDatabase();
                                    
                                    DeleteFilesThread deleteFilesThread = new DeleteFilesThread(contentFiles);
                                    deleteFilesThread.start();
                                    Log.e("Deleted capture Data", "Deleted");

                                    Thread cThread = new Thread(new ClientThread());
                                    cThread.start();
                                    
                                  
                                }
                            };
                            thread.setName("Stoping proxy");
                            thread.start();
                            
                        }
                    
                	}catch(Exception ex){
                		ex.printStackTrace();
                	}
                }
            });
            
            if (mHandlerLog == null){
                mHandlerLog =new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        String previousText = mLogWindowMessage;
                        String message = (String)msg.obj;
                        if (message.length() > MAX_MSG_SIZE){
                            message = message.substring(0,MAX_MSG_SIZE);
                        }
                        String newText = message;
                        int newSize = newText.length();
                        if (newSize > MAX_LOG_SIZE){
                            int size = MAX_LOG_SIZE - (MAX_LOG_SIZE / 4);
                            newText = newText.substring(0, size);
                        }
                        
                       
                    }
                };
            }
            if (mLogger == null){
                mLogger = new Logger(mHandlerLog);
            }
            
            if (!mInitChecked){
                initValues();
                mInitChecked = true;
            }
           
        
    	}catch(Exception ex){
    		ex.printStackTrace();
    	}
    }
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub

    	getMenuInflater().inflate(R.menu.v_menu, menu);
		return true;
	}
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.MASTS_service:			
            finish();
            startActivity(new Intent(Proxy_main_activity.this,
					MainActivity.class));
			break;
				default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
    @Override
	public void onBackPressed() {
    	 finish();
    	 startActivity(new Intent(Proxy_main_activity.this,
					MainActivity.class));
    	
    }
   
    public void Sendrequest(String req){
    	bufferrequest = new StringBuffer(req);
    	 try {
         	
         	InetAddress inet = InetAddress.getByName(pref.getString("IP","").trim());
     		Socket s=new Socket(inet,8090);
     		OutputStream os = s.getOutputStream();
		    PrintWriter pw = new PrintWriter(os, true);
			pw.write(bufferrequest.toString());
     		pw.close();
     		s.close();
     		bufferrequest.setLength(0);
			    Log.e("Request Send", "Suceesdds");} catch (Exception e) {
			    	e.printStackTrace();
         }
    }
     public class ClientThread implements Runnable {

        public void run() {
        	
        	Log.e("REQUEST THREAD CALL	", "OK");
            try {
            	
            	InetAddress inet = InetAddress.getByName(pref.getString("IP","").trim());
        		Socket s=new Socket(inet,45000);
        		OutputStream os = s.getOutputStream();
				PrintWriter pw = new PrintWriter(os, true);
				pw.write("Listner Stop");
        		pw.close();
        		s.close();
        		//bufferrequest.setLength(0);
        		Log.e("Stop Listener", "When Proxy Stop");
        		} catch (Exception e) {
			    	e.printStackTrace();
            }
        }
    } 
    
    
    public class DeleteFilesThread extends Thread{
        private File[] filesToDelete;
        public DeleteFilesThread(File[] files){
            filesToDelete = files;
        }
        
        public void run() {
            if (filesToDelete != null){
                for (File contentFile : filesToDelete) {
                    try{
                        contentFile.delete();
                        logger.finest("File deleted: " + contentFile.getAbsolutePath());
                    }catch (Exception ex){
                        Log.e(TAG, ex.getMessage());
                    }
                }
            }
        }
    }
    
    public static void setStore(Context context){
        if (framework != null){
            try {
                File file =  PreferenceUtils.getDataStorageDir(context);
                if (file != null){
                    File rootDir = new File(file.getAbsolutePath() + "/content");
                    if (!rootDir.exists()){
                        rootDir.mkdir();
                    }
                    framework.setSession("Database", SqlLiteStore.getInstance(context, rootDir.getAbsolutePath()), "");
                }
            } catch (StoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    /*
     *  this will work only on sdk 14 or higher
     */
    public static void exportCACertToUserStore(Context context){
        
    	
    	Intent intent = new Intent(ACTION_INSTALL);
        intent.setClassName("com.android.certinstaller","com.android.certinstaller.CertInstallerMain");
        try {
            String keystoreCAExportFullPath = PreferenceUtils.getCAExportFilePath(context.getApplicationContext());
            File caExportFile = new File(keystoreCAExportFullPath);
            byte[] result = new byte[(int) caExportFile.length()];
            FileInputStream in = new FileInputStream(caExportFile);
            in.read(result);
            in.close();
            intent.putExtra(EXTRA_CERTIFICATE, result);
            context.startActivity(intent);
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    
    
    /**
     * Checks if the device is rooted.
     * 
     * @return <code>true</code> if the device is rooted, <code>false</code>
     * otherwise.
     */
    public static boolean isDeviceRooted() {
      
      boolean trueReturn = true;
      String buildTags = android.os.Build.TAGS;
      if (buildTags != null && buildTags.contains("test-keys")) {
          return trueReturn;
      }
      try {
       
        {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
              return trueReturn;
            }
        }
       
        {
            String[] suPlaces = { "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                    "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/" };
            for (String suPlace : suPlaces) {
                File file = new File(suPlace + "su");
                if (file.exists()) {
                    return trueReturn;
                }
            }
        }
      } catch (Throwable e1) {
       
      }
      return false;
    }
    
    
    private void ipTablesForTransparentProxy(boolean activate){
        int processId = getApplicationInfo().uid;
        String excludedUid = String.valueOf(processId);
        String action = "A";
        String chainName = "spplugin";
        String chainName1 = "sppluginOutput";
        List<String> rules = new ArrayList<String>();

        if (activate){
            action = "A";
            String createChainRule = "iptables --new " + chainName; rules.add(createChainRule);
            String createNatChainRule = "iptables -t nat --new " + chainName; rules.add(createNatChainRule);
            String createNatChainRule1 = "iptables -t nat --new " + chainName1; rules.add(createNatChainRule1);
        }else{
            action = "D";
            String dettachChainRule = "iptables -D INPUT -j " + chainName; rules.add(dettachChainRule);
            String dettachNatChainRule = "iptables -t nat -D PREROUTING -j " + chainName; rules.add(dettachNatChainRule);
            String dettachNatChainRule1 = "iptables -t nat -D OUTPUT -j " + chainName1; rules.add(dettachNatChainRule1);
        }
        
        String accept80Rule = "iptables -" + action + " " + chainName + " -p tcp --dport 0:79 -j ACCEPT "; rules.add(accept80Rule);
        String redirect80Rule = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 0:79 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule);
        String exclude80UidRule = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 0:79 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule);
        
        String accept80Rule1 = "iptables -" + action + " " + chainName + " -p tcp --dport 80 -j ACCEPT "; rules.add(accept80Rule1);
        String redirect80Rule1 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 80 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule1);
        String exclude80UidRule1 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 80 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule1);
        
        String accept80Rule2 = "iptables -" + action + " " + chainName + " -p tcp --dport 81:442 -j ACCEPT "; rules.add(accept80Rule2);
        String redirect80Rule2 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 81:442 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule2);
        String exclude80UidRule2 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 81:442 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule2);
        
        String accept443Rule3 = "iptables -" + action + " " + chainName + " -p tcp --dport 443 -j ACCEPT ";rules.add(accept443Rule3);
        String redirect443Rule3 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 443 -j REDIRECT --to-port 8010 ";rules.add(redirect443Rule3);
        String exclude443UidRule3 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 443 -j DNAT --to 127.0.0.1:8010 ";rules.add(exclude443UidRule3);
        
        String accept80Rule4 = "iptables -" + action + " " + chainName + " -p tcp --dport 444:45000 -j ACCEPT "; rules.add(accept80Rule4);
        String redirect80Rule4 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 444:45000 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule4);
        String exclude80UidRule4 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 444:45000 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule4);
        
       
        
        
        if (activate){
            String attachChainRule = "iptables -A INPUT -j " + chainName; rules.add(attachChainRule);
            String attachNatChainRule = "iptables -t nat -A PREROUTING -j " + chainName; rules.add(attachNatChainRule);
            String attachNatChainRule1 = "iptables -t nat -A OUTPUT -j " + chainName1; rules.add(attachNatChainRule1);
        }else{
            
            String deleteChainRule = "iptables --delete-chain " + chainName; rules.add(deleteChainRule);
            String deleteNatChainRule = "iptables -t nat --delete-chain " + chainName; rules.add(deleteNatChainRule);
            String deleteNatChainRule1 = "iptables -t nat --delete-chain " + chainName1; rules.add(deleteNatChainRule1);
        }
        Process p;
        try {
            p = Runtime.getRuntime().exec(new String[]{"su", "-c", "sh"});
        
            DataOutputStream stdin = new DataOutputStream(p.getOutputStream());
            DataInputStream stdout = new DataInputStream(p.getInputStream());
            InputStream stderr = p.getErrorStream();
            
            for (String rule : rules) {
                logger.finest(rule);
                stdin.writeBytes(rule + "\n");
                stdin.writeBytes("echo $?\n");
                Thread.sleep(100);
                byte[] buffer = new byte[4096];
                int read = 0;
                String out = new String();
                String err = new String();
                while(true){
                    read = stdout.read(buffer);
                    out += new String(buffer, 0, read);
                    if(read<4096){
                        break;
                    }
                }
                while(stderr.available() > 0){
                    read = stderr.read(buffer);
                    err += new String(buffer, 0, read);
                    if(read < 4096){
                        break;
                    }
                }
                if (out != null && out.trim().length() > 0) logger.finest(out);
                if (err != null && err.trim().length() > 0) logger.finest(err);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.finest("Error executing rules: " + e.getMessage());
        }
    }
    
    /*
     * TODO this should be handled with preference settings activity
     */
    private void initValues(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        
       
        String dirName = pref.getString(PreferenceUtils.dataStorageKey, null);
        if (dirName == null){
            File dataDir = getExternalCacheDir();
            if (PreferenceUtils.IsDirWritable(dataDir)){
                pref.edit().putString(PreferenceUtils.dataStorageKey, dataDir.getAbsolutePath()).commit();
            }else{
                Toast.makeText(this, R.string.data_storage_missing, Toast.LENGTH_LONG).show();
            }
        }else{
            File dataStorage = new File(dirName);
            if (!PreferenceUtils.IsDirWritable(dataStorage)){
                Toast.makeText(this, R.string.data_storage_missing, Toast.LENGTH_LONG).show();
            }
        }
        
       
        String port = pref.getString(PreferenceUtils.proxyPort, null);
        
        if (port == null){
            pref.edit().putString(PreferenceUtils.proxyPort, "9008").commit();
        }
        
        boolean listenNonLocal = pref.getBoolean(PreferenceUtils.proxyListenNonLocal, false);
        if (!listenNonLocal){
            pref.edit().putBoolean(PreferenceUtils.proxyListenNonLocal, true).commit();
        }
        
        
        boolean transparentProxy = pref.getBoolean(PreferenceUtils.proxyTransparentKey, false);
        if (!transparentProxy){
            pref.edit().putBoolean(PreferenceUtils.proxyTransparentKey, true).commit();
        }
        
       
        boolean proxyCaptureData = pref.getBoolean(PreferenceUtils.proxyCaptureData, false);
        if (!proxyCaptureData){
            pref.edit().putBoolean(PreferenceUtils.proxyCaptureData, true).commit();
        }
    }
}