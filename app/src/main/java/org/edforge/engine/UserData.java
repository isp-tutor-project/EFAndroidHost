package org.edforge.engine;

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


    final private String TAG = "UserData";

    // json loadable
    public String userName;
    public int    currTutorNdx;
    public String currScene;
    public String instructionSeq;


    public UserData() {

        userName        = "";
        currTutorNdx    = 0;
        currScene       = "";
        instructionSeq  = "";
    }

    public UserData(String _userName) {

        userName        = _userName;
        currTutorNdx    = 0;
        currScene       = "";
        instructionSeq  = "";
    }

    public UserData(String _userName, int _currTutorNdx, String _currScene, String _instructionSeq) {

        userName        = _userName;
        currTutorNdx    = _currTutorNdx;
        currScene       = _currScene;
        instructionSeq  = _instructionSeq;
    }



    public void clone(UserData userData) {

        userName        = userData.userName;
        currTutorNdx    = userData.currTutorNdx;
        currScene       = userData.currScene;
        instructionSeq  = userData.instructionSeq;
    }


    @Override
    public void saveJSON(JSON_Util writer) {

        writer.addElement("userName", userName);
        writer.addElement("currTutorNdx", currTutorNdx);
        writer.addElement("currScene", currScene);
        writer.addElement("instructionSeq", instructionSeq);
    }

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);
    }

}
