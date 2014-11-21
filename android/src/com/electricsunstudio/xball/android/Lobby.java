package com.electricsunstudio.xball.android;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
		
		//add listener, start game when startmatch is received
		Game.serverInput.addHandler(StartMatch.class, new Handler(){
			@Override
			public void onReceived(Object t) {
				new StartMatchTask().execute(t);
			}
		});
	}
	
	class StartMatchTask extends AsyncTask
	{
		@Override
		protected Object doInBackground(Object... params) {
			StartMatch match = (StartMatch) params[0];
			Game.player = match.player;
			Game.level = Game.getLevelFromSimpleName(match.levelName);
			
			Log.d(Game.tag, String.format("starting level %s as %s\n", match.levelName, match.player));
			
			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			Toast.makeText(Lobby.this, "Starting match", Toast.LENGTH_SHORT).show();
			Intent intent = new Intent(Lobby.this, AndroidLauncher.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}
	
	@Override
	public void onBackPressed()
	{
		Game.serverOutput.send(new DisconnectIntent());
		Game.serverOutput.quit = true;
		super.onBackPressed();
	}
}
