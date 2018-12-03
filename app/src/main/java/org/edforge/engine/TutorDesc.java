package org.edforge.engine;

import org.edforge.util.CClassMap;
import org.edforge.util.IScope;
import org.edforge.util.ISerializableObject;
import org.edforge.util.JSON_Helper;
import org.edforge.util.JSON_Util;
import org.json.JSONObject;

/**
 * Created by kevin on 11/6/2018.
 */

public class TutorDesc implements ISerializableObject {


    // json loadable
    public String       launcher;
    public String       logfolder;
    public String       features;


    @Override
    public void saveJSON(JSON_Util jsonWriter) {

        // Define the launcher: the html filename that starts the instruction
        // The features to push into the tutor instance.
        //
        jsonWriter.addElement("launcher", launcher);
        jsonWriter.addElement("logfolder", logfolder);
        jsonWriter.addElement("features", features);
    }

    @Override
    public void loadJSON(JSONObject jsonObj, IScope scope) {

        JSON_Helper.parseSelf(jsonObj, this, CClassMap.classMap, scope);
    }
}
