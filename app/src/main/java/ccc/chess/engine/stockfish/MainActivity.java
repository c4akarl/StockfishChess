package ccc.chess.engine.stockfish;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.TextView;

// Added: recommended chess GUIs
public class MainActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_layout);
		webView = findViewById(R.id.info);
		webView.loadUrl(engineInfo);
		engineTitle = findViewById(R.id.engineTitle);
		engineTitle.setText("Stockfish Engines  " + BuildConfig.VERSION_NAME);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		finish();
	}

	WebView webView;
	final String engineInfo = "file:///android_res/raw/engine_info";
	TextView engineTitle = null;

}
