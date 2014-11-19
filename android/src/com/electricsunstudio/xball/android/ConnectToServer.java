package com.electricsunstudio.xball.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.network.ClientAction;
import com.electricsunstudio.xball.network.ClientIntent;
import com.electricsunstudio.xball.network.ObjectSocketQueue;
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
					InetAddress addr = InetAddress.getByName(host.getText().toString());
					
					Game.serverThread = new ObjectSocketQueue(addr, portNum);
					Game.serverThread.start();
					Game.username = username.getText().toString();
					Game.serverThread.send(new ClientIntent(ClientAction.connect, "", Game.username));

					Intent intent = new Intent(ConnectToServer.this, Lobby.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
				catch(NumberFormatException e){
					Toast.makeText(ConnectToServer.this, "Invalid port", Toast.LENGTH_SHORT).show();
				} catch (UnknownHostException ex) {
					Toast.makeText(ConnectToServer.this, "Unknown host", Toast.LENGTH_SHORT).show();					
				} catch (IOException ex) {
					Toast.makeText(ConnectToServer.this, "IO Exception", Toast.LENGTH_SHORT).show();
					ex.printStackTrace();
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
}
