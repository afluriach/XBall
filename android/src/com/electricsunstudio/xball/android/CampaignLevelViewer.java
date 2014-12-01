package com.electricsunstudio.xball.android;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.electricsunstudio.xball.Game;
import com.electricsunstudio.xball.levels.Level;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 *
 * @author toni
 */
public class CampaignLevelViewer extends XballActivity {
    public static Class selectedLevel;
    @Override
    public void onCreate(Bundle savesInstanceState)
    {
        super.onCreate(savesInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        String mapName = Game.getMapName(selectedLevel);

        //level name
        TextView title = new TextView(this);
        title.setText(Game.levelName(selectedLevel));
        layout.addView(title);
        
        //display preview of level
        ImageView preview = new ImageView(this);
        InputStream previewIs = null;
        try {
            previewIs = getApplicationContext().getAssets().open("previews/"+mapName+".png");
        } catch (IOException ex) {
            Log.e(Game.tag, "error opening level preview ", ex);
        }
        Drawable previewImage = Drawable.createFromStream(previewIs, "preview");
        preview.setImageDrawable(previewImage);
        layout.addView(preview);
        
        //TODO level description
        
        //icons to show if level has been completed for each player type
        LinearLayout trophyView = new LinearLayout(this);
        String[] colors = {"grey", "red", "green", "blue"};
        ImageView[] trophyIcons = new ImageView[4];
        for(int i=0;i<colors.length; ++i)
        {
            trophyIcons[i] = new ImageView(this);
            InputStream is = null;
            try {
                is = getApplicationContext().getAssets().open("sprite/"+colors[i]+"_trophy_dark.png");
            } catch (IOException ex) {
                Log.e(Game.tag, "exception opening image", ex);
            }
            trophyIcons[i].setImageDrawable(Drawable.createFromStream(is, colors[i]));
            trophyView.addView(trophyIcons[i]);
        }
        
        layout.addView(trophyView);
        
        Button launch = new Button(this);
        launch.setText("Play");
        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Game.level = selectedLevel;
                startActivity(AndroidLauncher.class);
            }
        });
        layout.addView(launch);
        
        setContentView(layout);
    }
}