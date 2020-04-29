package ccc.chess.engine.stockfish;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.main_layout);

		Log.i(TAG, "MainActivity, onCreate()");

		finish();
	}

	final String TAG = "MainActivity";
}
