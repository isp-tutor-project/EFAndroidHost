package org.edforge.engine;

import android.util.Log;

import org.edforge.util.CClassMap;
import org.edforge.util.IScope;
import org.edforge.util.ISerializableObject;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

/**
 *  NOTE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 *  This must be aligned with class in EFHomeScreen
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
    }

    public UserData(String _userName) {

        userName        = _userName;
        currSessionNdx  = 0;
        currTutorNdx    = 0;
        currScene       = "";
        instructionSeq  = "";
        timeStamp       = System.currentTimeMillis();
    }


    public void SetDefInstruction(String defaultInstr) {

        instructionSeq = defaultInstr;
    }

    public void clone(UserData userData) {

        userName        = userData.userName;
        currSessionNdx  = userData.currSessionNdx;
        currTutorNdx    = userData.currTutorNdx;
        currScene       = userData.currScene;
        instructionSeq  = userData.instructionSeq;
        timeStamp       = userData.timeStamp;
    }


    @Override
    public void saveJSON(JSON_Util writer) {

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
    }

}
