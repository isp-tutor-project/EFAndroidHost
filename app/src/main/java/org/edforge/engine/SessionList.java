package org.edforge.engine;

import org.edforge.util.CClassMap;
import org.edforge.util.IScope;
import org.edforge.util.ISerializableObject;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static org.edforge.androidhost.TCONST.EDFORGE_DATA_FOLDER;

/**
 * Created by kevin on 11/6/2018.
 */

public class SessionList implements ISerializableObject {

    private ArrayList<TutorList> sessionsArray;
    private TutorList            mCurrTutors;

    // json loadable
    public String comment;
    public TutorList[] sessions;


    public SessionList() {

        sessions      = new TutorList[0];
        sessionsArray = new ArrayList<TutorList>();
    }


    public boolean hasMoreSessions(UserData userData) {

        boolean result = false;

        if(userData.currSessionNdx < sessions.length) {

            result = hasMoreTutors(userData);
        }

        return result;
    }

    public boolean hasMoreTutors(UserData userData) {

        mCurrTutors = getSessionByIndex(userData.currSessionNdx);

        return mCurrTutors.hasMoreTutors(userData.currTutorNdx);
    }

    public TutorList getSessionByIndex(int index) {

        return sessionsArray.get(index);
    }

    public TutorDesc[] tutor(int index) {

        return sessionsArray.get(index).tutors;
    }

    public int size() {
        
        return sessionsArray.size();
    }



    public String launcher(UserData userData) {

        mCurrTutors = getSessionByIndex(userData.currSessionNdx);
        return mCurrTutors.launcher(userData.currTutorNdx);
    }

    public String logFolder(UserData userData) {

        mCurrTutors = getSessionByIndex(userData.currSessionNdx);
        return mCurrTutors.logFolder(userData.currTutorNdx);
    }

    public String features(UserData userData) {

        mCurrTutors = getSessionByIndex(userData.currSessionNdx);
        return mCurrTutors.features(userData.currTutorNdx);
    }


    public void preCreateLogFolders(UserData userData, String LogDataPath) {

        mCurrTutors = getSessionByIndex(userData.currSessionNdx);

        for(int i1 = 0; i1 < mCurrTutors.size() ; i1++) {

            File tutorFolder = new File(LogDataPath + "/" + mCurrTutors.logFolder(i1));

            if (!tutorFolder.exists())
                    tutorFolder.mkdir();
        }
    }


    @Override
    public void saveJSON(JSON_Util jsonWriter) {

        UserData[] outputData = sessionsArray.toArray(new UserData[sessionsArray.size()]);

        jsonWriter.addElement("comment", this.comment);
        jsonWriter.addObjectArray("sessions", outputData);

    }

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);

        sessionsArray = new ArrayList<TutorList>(Arrays.asList(sessions));
    }
}
