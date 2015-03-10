package ccc.chess.engine.stockfish;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Comparator;
import android.annotation.SuppressLint;
import android.os.Environment;
//import android.util.Log;

public class EngineFileManager 
{	// managing engines on package file system: /data/data/ccc.chess.engine.stockfish/engines

	//	FILE (SD-CARD)		FILE (SD-CARD)		FILE (SD-CARD)		FILE (SD-CARD)		FILE (SD-CARD)
	public String getExternalDirectory()
	{
		File f = Environment.getExternalStorageDirectory();
		return f.toString();
	}
	public String getParentFile(String path)
    {
		String newPath = "";
		File f = new File(path);
		try 	{newPath = f.getParentFile().toString();}
		catch 	(NullPointerException e) {return path;}
		return newPath;
    }
	public String[] getFileArrayFromPath(String path)
    {
		String[] sortedFiles = null;
// ERROR	v1.0	14.11.2011 02:24:11
		try
		{
			File f = new File(path);;
			sortedFiles = f.list();
			Arrays.sort(sortedFiles, new AlphabeticComparator());
			currentFilePath = path;
		}
		catch (NullPointerException e) {e.printStackTrace();}
		return sortedFiles;
    }
	public boolean fileIsDirectory(String fileName)
	{
		File f = new File(currentFilePath, fileName);
		return f.isDirectory();
	}
	//	FILE (DATA)		FILE (DATA)		FILE (DATA)		FILE (DATA)		FILE (DATA)
	public String[] getFileArrayFromData()
    {
		String[] sortedFiles = null;
		File f;
		f = new File(dataEnginesPath);
		sortedFiles = f.list();
		Arrays.sort(sortedFiles, new AlphabeticComparator());
		return sortedFiles;
    }
	public boolean writeEngineToData(String filePath, String fileName, InputStream is) 
	{ // if no engine exists in >/data/data/ccc.chess.engine.stockfish/engines< install default engine!
		File file = new File(dataEnginesPath);
		if (!file.exists())
		{
//			Log.i(TAG, dataEnginesPath + " not exists");
			if (!file.mkdir())
				return false;
			else
			{
//				Log.i(TAG, dataEnginesPath + " created");
			}
		}
		boolean engineUpdated = false;
		File f = new File(dataEnginesPath, fileName);
		if (f.exists()) 
		{
			try 
			{
				String cmd[] = { "chmod", "744", f.getAbsolutePath() };
				Process process = Runtime.getRuntime().exec(cmd);
				try 
				{
					process.waitFor();
					engineUpdated = true;
				} 
				catch (InterruptedException e) 
				{
					deleteFileFromData(fileName);
					return false;
				}
			} 
			catch (IOException e) 
			{
				deleteFileFromData(fileName);
				return false;
			}
		} 
		else
		{
//			Log.i(TAG, "Engine is missing from data. Intializing...");
//			Log.i(TAG, "dataEnginesPath, fileName: " + dataEnginesPath + ", " + fileName);
			try 
			{
				InputStream istream = null;
				if (is != null)
				{
					istream = is;
				}
				else
				{
					f = new File(filePath, fileName);
					istream = new FileInputStream(f);
				}
//				FileOutputStream fout = new FileOutputStream(dataEnginesPath + "/" + fileName);
				FileOutputStream fout = new FileOutputStream(dataEnginesPath + fileName);
				byte[] b = new byte[1024];
				int noOfBytes = 0;
				while ((noOfBytes = istream.read(b)) != -1) 
				{
					fout.write(b, 0, noOfBytes);
				}
				istream.close();
				fout.close();
//				Log.i(TAG, fileName + " copied to " + dataEnginesPath);
				try 
				{
//					String cmd[] = { "chmod", "744", dataEnginesPath + "/" + fileName };
					String cmd[] = { "chmod", "744", dataEnginesPath + fileName };
					Process process = Runtime.getRuntime().exec(cmd);
					try 
					{
						process.waitFor();
						engineUpdated = true;
					} 
					catch (InterruptedException e) 
					{
//						Log.i(TAG, "InterruptedException ???");
						deleteFileFromData(fileName);
						return false;
					}
				} 
				catch (IOException e) 
				{
//					Log.i(TAG, "IOException ???");
					deleteFileFromData(fileName);
					return false;
				}
				if (!isEngineProcess(fileName))	// not an engine process: kill data and return!
				{
//					Log.i(TAG, "!isEngineProcess(fileName) ???");
					deleteFileFromData(fileName);
					return false;
				}
			} 
			catch (IOException e) 
			{
//				Log.i(TAG, "IOException 2 ???");
				deleteFileFromData(fileName);
				return false;
			}
		}
		return engineUpdated;
	}
	private boolean isEngineProcess(String file) 
	{
		boolean isProcess = false;
    	Process process;
//    	ProcessBuilder builder = new ProcessBuilder(dataEnginesPath + "/" + file);
    	ProcessBuilder builder = new ProcessBuilder(dataEnginesPath + file);
    	try 
		{
    		process = builder.start();
    		OutputStream stdout = process.getOutputStream();
			InputStream stdin = process.getInputStream();
			reader = new BufferedReader(new InputStreamReader(stdin));
			writer = new BufferedWriter(new OutputStreamWriter(stdout));
//			Log.i(TAG, file + " start process");
		} 
		catch (IOException e) 
		{
//			Log.i(TAG, file + " not an engine process");
			return false;
		}
		try 
		{
//			Log.i(TAG, file + " write isready to process");
			writeToProcess("isready" + "\n");
		}
		catch (IOException e) 
		{
//			Log.i(TAG, file + " write error, IOException: ");
			e.printStackTrace();
		}
		
//		try {Thread.sleep(150);} 
//		catch (InterruptedException e) {}
		String line = "";
		int cnt = 0;
		while (cnt < 100)
		{
			try
			{
				line = readFromProcess();
//				Log.i(TAG, "cnt, line: " + cnt + ", " + line);
				if (line != null)
				{
					if (line.equals("readyok") | line.endsWith("readyok"))
					{
						isProcess = true;
						cnt = 100;
					}
				}
			}
			catch (IOException e) {e.printStackTrace();}
			try {Thread.sleep(10);} 
			catch (InterruptedException e) {}
			cnt++;
		}

		process.destroy();
		return isProcess;
	}
	private final synchronized void writeToProcess(String data) throws IOException //>175 write data to the process 
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
//		Log.i(TAG, "reader.ready(): " + reader.ready());
		if (reader != null)
		{
			line = reader.readLine();
		}
		return line;
	}
	public boolean dataFileExist(String file)
	{
		File f = new File(dataEnginesPath, file);
		return f.exists();
	}
	public boolean deleteFileFromData(String file)
	{
		File f = new File(dataEnginesPath, file);
		return  f.delete();
	}
	//	HELPERS		HELPERS		HELPERS		HELPERS		HELPERS		HELPERS		HELPERS
	class AlphabeticComparator implements Comparator<Object> 
	{
		  @SuppressLint("DefaultLocale")
		public int compare(Object o1, Object o2) 
		  {
		    String s1 = (String) o1;
		    String s2 = (String) o2;
		    return s1.toLowerCase().compareTo(s2.toLowerCase());
		  }
	}
	
	final String TAG = "EngineFileManager";
	String dataEnginesPath = "";
	String currentFilePath = "";
	private BufferedReader reader = null;
	private BufferedWriter writer = null;
}
