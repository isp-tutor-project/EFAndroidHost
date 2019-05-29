package org.edforge.engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import static org.edforge.androidhost.TCONST.EDFORGE_DATA_FOLDER;
import static org.edforge.androidhost.TCONST.EDFORGE_FOLDER;
import static org.edforge.androidhost.TCONST.INSTR_CONFIG;
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
    private String              mSessionPath;

    private SessionList         mSessions;
    private boolean             mInitialized = false;

    private LocalBroadcastManager bManager;
    private userReceiver          bReceiver;
    private JSON_Util             mJsonWriter = null;

    private InstructionConfig mInstructionConfig;
    public String DEF_INSTRUCTION_SEQ = "";

    public final static String  ASSET_FOLDER   = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_FOLDER;

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

        mSessionPath = mBasePath + EDFORGE_FOLDER + mUserData.instructionSeq;

        // Load the Instruction Description file
        //
        mSessions = new SessionList();
        jsonData  = JSON_Helper.cacheDataByName(mSessionPath);

        try {
            if(!jsonData.isEmpty())
                mSessions.loadJSON(new JSONObject(jsonData), null);

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
    }


    private void loadInstructionConfig() {

        // Load the user data file
        //
        mInstructionConfig = new InstructionConfig();
        String jsonData  = JSON_Helper.cacheDataByName(ASSET_FOLDER + INSTR_CONFIG);

        try {
            if(!jsonData.isEmpty()) {
                mInstructionConfig.loadJSON(new JSONObject(jsonData), null);

                DEF_INSTRUCTION_SEQ = mInstructionConfig.defInstr;
            }

        } catch (Exception e) {

            // TODO: Manage Exceptions
            Log.e(TAG, "UserData Parse Error: " + e);
        }
    }


    public void initEmptyInstruction() {

        loadInstructionConfig();

        if(mUserData.instructionSeq.equals("")) {
            mUserData.instructionSeq = DEF_INSTRUCTION_SEQ;
            updateUserPackage();
        }
    }

    public void preCreateLogFolders() {

        String LogDataPath = mBasePath + EDFORGE_DATA_FOLDER + getUserPath();
        File userFolder    = new File(LogDataPath);

        if (!userFolder.exists())
                userFolder.mkdir();

        mSessions.preCreateLogFolders(mUserData, LogDataPath);
    }


    public boolean hasMoreSessions() {

        mUserData.currSessionNdx++;
        mUserData.currTutorNdx = 0;
        mUserData.currScene    = "";

        updateUserPackage();

        return mSessions.hasMoreSessions(mUserData);
    }

    public boolean hasMoreTutors() {


        return mSessions.hasMoreTutors(mUserData);
    }

    public String getTutorFileName() {

        return mSessions.launcher(mUserData);
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
    public void updateScene(String sceneName, String sceneid) {

        Log.i(TAG, "LJSCR Updating Current Scene: " + sceneid);
        FileWriter fileWriter;

        mUserData.currScene = sceneid;

        if(sceneName.toLowerCase().equals("ssceneend")) {
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

        return mSessions.features(mUserData);
    }


    @android.webkit.JavascriptInterface
    public String getCurrentScene() {

        return mUserData.currScene;
    }


    private String getUserPath() {

        return mUser.replace("-","_").toUpperCase();
    }


    private String getCurrentLogPath() {
        return mBasePath + EDFORGE_DATA_FOLDER + getUserPath() + "/" + mSessions.logFolder(mUserData);
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
