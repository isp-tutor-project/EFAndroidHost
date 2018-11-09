package org.edforge.androidhost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import static org.edforge.androidhost.TCONST.EFHOME_FINISHER_INTENT;
import static org.edforge.androidhost.TCONST.EFHOST_FINISHER_INTENT;
import static org.edforge.androidhost.TCONST.PLUG_DISCONNECT;

/**
 * Created by kevin on 11/6/2018.
 */

public class PlugStatusReceiver extends BroadcastReceiver {

    private static final String TAG               = "PlugStatusReceiver";
    public static final String PLUG_PREFS         = "plug_prefs";
    public static final String PLUG_CONNECT_STATE = "plug_conn";

    private SharedPreferences mSharedPrefs;
    private LocalBroadcastManager bManager;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(context);

        Log.i(TAG, "BATTERY:" + intent.getAction());

        mSharedPrefs = context.getSharedPreferences(PLUG_PREFS, Context.MODE_PRIVATE);
        
        if(intent.getAction() == PLUG_CONNECT_STATE) {
            markPlugConnected(true);

            broadcast(EFHOST_FINISHER_INTENT);
        }
        if(intent.getAction() == PLUG_DISCONNECT) {
            markPlugConnected(false);
        }
    }

    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }

    void showToast(Context context, String msg) {
        String status = "Admin State Change:" + msg;
        Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
    }

    private boolean isPlugConnected() {

        return mSharedPrefs.getBoolean(PLUG_CONNECT_STATE, false);
    }
    private void markPlugConnected(boolean status) {

        mSharedPrefs.edit().putBoolean(PLUG_CONNECT_STATE, status).commit();
    }


}
