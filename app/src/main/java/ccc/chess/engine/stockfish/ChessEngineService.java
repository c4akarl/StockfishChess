package ccc.chess.engine.stockfish;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ChessEngineService extends Service
{	//>150 the bridge to the gui and to the engine(native processes)
	@Override
    public void onCreate() 
	{
		dataEnginesPath = getApplicationContext().getFilesDir() + "/engines/";
		efm = new EngineFileManager();
		efm.dataEnginesPath = dataEnginesPath;
		assetsEngineProcess = getInternalStockFishProcessName();

//		getPrefs();
		getPrefs_2();	// from ChessEngines
		
		engineName = ENGINE_PROCESS_NAME;
		if (isLogOn)
			Log.i(TAG, "assetsEngineProcess, engineName: " + assetsEngineProcess + ", " + engineName);
		super.onCreate();
    }
	@Override
	public IBinder onBind(Intent intent) 
	{	//>154 return the IBinder object
		if (isLogOn)
			Log.i(TAG, ENGINE_PROCESS_NAME + ": onBind(Intent): " + intent);
		return mBinder;
	}
	@Override
	public boolean onUnbind(Intent intent) 
	{	//>155 unbinds the Service from the Gui
		setPrefs();
		if (isLogOn)
			Log.i(TAG, engineName + ": onUnbind(intent): " + intent);
		stopSelf();	//>156 for starting/stopping; start Service: --->332 (CCC GUI)
		currentCallPid = "";
		engineName = "";
		if (process != null)
			process.destroy();
		return super.onUnbind(intent);
	}
	public String getCurrentCallPid()	{return currentCallPid;}	//>157 the current running Gui process on ChessEngineService
	
	private void getPrefs() 
    {	// get user preferences
		enginePrefs = getSharedPreferences("engine", 0);		//	engine Preferences
		isLogOn = enginePrefs.getBoolean("logOn", false);

//		isLogOn = true;	// test only
		
		engineProcess = enginePrefs.getString("engineProcess", engineProcess);
		int oldVersionCode = enginePrefs.getInt("versionCode", 0);
		try 
		{
			PackageInfo pinfo;
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionCode = pinfo.versionCode;
		} 
		catch (NameNotFoundException e) {e.printStackTrace();}
		if (isLogOn)
			Log.i(TAG, "versionCode, assetsEngineProcess, engineProcess: " + oldVersionCode + ", " + versionCode + ", " + assetsEngineProcess + ", " + engineProcess);
		if (!engineProcess.equals(assetsEngineProcess) | oldVersionCode != versionCode)
		{
			efm.deleteFileFromData(engineProcess);
			engineProcess = "";
		}
		if (!efm.dataFileExist(engineProcess) | engineProcess.equals(""))
		{
			if (isLogOn)
				Log.i(TAG, "writeDefaultEngineToData()");
			writeDefaultEngineToData();
		}
    }
	
	private void getPrefs_2() 
    {	// get user preferences
		enginePrefs = getSharedPreferences("engine", 0);		//	engine Preferences
		isLogOn = enginePrefs.getBoolean("logOn", false);
		engineProcess = enginePrefs.getString("engineProcess", engineProcess);
		if (!efm.dataFileExist(engineProcess) | !engineProcess.equals(assetsEngineProcess) | engineProcess.equals(""))
		{
			if (isLogOn)
				Log.i(TAG, "writeDefaultEngineToData()");
			writeDefaultEngineToData();
		}
//		Log.i(TAG, "getPrefs, engineProcess: " + engineProcess);
//		isLogOn = true; // for test only
    }
	
	private void setPrefs() 
    {	// update user preferences
		SharedPreferences.Editor ed = enginePrefs.edit();
//		ed.putBoolean("logOn", isLogOn);
		ed.putString("engineProcess", engineProcess);
		ed.putInt("versionCode", versionCode);
		ed.commit();
    }
	private void setChessEngineName(String uciIdName) 
	{ 
		engineName = uciIdName.substring(8, uciIdName.length());
	}
	private String getChessEngineNameAndStrength() 
	{ 
		String engineNameStrength = engineName;
		if (!engineName.equals("") & enginePrefs.getInt("strength", 100) != 100)
			engineNameStrength = engineName + " (" + enginePrefs.getInt("strength", 100) + "%, Level: " + uciStrength + ")";
		return engineNameStrength;
	}
	public String getInternalStockFishProcessName() 
	{
//        return "stockfish-" + Build.CPU_ABI;
		if (Build.CPU_ABI.endsWith("x86"))
			return ASSET_STOCKFISH_CPU_X86;
		else
			return ASSET_STOCKFISH_CPU_STANDARD;
    }
	private void writeDefaultEngineToData() 
    {
		try 
		{
			InputStream istream = getAssets().open(assetsEngineProcess);
//			InputStream istream = getAssets().open(assetsEngineProcess + ".aac");	// extension ".aac": no compression !
			if (efm.writeEngineToData("", assetsEngineProcess, istream))
				engineProcess = assetsEngineProcess;
			else
				engineProcess = "";
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			if (isLogOn)
				Log.i(TAG, "writeDefaultEngineToData(), IOException");
			engineProcess = "";
		}
		if (isLogOn)
			Log.i(TAG, "writeDefaultEngineToData(), assetsEngineProcess, engineProcess: " + assetsEngineProcess + ", " + engineProcess);
    }
	private final IChessEngineService.Stub mBinder = new IChessEngineService.Stub() 
	{	//>153 the EngineService / Gui methods(164...167)
		// ENGINE_SERVICE <--> CHESS_GUI + CALL NATIVE METHODS		ENGINE_SERVICE <--> CHESS_GUI + CALL NATIVE METHODS
		public boolean  startNewProcess(String callPid) throws RemoteException
		{	//>164 (called from Gui)
			processAlive = false;
			if (isLogOn)
				Log.i(TAG, "startNewProcess, engineProcess: " + engineProcess);
			if (isLogOn & !currentCallPid.equals(""))
				Log.i(TAG, engineName + ": current Process, startNewProcess: " + currentCallPid + ", " + callPid);
			if (currentCallPid.equals("") | currentCallPid.equals(callPid))
			{
				processAlive = startProcess(engineProcess);
				if (processAlive)
				{
					currentCallPid = callPid;
					if (isLogOn)
						Log.i(TAG, engineName + ": new session, gui: " + currentCallPid);
				}
				else
				{
					if (isLogOn)
						Log.i(TAG, engineName + ": error startProcess, engine name: " + engineProcess);
					currentCallPid = "";
				}
			}
			else
			{
				if (isLogOn)
					Log.i(TAG, engineName + ": startNewProcess failed, an process already is running: " + currentCallPid);
			}
			return processAlive;
		}
		public void writeLineToProcess(String data) throws RemoteException
		{	//>165 (called from Gui)
			try {writeToProcess(data + "\n");}
			catch (IOException e) 
			{
				e.printStackTrace();
				processAlive = false;
			}
			if (isLogOn)
				Log.i(TAG, currentCallPid + " >>> " +  engineName + ": " + data);
			if (data.equals("quit"))
			{
				currentCallPid = "";
				engineName = "";
				if (process != null)
					process.destroy();
			}
		}
		public String readLineFromProcess(int timeoutMillis)  throws RemoteException
		{	//>166 (called from Gui)
			String message = "";
			try{message =  readFromProcess();}
			catch (IOException e) 
			{
				if (isLogOn)
					Log.i(TAG, engineName + " >>> " + currentCallPid + ": IOException");
				e.printStackTrace();
			}
			if (message != null)
			{
				if (message.startsWith("id name "))
					setChessEngineName(message);
				if (isLogOn)
					Log.i(TAG, engineName + " >>> " + currentCallPid + ": " + message);
			}
			else
				message = "";
			if (message.equals("uciok"))
				setUciPrefs();
			return message;
		}
		// ENGINE_SERVICE <--> CHESS_GUI		ENGINE_SERVICE <--> CHESS_GUI	ENGINE_SERVICE <--> CHESS_GUI
		public String getInfoFromEngineService(String infoId)  throws RemoteException
		{	//>167 for GUI <---> SERVICE only, no native process! (called from GUI)
			String info = "";
			if (infoId.equals("CURRENT_PROCESS"))
				info =  getCurrentCallPid();
			if (infoId.startsWith("ENGINE_PROCESS"))
				info =  engineProcess;								// get current engine process(file name)
			if (infoId.equals("ENGINE_NAME"))
				info =  engineName;								// get current engine name
			if (infoId.equals("ENGINE_TYPE"))
				info =  ENGINE_TYPE;
			if (infoId.equals("SET_LOGFILE_ON"))
				isLogOn = true;
			if (infoId.equals("SET_LOGFILE_OFF"))
				isLogOn = false;
			if (infoId.equals("GET_ENGINE_NAME_STRENGTH"))
			{
				if (enginePrefs.getInt("strength", 100) != 100)
					info = getChessEngineNameAndStrength();
			}			
			if (infoId.equals("ENGINE_ALIVE"))
				info =  Boolean.toString(processAlive);
			if (isLogOn)
				Log.i(TAG, ENGINE_PROCESS_NAME + "(" + engineName + "): getInfoFromEngineService, " + infoId + ": " + info);
			return info;
		}
	};
	// NATIVE METHODS		NATIVE METHODS		NATIVE METHODS		NATIVE METHODS		NATIVE METHODS
//	final static native int getNPhysicalProcessors();				// Stockfish 2.2.2
//	private final native void startProcess();						//>174 start native process
//	private final native void writeToProcess(String data);			//>175 write data to the process
//	private final native String readFromProcess(int timeoutMillis);	//>176 read a line of data from the process
	
	private final boolean startProcess(String fileName)	//>174 start native process
	{
//		ProcessBuilder builder = new ProcessBuilder(DATA_ENGINES_PATH + "/" + fileName);
		ProcessBuilder builder = new ProcessBuilder(dataEnginesPath + fileName);
		try 
		{
			process = builder.start();
			OutputStream stdout = process.getOutputStream();
			InputStream stdin = process.getInputStream();
			reader = new BufferedReader(new InputStreamReader(stdin));
			writer = new BufferedWriter(new OutputStreamWriter(stdout));
			return true;
		} 
		catch (IOException e) 
		{
			if (isLogOn)
			{
				Log.i(TAG, engineName + ": startProcess, IOException");
				Log.i(TAG, engineName + "dataEnginesPath, fileName: " + dataEnginesPath + ", " + fileName);
			}
			return false;
		}
	}
	private final void writeToProcess(String data) throws IOException //>175 write data to the process 
	{
		if (writer != null) 
		{
			writer.write(data);
			writer.flush();
		}
	}
	private final String readFromProcess() throws IOException //>176 read a line of data from the process
	{
		String line = null;
		if (reader != null && reader.ready())
		{
			line = reader.readLine();
		}
		return line;
	}
	void setUciPrefs()
	{
		int skillStrength = (int) (0.2 * enginePrefs.getInt("strength", 100));
		int skillAggressiveness = (int) (2 * enginePrefs.getInt("aggressiveness", 50));
		String mesStrength =  "Skill Level: " + skillStrength + ", strength: " + enginePrefs.getInt("strength", 100) + "%";
		String mesAggressiveness =  "Aggressiveness: " + skillAggressiveness + ", aggressiveness: " + enginePrefs.getInt("aggressiveness", 50) + "%";
		try 
		{
			if (isLogOn)
			{
				Log.i(TAG, engineName + " >>> " + currentCallPid + ": " + mesStrength);
				Log.i(TAG, engineName + " >>> " + currentCallPid + ": " + mesAggressiveness);
				Log.i(TAG, engineName + " >>> " + currentCallPid + ": " + "setoption name Skill Level value " + skillStrength);
				Log.i(TAG, engineName + " >>> " + currentCallPid + ": " + "setoption name Aggressiveness value " + skillAggressiveness);
			}
			writeToProcess("setoption name Skill Level value " + skillStrength + "\n");
			writeToProcess("setoption name Aggressiveness value " + skillAggressiveness + "\n");
			uciStrength = skillStrength;
		}
		catch (IOException e) {e.printStackTrace();}
	}
	
	final String TAG = "ChessEngineService";
	private static final String ENGINE_PROCESS_NAME = "Stockfish";	//>152 Engine name, using for logging
	private static final String ENGINE_TYPE = "";				//> Engine compile type, (JNI(since versionCode 8: ""), ARM)
	boolean isLogOn = false;			// LogFile on/off(SharedPreferences)
	SharedPreferences enginePrefs;		// user preferences(LogFile on/off)	
	EngineFileManager efm;
	String currentCallPid = "";			// the process bound to this service(Gui or StartChessEngine) // final String GUI_PROCESS_NAME
	String engineName = "";				// the uci engine name
	String engineProcess = "";			// the compiled engine process name (file name)
	int versionCode = 0;				// application versioon number
	String assetsEngineProcess = "";
	final String ASSET_STOCKFISH_CPU_STANDARD = "stockfish-6-ja";
//	final String ASSET_STOCKFISH_CPU_STANDARD = "stockfish5-armv7";
	final String ASSET_STOCKFISH_CPU_X86 = "stockfish_x86";

	String dataEnginesPath = "";
	
	private boolean processAlive = false;
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
	int uciStrength = 3000;
	private Process process;
}