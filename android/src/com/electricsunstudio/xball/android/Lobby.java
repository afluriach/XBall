package com.electricsunstudio.xball.android;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.network.*;

/**
 *
 * @author toni
 */
public class Lobby extends Activity{
	@Override
	public void onCreate(Bundle savedInstance)
	{
		super.onCreate(savedInstance);
		
		LinearLayout layout = new LinearLayout(this);
		
		TextView title = new TextView(this);
		title.setText("Lobby");
		
		layout.addView(title);
		
		setContentView(layout);
	}
	
	@Override
	public void onBackPressed()
	{
		Game.serverThread.send(new DisconnectIntent());
		Game.serverThread.quit = true;
		super.onBackPressed();
	}
}
