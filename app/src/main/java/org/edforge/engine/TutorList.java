package org.edforge.engine;

import org.edforge.util.CClassMap;
import org.edforge.util.IScope;
import org.edforge.util.ISerializableObject;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by kevin on 11/6/2018.
 */

public class TutorList implements ISerializableObject {

    private ArrayList<TutorDesc> tutorArray;

    // json loadable
    public TutorDesc[] tutors;


    public TutorList() {

        tutors     = new TutorDesc[0];
        tutorArray = new ArrayList<TutorDesc>();
    }

    public boolean hasMoreTutors(int index) {

        return (index < tutors.length);
    }

    public TutorDesc getTutorDescByIndex(int index) {

        return tutorArray.get(index);
    }

    public String logFolder(int index) {

        return tutorArray.get(index).logfolder;
    }
    
    public int size() {
        
        return tutorArray.size();
    }
    
    @Override
    public void saveJSON(JSON_Util jsonWriter) {

        UserData[] outputData = tutorArray.toArray(new UserData[tutorArray.size()]);

        jsonWriter.addObjectArray("tutors", outputData);

    }

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);

        tutorArray = new ArrayList<TutorDesc>(Arrays.asList(tutors));
    }
}
