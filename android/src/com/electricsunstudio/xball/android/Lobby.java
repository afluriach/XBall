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
public class Lobby extends XballActivity{
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
                StartMatch match = (StartMatch) t;
                Game.player = match.player;
                Game.level = Game.getLevelFromSimpleName(match.levelName);
                Log.d(Game.tag, String.format("starting level %s as %s\n", match.levelName, match.player));

                //start match
				runUiTask(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        showToast("Starting match", false);
                        startActivity(AndroidLauncher.class);
                    }
                });
			}
		});
	}
	
	@Override
	public void onBackPressed()
	{
		Game.serverOutput.send(new DisconnectIntent());
		Game.serverOutput.quit = true;
		super.onBackPressed();
	}
}
