package ccc.chess.engine.stockfish;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);

		Log.i(TAG, "1 MainActivity, onCreate()");
		Log.i(TAG, "2 MainActivity, onCreate()");

//		finish();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		finish();
	}
	final String TAG = "MainActivity";
}
