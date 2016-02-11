package ccc.chess.engine.stockfish;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
//import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
//import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.util.Log;

public class StartChessEngine extends Activity implements OnTouchListener
{	//>110
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        enginePrefs = getSharedPreferences("engine", 0);
        setContentView(R.layout.main);
        getEnginePrefs();
        if (enginePrefs.getBoolean("clearData", true))
        	clearApplicationData();
        btnInfo = (ImageView) findViewById(R.id.btnInfo); 
        btnOptions = (ImageView) findViewById(R.id.btnOptions);
        btnGui = (ImageView) findViewById(R.id.btnGui);
        btnMenu = (ImageView) findViewById(R.id.btnMenu); 
        
        registerForContextMenu(btnMenu);
        btnMenu.setOnTouchListener((OnTouchListener) this);
        tvMain = (TextView) findViewById(R.id.tvMain);
        aboutApp();
        initEngineService();
    }
    @Override
    protected void onDestroy() 						// Program-Exit						(onDestroy)
    {
   		releaseEngineService(true);
    	super.onDestroy();
    }
//	MENU		MENU		MENU		MENU		MENU		MENU		MENU
    @Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{	
	    if (keyCode == KeyEvent.KEYCODE_MENU) 
	    	openContextMenu(btnMenu);
	    return super.onKeyUp(keyCode, event);
	}
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
    {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_about, menu);
	    menu.setHeaderTitle(getString(R.string.menu_title));
    }
    @Override  
    public boolean onContextItemSelected(MenuItem item)
    {  
    	super.onContextItemSelected(item);
        switch (item.getItemId()) 
        { 
	        case R.id.menu_whatsNew: 
	        	showDialog(WHATS_NEW);
	            return true;
	        case R.id.menu_about: 
	        	showDialog(ABOUT_DIALOG);
	            return true;  
	        case R.id.menu_homepage: 
	        	Intent irw = new Intent(Intent.ACTION_VIEW);
                irw.setData(Uri.parse("http://c4akarl.blogspot.com/"));
    			startActivityForResult(irw, HOMEPAGE_REQUEST_CODE);
	            return true;
            case R.id.menu_sourceCode:
                Intent ihp = new Intent(Intent.ACTION_VIEW);
                ihp.setData(Uri.parse("https://github.com/c4akarl/StockfishChess"));
                startActivityForResult(ihp, SOURCECODE_REQUEST_CODE);
                return true;
	        case R.id.menu_contact: 
	        	Intent send = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", APP_EMAIL, null)); 
	        	send.putExtra(android.content.Intent.EXTRA_SUBJECT, ""); 
	        	send.putExtra(android.content.Intent.EXTRA_TEXT, ""); 
	        	startActivity(Intent.createChooser(send, getString(R.string.sendEmail)));
	            return true;
         }   
        return false; //should never happen  
    }
    public void myClickHandler(View view) 			// ClickHandler 					(ButtonEvents)
    {
    	Intent i;
		switch (view.getId())
		{
		case R.id.btnInfo:
			if (!isAboutApp)
			{
				isAboutApp = true;
				aboutApp();
			}
			else
			{
//				if (isReady)
//					readUCIOptions();
//				else
//					aboutApp();
				readUCIOptions();
				isAboutApp = false;
			}
			break;
		case R.id.btnOptions:
			i = new Intent(StartChessEngine.this, ChessEnginePreferences.class);
			startActivityForResult(i, PREFERENCES_REQUEST_CODE);
			break;
		case R.id.btnGui:
			releaseEngineService(false);
			if (this.getCallingPackage() == null)
			{
				boolean startMarket = false;
				try
				{
					PackageManager pm = this.getPackageManager();
				    i = pm.getLaunchIntentForPackage(GUI_C4A);
				    if (i != null)
				        startActivity(i);
				    else
				    	startMarket = true;
				}
				catch (ActivityNotFoundException e)	{startMarket = true;}
				if (startMarket)
				{
					try
					{
						i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse("market://details?id=" + GUI_C4A));
						startActivity(i);
					}
					catch (ActivityNotFoundException e) {}
				}
			}
			finish();
			break;
		}
    }
    public boolean onTouch(View view, MotionEvent event)
	{	// Touch Listener
    	if (view.getId() == R.id.btnMenu & event.getAction() == MotionEvent.ACTION_UP)	
     		openContextMenu(btnMenu);
      	return true;
	}
    protected void onActivityResult(int requestCode, int resultCode, Intent data)			// SubActivityResult
    {
 	   getEnginePrefs();
	}
    private void getEnginePrefs() 
    {
//    	enginePrefs = getSharedPreferences("engine", 0);
    	if (enginePrefs.getBoolean("logOn", false))
    		getInfoFromEngineService("SET_LOGFILE_ON");
    	else
    		getInfoFromEngineService("SET_LOGFILE_OFF");
    }
    @Override
	protected Dialog onCreateDialog(int id) 
    {
		switch (id) 
		{
			case NO_CHESS_ENGINE_DIALOG: 
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				String notFound = "";
				if (!currentProcess.equals(""))
					notFound = getString(R.string.engineAlreadyRunning) + " " + currentProcess;
				else
					notFound = getString(R.string.engineNotFound) + ENGINE_STOCKFISH;
				builder.setTitle(R.string.app_name_uci).setMessage(notFound);
				builder.setOnCancelListener(new OnCancelListener() 
				{
			        public void onCancel(DialogInterface dialog) 
			        {
			        	finish();
				    }
				});
				AlertDialog alert = builder.create(); 
				return alert;
			}
			case WHATS_NEW: 
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.whatsNew).setMessage(R.string.whatsNew_text);
				AlertDialog alert = builder.create(); 
				return alert;
			}
			case ABOUT_DIALOG: 
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.menu_about).setMessage(R.string.menu_about_text);
				AlertDialog alert = builder.create(); 
				return alert;
			}
			case CONTACT_DIALOG: 
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.menu_contact).setMessage(R.string.menu_contact_text);
				AlertDialog alert = builder.create(); 
				return alert;
			}
		}
		return null;
	}

    // Binds this activity to the service. 
    private void initEngineService() 
    {	//>133
    	engineServiceConnection = new EngineServiceConnection();
    	engineIntent = new Intent(ENGINE_STOCKFISH);
		engineIntent.setPackage("ccc.chess.engine.stockfish");
		startService(engineIntent);		// starting once for visibility on device(running services)
    	bindService(engineIntent, engineServiceConnection, Context.BIND_AUTO_CREATE);
    }
    private void startEngine() 
    {	//>134
    	startProcess();
//    	Log.i(TAG, "startEngine, processAlive: " + processAlive);
    	if (processAlive)
    	{
//	    	writeLineToProcess("isready");
//	    	handlerEngineIsreadyCnt = 0;
//	    	handlerEngineIsready.removeCallbacks(mUpdateEngineIsready);
//	    	handlerEngineIsready.postDelayed(mUpdateEngineIsready, 100);
    	}
    	else
    	{
    		currentProcess = getInfoFromEngineService("CURRENT_PROCESS");
    		showDialog(NO_CHESS_ENGINE_DIALOG);
    	}
    }
    public Runnable mUpdateEngineIsready = new Runnable() 		
	{	
	   public void run() 
	   {
		   	String s = readLineFromProcess(1000);
			if (s == null)
				s = "";
    		if (s.equals("readyok") | s.endsWith("readyok"))
    		{
//	    		Log.i(TAG, "mUpdateEngineIsready, readyok");
    			isReady = true;
    			handlerEngineIsready.removeCallbacks(mUpdateEngineIsready);
    		}
    		else
    		{
    			handlerEngineIsreadyCnt++;
    			if (handlerEngineIsreadyCnt < 100)
	    			handlerEngineIsready.postDelayed(mUpdateEngineIsready, 30);
    			else
    			{
    				releaseEngineService(false);
    				handlerEngineIsready.removeCallbacks(mUpdateEngineIsready);
    			}
    		}
	   }
	};
	public Runnable mUpdateEngineUciOptions = new Runnable() 		
	{	
		public void run() 
		{
			String s = readLineFromProcess(1000);
			if (s == null)
				s = "";
    		if (s.equals("uciok") | s.endsWith("uciok"))
    		{
    			tvMain.setText(infoText);
    			handlerEngineUciOptions.removeCallbacks(mUpdateEngineUciOptions);
    		}
    		else 
    		{
    			handlerEngineUciOptionsCnt++;
				if (handlerEngineUciOptionsCnt < 100)
				{
					if (s.length() > 0)
					{
	    				infoText.append(s);
	    				infoText.append("\n\n");
					}
	    			handlerEngineUciOptions.postDelayed(mUpdateEngineUciOptions, 30);
				}
				else
				{
					releaseEngineService(false);
					handlerEngineUciOptions.removeCallbacks(mUpdateEngineUciOptions);
				}
    		}
		}
	};
    private void releaseEngineService(boolean unbind) 
    {	//>135 unbinds this activity from the service
    	if (processAlive) 
		{
    		writeLineToProcess("quit");
    		isReady = false;
    		processAlive = false;
		}
    	if (unbind)
    	{
			unbindService(engineServiceConnection);
			engineServiceConnection = null;
    	}
    }
    private void readUCIOptions() 
	{
    	this.setTitle(R.string.app_name_uci);
    	writeLineToProcess("uci");
    	infoText  =  new StringBuffer(2000);
    	handlerEngineUciOptionsCnt = 0;
    	handlerEngineUciOptions.removeCallbacks(mUpdateEngineUciOptions);
    	handlerEngineUciOptions.postDelayed(mUpdateEngineUciOptions, 100);
    }
    private void aboutApp() 
	{
    	this.setTitle(R.string.app_name_about);
    	tvMain.setText(getString(R.string.about).replace("\\n", "\n"));
	}
    public String[] tokenize(String cmdLine) 
    {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }
	// INNER CLASS: EngineServiceConnection			INNER CLASS: EngineServiceConnection
	// This class represents the actual service connection. 
	// It casts the bound stub implementation of the service to the AIDL interface.
	class EngineServiceConnection implements ServiceConnection 
	{	//>130
		public void onServiceConnected(ComponentName name, IBinder boundService) 
		{	//>131
			engineService = IChessEngineService.Stub.asInterface((IBinder) boundService);
			try {Thread.sleep(100);} 
			catch (InterruptedException e) {}
			startEngine();
		}
		public void onServiceDisconnected(ComponentName name) 
		{	//>132
			engineService = null;
		}
	}
// ENGINE-SERVICE		ENGINE-SERVICE		ENGINE-SERVICE		ENGINE-SERVICE		ENGINE-SERVICE
    public final void startProcess() 
	{	//>184
    	try 	
		{ 
    		if (engineService != null)
    			processAlive = engineService.startNewProcess(GUI_PROCESS_NAME);
		} 
    	catch 	( RemoteException e) 
		{
			e.printStackTrace(); 
			processAlive = false;
			engineService = null;
		}
	}
    public final synchronized void writeLineToProcess(String data) 
	{	//>185
    	if (engineService != null)
    	{
			if (processAlive)
			{
				try 	{ engineService.writeLineToProcess(data); } 
				catch 	( RemoteException e) 
				{
					e.printStackTrace();
					processAlive = false;
					engineService = null;
				}
			}
    	}
	}
    public final String readLineFromProcess(int timeoutMillis) 
	{	//>186
		String ret = "";
		if (engineService != null)
    	{
			if (processAlive)
			{
				try 	{ ret = engineService.readLineFromProcess(timeoutMillis); } 
				catch 	( RemoteException e) 
				{
					e.printStackTrace();
					processAlive = false;
					engineService = null;
				}
			}
    	}
		return ret;
	}
	public String getInfoFromEngineService(String infoId)
	{	//>187
    	String info = "";
    	if (engineService != null)
    	{
	    	try 	{ info = engineService.getInfoFromEngineService(infoId); } 
			catch 	(RemoteException e) { info = ""; }
// ERROR	v1.4	20.10.2011 15:27:10
			catch 	(NullPointerException e) { info = ""; }
    	}
		return info;
	}
	public void clearApplicationData() 
	{
//		enginePrefs = getSharedPreferences("engine", 0);
		int progressStrength = enginePrefs.getInt("strength", 100);
		int progressAggressiveness = enginePrefs.getInt("aggressiveness", 50);
		boolean logOn = enginePrefs.getBoolean("logOn", false);
		Log.i("TAG", "progressStrength, progressAggressiveness, logOn: " + progressStrength + ", " + progressAggressiveness +", " + logOn);
		
	    File cache = getCacheDir();
	    File appDir = new File(cache.getParent());
	    if (appDir.exists()) 
	    {
	        String[] children = appDir.list();
	        for (String s : children) 
	        {
	            if (!s.equals("lib")) 
	            {
	                deleteDir(new File(appDir, s));
	                Log.i("TAG", "**************** File /data/data/APP_PACKAGE/" + s + " DELETED *******************");
	            }
	        }
	    }
	    
//	    enginePrefs = getSharedPreferences("engine", 0);
	    SharedPreferences.Editor ed = enginePrefs.edit();
		ed.putInt("strength", progressStrength);
		ed.putInt("aggressiveness", progressAggressiveness);
        ed.putBoolean("logOn", logOn);
        ed.putBoolean("clearData", false);
        ed.commit();
        getEnginePrefs();
	}

	public static boolean deleteDir(File dir) 
	{
	    if (dir != null && dir.isDirectory()) 
	    {
	        String[] children = dir.list();
	        for (int i = 0; i < children.length; i++) 
	        {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) 
	                return false;
	        }
	    }
	    return dir.delete();
	}
	
	final String TAG = "StartChessEngine";
	//>111
	private static final String ENGINE_STOCKFISH = "ccc.chess.engine.stockfish.IChessEngineService";
	private static final String GUI_C4A = "ccc.chess.gui.chessforall";
	final static int HOMEPAGE_REQUEST_CODE = 10;
	final static int SOURCECODE_REQUEST_CODE = 20;
	final String APP_EMAIL = "c4akarl@gmail.com";
	//>112
	private static final String GUI_PROCESS_NAME = "START_STOCKFISH";
	final int NO_CHESS_ENGINE_DIALOG = 109;
	final int ABOUT_DIALOG = 901;
	final int CONTACT_DIALOG = 902;
	final int WHATS_NEW = 903;
//	subActivity RequestCode
	private static final int PREFERENCES_REQUEST_CODE = 1;
	IChessEngineService engineService;
	EngineServiceConnection engineServiceConnection;
	Intent engineIntent;
	public SharedPreferences enginePrefs;
	public Handler handlerEngineIsready = new Handler();
	public Handler handlerEngineUciOptions = new Handler();
	int handlerEngineIsreadyCnt = 0;
	int handlerEngineUciOptionsCnt = 0;
	boolean isAboutApp = true;					
	boolean stopService = true;	
	boolean processAlive;
	String currentProcess = "";
	boolean isReady = false;
	ImageView btnInfo = null;
	ImageView btnOptions = null;
	ImageView btnGui = null;
	ImageView btnMenu = null;
	TextView tvMain = null;
	StringBuffer infoText;
}