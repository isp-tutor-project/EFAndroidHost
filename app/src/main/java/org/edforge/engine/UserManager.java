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
import static org.edforge.androidhost.TCONST.DEFAULT_TUTOR_INSTR;
import static org.edforge.androidhost.TCONST.EDFORGE_DATA_FOLDER;
import static org.edforge.androidhost.TCONST.EDFORGE_FOLDER;
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

    private LocalBroadcastManager bManager;
    private userReceiver          bReceiver;
    private JSON_Util             mJsonWriter = null;

    private final  String  TAG = "UserManager";



    private static UserManager ourInstance = new UserManager();

    public static UserManager getInstance() {
        return ourInstance;
    }

    private UserManager() {

        mBasePath = Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public void init(Context context) {

        String jsonData;

        mContext = context;
        mJsonWriter = new JSON_Util();

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(mContext);

        IntentFilter filter = new IntentFilter(LAUNCH_TUTOR);

        bReceiver = new userReceiver();
        bManager.registerReceiver(bReceiver, filter);


        // Load the user data file
        //
        mUserInfoPath = mBasePath + EDFORGE_DATA_FOLDER + TCONST.USER_DATA;
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

        String LogDataPath = mBasePath + EDFORGE_DATA_FOLDER + mUser.replace("-","_");
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

        return mTutors.getTutorDescByIndex(mUserData.currTutorNdx).launcher;
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

        updateUserPackage();

        if(sceneid.toLowerCase().equals("ssceneend")) {
            tutorComplete();
        }
    }


    @android.webkit.JavascriptInterface
    public void tutorComplete() {

        Log.i(TAG, "LJSCR Tutor Complete: ");

        mUserData.currTutorNdx++;

        updateUserPackage();
        broadcast(TUTOR_COMPLETE);
    }

    private String getLogPath() {
        return mBasePath + EDFORGE_DATA_FOLDER + mUser.replace("-","_") +  mTutors.logFolder(mUserData.currTutorNdx);
    }

    @android.webkit.JavascriptInterface
    public void logState(String scenename, String scene, String module,String tutor) {

        Log.i(TAG, "LJSCR logState: ");

        FileWriter logWriter;

        String buffer = "{\"scene\":" + scene + "," + "\"module\":" + module + "," + "\"tutor\":" + tutor + "}";

        try {
            logWriter = new FileWriter(getLogPath() + "/" + scenename + ".json");

            logWriter.write(buffer);
            logWriter.close();
        }
        catch(Exception e) {

        }
    }

}
