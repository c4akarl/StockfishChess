package ccc.chess.engine.stockfish;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		webView = findViewById(R.id.info);
		webView.loadUrl(engineInfo);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		finish();
	}

//	final String TAG = "MainActivity";
	WebView webView;
	final String engineInfo = "file:///android_res/raw/engine_info";

}
