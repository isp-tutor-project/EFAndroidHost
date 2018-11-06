//*********************************************************************************
//
//    Copyright(c) 2018  Kevin Willows All Rights Reserved
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************
//
package org.edforge.androidhost;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.edforge.engine.CTutorAssetManager;
import org.edforge.engine.ITutorManager;
import org.edforge.net.CErrorManager;
import org.edforge.net.CLogManager;
import org.edforge.net.CPreferenceCache;
import org.edforge.net.ILogManager;
import org.edforge.util.CAssetObject;
import org.edforge.util.CDisplayMetrics;
import org.edforge.util.IGuidView;
import org.edforge.util.JSON_Helper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.edforge.androidhost.TCONST.GRAPH_MSG;
import static org.edforge.androidhost.TCONST.EDFORGE_ASSET_PATTERN;
import static org.edforge.androidhost.TCONST.LAUNCH_TUTOR;

// Push content via ADB
// E:\Projects\EdForge\PRODUCTION\EdForge> adb push ./ /sdcard/EdForge

public class AndroidHost extends AppCompatActivity {

    static public MasterContainer masterContainer;
    static public ILogManager     logManager;
    static CTutorAssetManager     tutorAssetManager;

    static public String        VERSION_AH;
    static public ArrayList     VERSION_SPEC;

    static public CDisplayMetrics displayMetrics;

    private LocalBroadcastManager   bManager;
    private hostReceiver            bReceiver;

    private LayoutInflater          mInflater;
    private HostWebView mWebView;
    private View                    mCurrView = null;


    static public String        APP_PRIVATE_FILES;
    static public String        LOG_ID = "STARTUP";

    static public Activity      ACTIVITY;
    static public String        PACKAGE_NAME;
    static public boolean       DELETE_INSTALLED_ASSETS = false;

    final static public  String CacheSource = TCONST.ASSETS;                // assets or extern

    // TODO: This is a temporary log update mechanism - see below
    //
    static private IGuidView guidCallBack;

    private boolean                 isReady       = false;
    private boolean                 engineStarted = false;
    static public boolean           STANDALONE    = false;

    public final static String  LOG_PATH       = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_FOLDER;
    public final static String  DOWNLOAD_PATH  = Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DOWNLOADS;
    public final static String  EXT_ASSET_PATH = Environment.getExternalStorageDirectory() + File.separator + TCONST.EDFORGE_ASSET_FOLDER;

    private final  String  TAG = "CAndroidHost";

    private final String[] tutors = {"/mats.html","/dr.html","/rq.html","/intro.html","/ted.html"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Note = we don't want the system to try and recreate any of our views- always pass null
        //
        super.onCreate(null);

        FrameLayout.LayoutParams params;

        // Get the primary container for tutors
        //
        setContentView(R.layout.activity_host);
        masterContainer = (MasterContainer)findViewById(R.id.master_container);

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(this);

        IntentFilter filter = new IntentFilter(LAUNCH_TUTOR);

        bReceiver = new hostReceiver();
        bManager.registerReceiver(bReceiver, filter);

        //TODO: fix up preferences initialization
        onStartTutor();

        PACKAGE_NAME = getApplicationContext().getPackageName();
        ACTIVITY     = this;

        VERSION_AH = BuildConfig.VERSION_NAME;
        VERSION_SPEC = CAssetObject.parseVersionSpec(VERSION_AH);

        logManager = CLogManager.getInstance();
        logManager.startLogging(LOG_PATH);
        CErrorManager.setLogManager(logManager);

        // TODO : implement time stamps
        logManager.postDateTimeStamp(GRAPH_MSG, "EdForge:SessionStart");
        logManager.postEvent_I(GRAPH_MSG, "EngineVersion:" + VERSION_AH);

        Log.v(TAG, "External_Download:" + DOWNLOAD_PATH);

        // Set fullscreen and then get the screen metrics
        //
        // get the multiplier used for drawables at the current screen density and calc the
        // correction rescale factor for design scale
        // This initializes the static object
        //
        setFullScreen();
        displayMetrics = CDisplayMetrics.getInstance(this);

        APP_PRIVATE_FILES = getApplicationContext().getExternalFilesDir("").getPath();

        // Initialize the JSON Helper STATICS - just throw away the object.
        //
        new JSON_Helper(getAssets(), CacheSource, AndroidHost.APP_PRIVATE_FILES);

        // Initialize the media manager singleton - it needs access to the App assets.
        //
//        mMediaController = CMediaController.getInstance();
        AssetManager mAssetManager = getApplicationContext().getAssets();
//        mMediaController.setAssetManager(mAssetManager);

        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mWebView = (HostWebView) mInflater.inflate(R.layout.web_view, null );
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mWebView.setLayoutParams(params);

        switchView(mWebView);

        broadcast(LAUNCH_TUTOR, tutors[0]);
    }


    /**
     * Ignore the state bundle
     *
     * @param bundle
     */
    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        //super.onRestoreInstanceState(bundle);
        logManager.postEvent_V(TAG, "EdForge:onRestoreInstanceState");
    }


    private void setFullScreen() {

        ((View) masterContainer).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }


    /**
     *
     *
     * @param event
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        boolean result = super.dispatchTouchEvent(event);

        switch (event.getAction()) {

            case MotionEvent.ACTION_UP:
                logManager.postEvent_V(TAG, "RT_SCREEN_RELEASE: X:" + event.getX() + "  Y:" + event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                logManager.postEvent_V(TAG, "RT_SCREEN_MOVE X:" + event.getX() + "  Y:" + event.getY());
                break;

            case MotionEvent.ACTION_DOWN:
                logManager.postEvent_V(TAG, "RT_SCREEN_TOUCH X:" + event.getX() + "  Y:" + event.getY());
                break;
        }

        // Manage system levelFolder timeout here

        return result;
    }


    /**
     * Moves new assets to an external storyFolder so the Sphinx code can access it.
     *
     */
    class tutorConfigTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(Void... unused) {

            boolean result = false;

            try {
                // TODO: Don't do this in production
                // At the moment we always reinstall the tutor spec data - for development
                if(CacheSource.equals(TCONST.EXTERN)) {
                    tutorAssetManager.installAssets(TCONST.TUTORROOT);
                    logManager.postEvent_V(TAG, "INFO:Tutor Assets installed");
                }

                if(!tutorAssetManager.fileCheck(TCONST.LTK_PROJECT_ASSETS)) {
                    tutorAssetManager.installAssets(TCONST.LTK_PROJEXCTS);
                    logManager.postEvent_V(TAG, "INFO:LTK Projects installed");

                    // Note the Projects Zip file is anticipated to contain a storyFolder called "projects"
                    // containing the ltk data - this is unpacked to AndroidHost.APP_PRIVATE_FILES + TCONST.LTK_DATA_FOLDER
                    //
                    tutorAssetManager.extractAsset(TCONST.LTK_PROJEXCTS, TCONST.LTK_DATA_FOLDER);
                    logManager.postEvent_V(TAG, "INFO:LTK Projects extracted");
                }

                if(!tutorAssetManager.fileCheck(TCONST.LTK_GLYPH_ASSETS)) {
                    tutorAssetManager.installAssets(TCONST.LTK_GLYPHS);
                    logManager.postEvent_V(TAG, "INFO:LTK Glyphs installed");

                    // Note the Glyphs Zip file is anticipated to contain a storyFolder called "glyphs"
                    // containing the ltk glyph data - this is unpacked to AndroidHost.APP_PRIVATE_FILES + TCONST.LTK_DATA_FOLDER
                    //
                    tutorAssetManager.extractAsset(TCONST.LTK_GLYPHS, TCONST.LTK_DATA_FOLDER);
                    logManager.postEvent_V(TAG, "INFO:LTK Glyphs extracted");
                }

                // Find and install (move to ext_asset_path) any new or updated audio/story assets
                //
                tutorAssetManager.updateAssetPackages(EDFORGE_ASSET_PATTERN, AndroidHost.EXT_ASSET_PATH );

                result = true;

            } catch (IOException e) {
                // TODO: Manage exceptions
                e.printStackTrace();
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            isReady = result;

            onServiceReady("ROOT", result ? 1 : 0);
        }
    }


    /**
     * Callback used by services to announce ready state
     * @param serviceName
     */
    public void onServiceReady(String serviceName, int status) {

        logManager.postEvent_V(TAG, "onServiceReady:" + serviceName + "status:" + status);

        startEngine();
    }


    /**
     * Start the tutor engine once everything is intialized.
     *
     * There are several async init tasks and they all call this when they're finished.
     * The last one ready passes all the tests and starts the engine.
     *
     * TODO: Manage initialization failures
     *
     */
    private void startEngine() {

        if(!engineStarted) {
            engineStarted = true;

            logManager.postEvent_V(TAG, "TutorEngine:Starting");

            // If running without built-in home screen add a start screen
            //
            if(STANDALONE) {

                // TODO: This is a temporary log update mechanism - see below
                //
//                masterContainer.addAndShow(startView);
//                startView.startTapTutor();
                setFullScreen();
            }
            // Otherwise go directly to the sessionManager
            //
            else {
                onStartTutor();
            }

        }
        // Note that it is possible for the masterContainer to be recreated without the
        // engine begin destroyed so we must maintain sync here.
        else {
            logManager.postEvent_V(TAG, "TutorEngine:Restarting");
        }
    }


    // TODO: This is a temporary log update mechanism - see below
    //
    static public void setGUIDCallBack(IGuidView callBack) {

        guidCallBack = callBack;
    }


    // TODO: This is a temporary log update mechanism - see below
    //
    public void onStartTutor() {

//        logManager.postEvent_V(TAG, "LOG_GUID:" + LOG_ID );
        LOG_ID = CPreferenceCache.initLogPreference(this);

        setFullScreen();

        // Disable screen sleep while in a session
        //
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    /**
     * TODO: Manage the back button
     */
    @Override
    public void onBackPressed() {

        logManager.postEvent_V(TAG, "EdForge:onBackPressed");

        // Allow the screen to sleep when not in a session
        //
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setFullScreen();
    }



    /***  State Management  ****************/


    /**
     *
     */
    @Override
    protected void onStart() {

        super.onStart();

        // On-Screen
        logManager.postEvent_V(TAG, "EdForge:onStart");

        // We only want to run the engine start sequence once per onStart call
        //
        engineStarted = false;

        // Debug - determine platform dependent memory limit
        //
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        int memAvail       = am.getMemoryClass();

        logManager.postEvent_V(TAG, "AvailableMemory:" + memAvail);
    }


    /**
     *  requery DB Cursors here
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        logManager.postEvent_V(TAG, "EdForge:onRestart");
    }


    /**
     *  Deactivate DB Cursors here
     */
    @Override
    protected void onStop() {

        super.onStop();
        // Off-Screen
        logManager.postEvent_V(TAG, "EdForge:onStop");
    }


    /**
     * This callback is mostly used for saving any persistent state the activity is editing, to
     * present a "edit in place" model to the user and making sure nothing is lost if there are
     * not enough resources to start the new activity without first killing this one. This is also
     * a good place to do things like stop animations and other things that consume a noticeable
     * amount of CPU in order to make the switch to the next activity as fast as possible, or to
     * close resources that are exclusive access such as the camera.
     *
     */
    @Override
    protected void onPause() {

        super.onPause();
        logManager.postEvent_V(TAG, "EdForge:onPause");

        SharedPreferences.Editor prefs = getPreferences(Context.MODE_PRIVATE).edit();
    }


    /**
     *
     */
    @Override
    protected void onResume() {

        super.onResume();
        logManager.postEvent_V(TAG, "EdForge:onResume");

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        String restoredText = prefs.getString("text", null);

        if (restoredText != null) {
        }
    }


    /**
     * In general onSaveInstanceState(Bundle) is used to save per-instance state in the activity
     *
     * @param outState
     */
    @Override
    protected void onSaveInstanceState (Bundle outState) {

        super.onSaveInstanceState(outState);
        logManager.postEvent_V(TAG, "EdForge:onSaveInstanceState");

//        SharedPreferences prefs = AndroidHost.ACTIVITY.getPreferences(Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//
//        int assetFullOrdinal = prefs.getInt(assetName + TCONST.ASSET_RELEASE_VERSION, 0);
//        int assetIncrOrdinal = prefs.getInt(assetName + TCONST.ASSET_UPDATE_VERSION, 0);
//
//        editor.putInt(assetName + TCONST.ASSET_UPDATE_VERSION , mAssetObject.getVersionField(INDEX_UPDATE, TCONST.ASSET_UPDATE_VERSION));
//        editor.apply();
    }


    @Override
    protected void onDestroy() {

        logManager.postEvent_V(TAG, "EdForge:onDestroy");

        Log.v(TAG, "isfinishing:" + isFinishing());

        super.onDestroy();

        logManager.postDateTimeStamp(GRAPH_MSG, "EdForge:SessionEnd");
        logManager.stopLogging();
    }


    public void switchView(View target) {

        if(mCurrView != null)
            masterContainer.removeView(mCurrView);

        masterContainer.addAndShow(target);
        mCurrView = target;
    }


    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    class hostReceiver extends BroadcastReceiver {

        public void onReceive (Context context, Intent intent) {

            Log.d("homeReceiver", "Broadcast recieved: ");

            switch(intent.getAction()) {

            }
        }
    }

}
