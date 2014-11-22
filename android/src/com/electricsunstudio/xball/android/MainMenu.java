package com.electricsunstudio.xball.android;

import android.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;

/**
 *
 * @author toni
 */
public class MainMenu extends XballActivity {
    @Override
    public void onCreate(Bundle savesInstanceState)
    {
        super.onCreate(savesInstanceState);
        LinearLayout layout = new LinearLayout(this);
        ListView list = new ListView(this);
        
        final String[] options = {"Campaign", "Connect to server"};
        
        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1, options);
        list.setAdapter(listAdapter);
        
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View view, int i, long l) {
                if(i == 0)
                {
                    startActivity(CampaignSelect.class);
                }
                else if(i == 1)
                {
                    startActivity(ConnectToServer.class);
                }
            }
        });
        
        layout.addView(list, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        setContentView(layout);
    }
}
