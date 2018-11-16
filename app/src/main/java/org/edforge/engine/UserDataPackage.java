package org.edforge.engine;

import android.util.Log;

import org.edforge.util.CClassMap;
import org.edforge.util.IScope;
import org.edforge.util.ISerializableObject;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by kevin on 11/4/2018.
 */

public class UserDataPackage implements ISerializableObject {

    private ArrayList<UserData> userArray;

    // json loadable
    public UserData   currUser;
    public UserData[] users;

    public int        mUserNdx;

    final private String TAG = "UserDataPackage";

    public UserDataPackage() {

        currUser  = new UserData();
        userArray = new ArrayList<UserData>();
    }


    public UserData setDebugUser(UserData debugUser) {

        UserData tDebugUser;

        currUser  = debugUser;
        tDebugUser = getUserByName(currUser.userName);

        if(tDebugUser != null) {

            tDebugUser.clone(debugUser);
        }
        else {
            addUser(debugUser);
        }

        return currUser;
    }


    public void addUser(String userName) {

        UserData temp = new UserData(userName);

        addUser(temp);
    }


    public void addUser(UserData userData) {

        userArray.add(userData);
        getUserIndexByName(userData.userName);
    }


    public UserData getUserByNdx(int Index) {

        mUserNdx = Index;

        return userArray.get(mUserNdx);
    }


    public UserData getUserByName(String userName) {

        UserData user;

        getUserIndexByName(userName);

        if(mUserNdx != -1)
            user = userArray.get(mUserNdx);
        else
            user = null;

        return user;
    }


    public int getUserIndexByName(String userName) {

        UserData user;
        mUserNdx = -1;

        for(int i1 = 0 ; i1 < userArray.size() ; i1++) {

            //#TEST
            // Log.d(TAG, "Launch User: " + userArray.get(i1).userName);

            if (userArray.get(i1).userName.equals(userName)) {
                mUserNdx = i1;
                break;
            }
        }

        return mUserNdx;
    }



    @Override
    public void saveJSON(JSON_Util jsonWriter) {

        jsonWriter.addElement("currUser", currUser);

        UserData[] outputData = userArray.toArray(new UserData[userArray.size()]);

        jsonWriter.addObjectArray("users", outputData);
    }

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);

        userArray = new ArrayList<UserData>(Arrays.asList(users));
    }
}

