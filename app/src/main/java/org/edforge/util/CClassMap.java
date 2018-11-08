package org.edforge.util;

import org.edforge.engine.TutorDesc;
import org.edforge.engine.UserData;

import java.util.HashMap;


public class CClassMap {

    static public HashMap<String, Class> classMap = new HashMap<String, Class>();

    //
    // This is used to map "type" (class names) used in json HashMap specs to real classes

    static {

        classMap.put("TutorDesc", TutorDesc.class);
        classMap.put("UserData", UserData.class);

        classMap.put("string", String.class);
        classMap.put("bool", Boolean.class);
        classMap.put("int", Integer.class);
        classMap.put("float", Float.class);
        classMap.put("byte", Byte.class);
        classMap.put("long", Long.class);
        classMap.put("short", Short.class);
        classMap.put("object", Object.class);
    }
}