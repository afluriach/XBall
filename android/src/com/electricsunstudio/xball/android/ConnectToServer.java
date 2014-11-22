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
public class ConnectToServer extends XballActivity {
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
                        showToast("Invalid port", false);
                        return;
                    }
                    new MakeServerConnection().execute(host.getText().toString(), portNum, username.getText().toString());
                }
                catch(NumberFormatException e){
                    showToast("Invalid port", false);
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
    
    class ServerResponse extends AsyncTask
    {
        boolean connected;
        String msg;

        public ServerResponse(boolean connected, String msg) {
            this.connected = connected;
            this.msg = msg;
        }

        
        @Override
        protected Object doInBackground(Object... paramss) {
            return null;
        }
        
        @Override
        protected void onPostExecute(Object o)
        {
            showToast(msg, false);
            if(connected)
            {
                startActivity(Lobby.class);
            }
        }
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
                
                Game.serverOutput = new ObjectSocketOutput(addr, port);
                Game.serverOutput.start();
                Game.username = username;
                
                Game.serverInput = new ObjectSocketInput(Game.serverOutput.sock);
                Game.serverInput.addHandler(LoginResponse.class, new Handler(){

                    @Override
                    public void onReceived(Object t) {
                        LoginResponse response = (LoginResponse) t;
                        new ServerResponse(response.success, response.msg).execute();
                    }
                });
                
                Game.serverInput.start();
                Game.serverOutput.send(new ConnectIntent(Game.username));

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
            //make status toast and open Lobby if successful
            showToast(msg, false);
        }
    }
}
