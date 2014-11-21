package com.electricsunstudio.xball.android;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 *
 * @author toni
 */
public class XballActivity extends Activity
{
    public void showToast(String msg, boolean longToast)
    {
        Toast.makeText(this, msg, longToast ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
    public void startActivity(Class activityCls)
    {
        Intent intent = new Intent(this, activityCls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    public void runUiTask(Runnable r)
    {
        final Runnable run = r;
        new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object... paramss) {
                return null;
            }
            @Override
            protected void onPostExecute(Object o)
            {
                run.run();
            }
        }.execute();
    }
}
