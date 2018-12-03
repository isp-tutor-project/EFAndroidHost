package org.edforge.engine;

import android.util.Log;

import org.edforge.util.CClassMap;
import org.edforge.util.IScope;
import org.edforge.util.ISerializableObject;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

/**
 * Created by kevin on 11/4/2018.
 */

public class UserData implements ISerializableObject {


    final private String TAG = "EFUserData";

    // json loadable
    public String userName;
    public int    currSessionNdx;
    public int    currTutorNdx;
    public String currScene;
    public String instructionSeq;
    public long   timeStamp;


    public UserData() {

        userName        = "";
        currSessionNdx  = 0;
        currTutorNdx    = 0;
        currScene       = "";
        instructionSeq  = "";
        timeStamp       = System.currentTimeMillis();

        Log.e(TAG, "UserData null");
    }

    public UserData(String _userName) {

        userName        = _userName;
        currSessionNdx  = 0;
        currTutorNdx    = 0;
        currScene       = "";
        instructionSeq  = "";
        timeStamp       = System.currentTimeMillis();

        if(userName.equals("GUESTBL_JAN_1"))
            Log.e(TAG, "UserData named");
    }


    public void clone(UserData userData) {

        userName        = userData.userName;
        currSessionNdx  = userData.currSessionNdx;
        currTutorNdx    = userData.currTutorNdx;
        currScene       = userData.currScene;
        instructionSeq  = userData.instructionSeq;
        timeStamp       = userData.timeStamp;

        if(userName.equals("GUESTBL_JAN_1"))
            Log.e(TAG, "UserData clone: " + currSessionNdx + ":" + currTutorNdx);
    }


    @Override
    public void saveJSON(JSON_Util writer) {

        if(userName.equals("GUESTBL_JAN_1"))
            Log.e(TAG, "UserData write: " + currSessionNdx + ":" + currTutorNdx);

        writer.addElement("userName",       userName);
        writer.addElement("currSessionNdx", currSessionNdx);
        writer.addElement("currTutorNdx",   currTutorNdx);
        writer.addElement("currScene",      currScene);
        writer.addElement("instructionSeq", instructionSeq);
        writer.addElement("timeStamp",      timeStamp);

    }


    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);

        userName = userName.replace("-","_").toUpperCase();

        if(userName.equals("GUESTBL_JAN_1"))
            Log.e(TAG, "UserData load: " + currSessionNdx + ":" + currTutorNdx);

    }

}
