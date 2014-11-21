package com.electricsunstudio.xball.android;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.network.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *
 * @author toni
 */
public class ConnectToServer extends Activity {
	@Override
	public void onCreate(Bundle savesInstanceState)
	{
		super.onCreate(savesInstanceState);
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
	
		TextView hostLabel = new TextView(this);
		hostLabel.setText("Hostname");
		
		TextView portLabel = new TextView(this);
		portLabel.setText("Port");
		
		TextView usernameLabel = new TextView(this);
		usernameLabel.setText("Username");
		
		final EditText host = new EditText(this);
		final EditText port = new EditText(this);
		final EditText username = new EditText(this);
		
		port.setText("49000");
		port.setInputType(InputType.TYPE_CLASS_NUMBER);
		
		Button button = new Button(this);
		button.setText("Connect");
		button.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v)
			{
				try{
					int portNum = Integer.parseInt(port.getText().toString());
					
					if(portNum <=0 || portNum >= 65536)
					{
						Toast.makeText(ConnectToServer.this, "Invalid port", Toast.LENGTH_SHORT).show();
						return;
					}
					new MakeServerConnection().execute(host.getText().toString(), portNum, username.getText().toString());
				}
				catch(NumberFormatException e){
					Toast.makeText(ConnectToServer.this, "Invalid port", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		layout.addView(hostLabel);
		layout.addView(host);
		
		layout.addView(portLabel);
		layout.addView(port);
		
		layout.addView(usernameLabel);
		layout.addView(username);
		
		layout.addView(button);
		
		setContentView(layout);
	}
	
	class MakeServerConnection extends AsyncTask
	{
		String msg;
		boolean success;
		
		@Override
		protected Object doInBackground(Object... params) {
			String host = (String) params[0];
			Integer port = (Integer) params[1];
			String username = (String) params[2];
			
			try
			{
				InetAddress addr = InetAddress.getByName(host);

				Game.serverThread = new ObjectSocketQueue(addr, port);
				Game.serverThread.start();
				Game.username = username;
				Game.serverThread.send(new ConnectIntent(Game.username));
				
				msg = "Connected to server";
				success = true;
			}
			catch(UnknownHostException ex)
			{
				msg = "Unknown host";
				Log.e(Game.tag, "Unknown host", ex);
			}
			catch(IOException ex)
			{
				msg = "Connection error";
				Log.e(Game.tag, "IO exception connecting to server", ex);
			}
			return null;
		}
	
		@Override
		protected void onPostExecute(Object o)
		{
			Toast.makeText(ConnectToServer.this, msg, Toast.LENGTH_SHORT).show();
			if(success)
			{
				//make status toast and open Lobby if successful
				Intent intent = new Intent(ConnectToServer.this, Lobby.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		}
	}
}
