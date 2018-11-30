package org.edforge.engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.edforge.androidhost.TCONST;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

import static org.edforge.androidhost.TCONST.DEBUG_USER_JSON;
import static org.edforge.androidhost.TCONST.DEFAULT_TUTOR_INSTR;
import static org.edforge.androidhost.TCONST.EDFORGE_DATA_FOLDER;
import static org.edforge.androidhost.TCONST.EDFORGE_FOLDER;
import static org.edforge.androidhost.TCONST.EDFORGE_TUTOR_DATA;
import static org.edforge.androidhost.TCONST.LAUNCH_TUTOR;
import static org.edforge.androidhost.TCONST.REPLACE;
import static org.edforge.androidhost.TCONST.TUTOR_COMPLETE;

/**
 * Created by kevin on 11/6/2018.
 */

public class UserManager {

    private Context             mContext;
    private UserDataPackage     mUserDataPackage;
    private String              mUser;
    private UserData            mUserData;

    private String              mBasePath;
    private String              mUserInfoPath;
    private String              mTutorDataPath;

    private TutorList           mTutors;
    private boolean             mInitialized = false;

    private LocalBroadcastManager bManager;
    private userReceiver          bReceiver;
    private JSON_Util             mJsonWriter = null;

    private final  String  TAG = "UserManager";

// TODO: Examine other ways to manage singletons that don't have this problem of JAVA class persistance
// TODO: where we may get a singleton instance that was initialized in another invocation of an activity
// TODO: And it may have been partially torn down as well.


    private static UserManager ourInstance = new UserManager();

    public static UserManager getInstance() {
        return ourInstance;
    }

    private UserManager() {

        mBasePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public void init(Context context) {

        String jsonData;

        if(!mInitialized) {

            mInitialized = true;
            mContext     = context;
            mJsonWriter  = new JSON_Util();

            // Capture the local broadcast manager
            bManager = LocalBroadcastManager.getInstance(mContext);

            IntentFilter filter = new IntentFilter(LAUNCH_TUTOR);

            bReceiver = new userReceiver();
            bManager.registerReceiver(bReceiver, filter);
        }

        // Always Load the user data file
        //
        mUserInfoPath    = mBasePath + EDFORGE_DATA_FOLDER + TCONST.USER_DATA;
        mUserDataPackage = new UserDataPackage();

        jsonData = JSON_Helper.cacheDataByName(mUserInfoPath);

        try {
            if (!jsonData.isEmpty())
                mUserDataPackage.loadJSON(new JSONObject(jsonData), null);

        } catch (Exception e) {

            // TODO: Manage Exceptions
            Log.e(TAG, "UserData Parse Error: " + e);
        }
    }


    public void onDestroy() {

        mInitialized = false;

        if(bManager != null && bReceiver != null)
            bManager.unregisterReceiver(bReceiver);

        bManager  = null;
        bReceiver = null;
    }


    private void updateUserPackage() {

        mJsonWriter.write(mUserDataPackage, mUserInfoPath, REPLACE);
    }


    private void loadTutorDesc() {

        String jsonData;

        mTutorDataPath = mBasePath + EDFORGE_FOLDER + mUserData.instructionSeq;

        // Load the Instruction Description file
        //
        mTutors = new TutorList();
        jsonData  = JSON_Helper.cacheDataByName(mTutorDataPath);

        try {
            if(!jsonData.isEmpty())
                mTutors.loadJSON(new JSONObject(jsonData), null);

        } catch (Exception e) {

            // TODO: Manage Exceptions
            Log.e(TAG, "TutorList Parse Error: " + e);
        }
    }
    public void initUser(String user) {

        mUser     = user;
        mUserData = mUserDataPackage.getUserByName(mUser);

        Log.d(TAG, "Launch UserData: " + mUserData);

        initEmptyInstruction();
        loadTutorDesc();

        prepLogs();
    }
    public void initDebugUser() {

        mUserData = new UserData();

        try {
            mUserData.loadJSON(new JSONObject(DEBUG_USER_JSON), null);

        } catch (Exception e) {

            // TODO: Manage Exceptions
            Log.e(TAG, "TutorList Parse Error: " + e);
        }

        mUser     = mUserData.userName;
        mUserData = mUserDataPackage.setDebugUser(mUserData);

        initEmptyInstruction();
        loadTutorDesc();

        prepLogs();
    }
    public void initEmptyInstruction() {

        if(mUserData.instructionSeq.equals("")) {
            mUserData.instructionSeq = DEFAULT_TUTOR_INSTR;
            updateUserPackage();
        }
    }

    private void prepLogs() {

        String LogDataPath = mBasePath + EDFORGE_DATA_FOLDER + getUserPath();
        File userFolder = new File(LogDataPath);

        if (!userFolder.exists())
                userFolder.mkdir();

        for(int i1 = 0 ; i1 < mTutors.size() ; i1++) {

            File tutorFolder = new File(LogDataPath + "/" + mTutors.logFolder(i1));

            if (!tutorFolder.exists())
                    tutorFolder.mkdir();
        }
    }


    public boolean hasMoreTutors() {

        return mTutors.hasMoreTutors(mUserData.currTutorNdx);
    }

    public String getTutorFileName() {

        return mTutors.launcher(mUserData.currTutorNdx);
    }

    public void broadcast(String Action) {

        Intent msg = new Intent(Action);

        bManager.sendBroadcast(msg);
    }

    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    class userReceiver extends BroadcastReceiver {

        public void onReceive (Context context, Intent intent) {

            Log.d("homeReceiver", "Broadcast recieved: ");

            switch(intent.getAction()) {

            }
        }
    }


    //******************************************************************************************
    //******************************************************************************************


    @android.webkit.JavascriptInterface
    public void updateScene(String sceneid) {

        Log.i(TAG, "LJSCR Updating Current Scene: " + sceneid);
        FileWriter fileWriter;

        mUserData.currScene = sceneid;

        if(sceneid.toLowerCase().equals("ssceneend")) {
            tutorComplete();
        }
        else {
            updateUserPackage();
        }
    }


    @android.webkit.JavascriptInterface
    public void tutorComplete() {

        Log.i(TAG, "LJSCR Tutor Complete: ");

        mUserData.currScene = "";
        mUserData.currTutorNdx++;

        updateUserPackage();
        broadcast(TUTOR_COMPLETE);
    }


    @android.webkit.JavascriptInterface
    public String getUserId() {

        Log.i(TAG, "LJSCR get Uer ID: ");

        return getUserPath();
    }


    @android.webkit.JavascriptInterface
    public String getFeatures() {

        Log.i(TAG, "LJSCR getFeatures: ");

        return mTutors.features(mUserData.currTutorNdx);
    }


    @android.webkit.JavascriptInterface
    public String getCurrentScene() {

        return mUserData.currScene;
    }


    private String getUserPath() {

        return mUser.replace("-","_").toUpperCase();
    }

    private String getPreviousLogPath() {

        String prevLogPath = "";

        if (mUserData.currTutorNdx > 0) {

            int prevTutorNdx = mUserData.currTutorNdx - 1;
            prevLogPath = mBasePath + EDFORGE_DATA_FOLDER + getUserPath() +  mTutors.logFolder(prevTutorNdx);
        }
        else {
            prevLogPath = null;
        }

        return prevLogPath;
    }

    private String getCurrentLogPath() {
        return mBasePath + EDFORGE_DATA_FOLDER + getUserPath() + "/" + mTutors.logFolder(mUserData.currTutorNdx);
    }

    @android.webkit.JavascriptInterface
    public void logState(String scenename, String scene, String module,String tutor) {

        Log.i(TAG, "LJSCR logState: ");

        FileWriter logWriter;

        String buffer = "{\"scene\":" + scene + "," + "\"module\":" + module + "," + "\"tutor\":" + tutor + "}";

        try {
            logWriter = new FileWriter(getCurrentLogPath() + "/" + scenename + ".json");

            logWriter.write(buffer);
            logWriter.close();
        }
        catch(Exception e) {
            Log.e(TAG, "Log Write Failed : " + e);
        }
    }


    // Note the Tutor ID set in the tutorconfig.json sets the state that should be used with the
    // tutor - allows tutors to share state - i.e. as a continuous instruction sequence
    //
    private String getTutorStatePath(String tutorID) {

        return mBasePath + EDFORGE_DATA_FOLDER + getUserPath() + "/" + "tutorstate_" + tutorID + ".json";
    }

    @android.webkit.JavascriptInterface
    public void updateTutorState(String tutorID, String tutorStateJSON) {

        Log.i(TAG, "LJSCR update Tutor State: ");

        FileWriter logWriter;

        try {
            logWriter = new FileWriter(getTutorStatePath(tutorID));

            logWriter.write(tutorStateJSON);
            logWriter.close();
        }
        catch(Exception e) {
            Log.e(TAG, "Log Write Failed : " + e);
        }
    }


    @android.webkit.JavascriptInterface
    public String getTutorState(String tutorID) {

        Log.i(TAG, "LJSCR getTutorState: ");

        String jsonData  = JSON_Helper.cacheDataByName(getTutorStatePath(tutorID));

        return jsonData;
    }

}
