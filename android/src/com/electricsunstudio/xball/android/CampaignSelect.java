package com.electricsunstudio.xball.android;

import android.R;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import com.electricsunstudio.xball.Game;

/**
 *
 * @author toni
 */
public class CampaignSelect extends XballActivity {
    @Override
    public void onCreate(Bundle savesInstanceState)
    {
        super.onCreate(savesInstanceState);
        LinearLayout layout = new LinearLayout(this);
        ListView list = new ListView(this);
        
        String[] levels = Game.availableLevelNames();
        
        ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1, levels);
        list.setAdapter(listAdapter);
        
        list.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View view, int i, long l) {
                //start game
                CampaignSelectViewer.selectedLevel = Game.availableLevels[i];
                Log.e(Game.tag, "selected level: " + CampaignSelectViewer.selectedLevel.getSimpleName());
                startActivity(CampaignSelectViewer.class);
            }
        });
        
        layout.addView(list, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        setContentView(layout);
    }
}
