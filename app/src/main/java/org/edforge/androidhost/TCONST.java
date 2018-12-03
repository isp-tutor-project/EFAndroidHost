//*********************************************************************************
//
//    Copyright(c) 2016-2017  Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package org.edforge.androidhost;

// global tutor constants

import android.content.Intent;

import org.edforge.engine.UserData;

import java.util.HashMap;

public class TCONST {


    public static final String DEBUG_USER_NAME   = "@DEBUG_USER";
    public static final int    DEBUG_TUTOR_NDX   = 0;
    public static final String DEBUG_INSTRUCTION = "";
    public static final String DEBUG_TUTOR_SCENE = "";

    public static final String EDFORGE_ASSET_FOLDER   = "/EdForge_ASSETS/";
    public static final String EDFORGE_FOLDER         = "/EdForge/";
    public static final String EDFORGE_UPDATE_FOLDER  = "/EdForge_UPDATE/";
    public static final String EDFORGE_LOG_FOLDER     = "/EdForge_LOG/";
    public static final String EDFORGE_DATA_FOLDER    = "/EdForge_DATA/";
    public static final String EDFORGE_DATA_TRANSFER  = "/EdForge_XFER/";
    public static final String EDFORGE_TUTOR_DATA     = "/EdForge/EFTutors/";

    public static final String USER_DATA            = "isp_userdata.json";

    public static final boolean APPEND     = true;
    public static final boolean REPLACE    = false;

    // WIFI constants
    public static final String WEP         = "WEP";
    public static final String WPA         = "WPA";
    public static final String OPEN        = "OPEN";

    // Server States
    public static final int START_STATE         = 0;
    public static final int COMMAND_WAIT        = 1;
    public static final int COMMAND_PACKET      = 2;
    public static final int PROCESS_COMMAND     = 3;

    public static final int COMMAND_SENDSTART   = 4;
    public static final int COMMAND_SENDDATA    = 5;
    public static final int COMMAND_SENDACK     = 6;

    public static final int COMMAND_RECVSTART   = 7;
    public static final int COMMAND_RECVDATA    = 8;
    public static final int COMMAND_RECVACK     = 9;


    public static final String PULL         = "PULL";
    public static final String PUSH         = "PUSH";
    public static final String INSTALL      = "INSTALL";

    public static final String EDFORGEZIPTEMP = "efztempfile.zip";

    public static final String DEFAULT_TUTOR_INSTR = "tutor_seq_dayone.json";

    public static final String DEBUG_USER_JSON  = "{\"userName\":\"KEVINWI_DEC_27\", \"currTutorNdx\":0, \"currScene\":\"Scene0\", \"instructionSeq\":\"tutor_seq_ted_baseline.json\"}";
    public static final String DEBUG_TUTOR_LIST = "";



//    sdcard/robotutor_assets/assets/audio/en/cmu/xprize/activity_selector/d39950ec96e6a5361508996ce7ae6444.mp3

    // These features are based on the current tutor selection model
    // When no tutor has been selected it should run the tutor select
    // and when it finishes it should run the difficulty select until
    // the user wants to select another tutor.
    //

    public static final String FTR_TUTOR_SELECT       = "FTR_TUTOR_SELECT";
    public static final String FTR_DIFFICULTY_ASSESS  = "FTR_DIFFICULTY_ASSESS";
    public static final String FTR_DEBUG_SELECT       = "FTR_DEBUG_SELECT";
    public static final String FTR_DEBUG_LAUNCH       = "FTR_DEBUG_LAUNCH";

    public static final String SKILL_WRITING    = "letters";
    public static final String SKILL_STORIES    = "stories";
    public static final String SKILL_MATH       = "numbers";
    public static final String SKILL_SHAPES     = "shapes";
    public static final String FINISH           = "FINISH";
    public static final String SELECTOR_MODE    = "SELECTOR_MODE";

    public static final String TUTOR_SELECTED   = "TUTOR_SELECTED";
    public static final String SKILL_SELECTED   = "SKILL_SELECTED";
    public static final String DEBUG_LAYOUT     = "DEBUG_LAYOUT";
    public static final String START_SESSION    = "org.EdForge.START_SESSION";
    public static final String TUTOR_COMPLETE   = "org.EdForge.TUTOR_COMPLETE";




    // Version spec Index meaning 0.1.2.3
    // Given 4.23.9.8
    // Major release 4 | Feature release 23 | Fix release 9 | compatible Asset Version 8
    //
    public static final int MAJOR_VERSION   = 0;
    public static final int FEATURE_RELEASE = 1;
    public static final int FIX_RELEASE     = 2;
    public static final int ASSET_VERSION   = 3;


    // Spec elements for asset zip files releases
    // For assets to be compatible with Robotutor the ASSET_CODE_VERSION must match the ASSET_VERSION
    //
    public static final int ASSET_CODE_VERSION    = 0;
    public static final int ASSET_RELEASE_VERSION = 1;
    public static final int ASSET_UPDATE_VERSION  = 2;


    // These represent the base name for assets delivered in Zip files and loaded
    // through calls to updateZipAsset

    // They will arrive in files named - RoboTutor_AssetA.0.1.0.zip
    //
    public static final String EDFORGE_ASSET_PATTERN = "rtasset_";

    public static final String DATA_PATH        = "data";


    public static final String FONT_FOLDER     = "fonts/";

    public static final String START_PROGRESSIVE_UPDATE   = "START_PROGRESSIVE_UPDATE";
    public static final String START_INDETERMINATE_UPDATE = "START_INDETERMINATE_UPDATE";
    public static final String UPDATE_PROGRESS            = "UPDATE_PROGRESS";
    public static final String PROGRESS_TITLE             = "PROGRESS_TITLE";
    public static final String PROGRESS_MSG1              = "PROGRESS_MSG1";
    public static final String PROGRESS_MSG2              = "PROGRESS_MSG2";
    public static final String ASSET_UPDATE_MSG           = "Installing Assets: ";
    public static final String PLEASE_WAIT                = " - Please Wait.";
    public static final String LAUNCH_TUTOR               = "LAUNCH_TUTOR";

    public static final String INT_FIELD                  = "INT_FIELD";
    public static final String NAME_FIELD                 = "NAME_FIELD";
    public static final String USER_FIELD                 = "USER_FIELD";

    // Core log message types - anumation scenegraph and queued scenegraph
    //
    public static final String GRAPH_MSG                  = "RTag";

    static public HashMap<String, Integer> colorMap = new HashMap<String,Integer>();
    //
    // This is used to map "states" to colors

    static {
        colorMap.put(TCONST.COLORWRONG,  new Integer(0xFFFF0000));
        colorMap.put(TCONST.COLORERROR,  new Integer(0x44000000));
        colorMap.put(TCONST.COLORWARNING,new Integer(0xFFFFFF00));
        colorMap.put(TCONST.COLORRIGHT,  new Integer(0xff0000ff));
        colorMap.put(TCONST.COLORNORMAL, new Integer(0xff000000));
        colorMap.put(TCONST.COLORNONE,   new Integer(0x00000000));
    }

    public static final String COLORINDET          = "indeterminate";
    public static final String COLORWRONG          = "wrong";
    public static final String COLORWARNING        = "warning";
    public static final String COLORRIGHT          = "right";
    public static final String COLORERROR          = "error";
    public static final String COLORNORMAL         = "normal";
    public static final String COLORNONE           = "none";


    static public HashMap<String, String> fontMap = new HashMap<String, String>();

    static {
        fontMap.put("grundschrift",         FONT_FOLDER + "Grundschrift.ttf");
        fontMap.put("grundschrift-kontur",  FONT_FOLDER + "Grundschrift-Kontur.otf");
        fontMap.put("grundschrift-punkt",   FONT_FOLDER + "Grundschrift-Punkt.otf");
    }


    //*** Reading Tutor compatible string combinations

    static public HashMap<String, String> numberMap = new HashMap<String, String>();

    static {
        numberMap.put("LANG_EN", "AND,ZERO,ONE,TWO,THREE,FOUR,FIVE,SIX,SEVEN,EIGHT,NINE,TEN,ELEVEN,TWELVE,THIRTEEN,FORTEEN,FIFTEEN,SIXTEEN,SEVENTEEN,EIGHTEEN,NINETEEN,TWENTY,THIRTY,FORTY,FIFTY,SIXTY,SEVENTY,EIGHTY,NINETY,HUNDRED,THOUSAND,MILLION,BILLION,TRILLION,QUADRILLION");
        numberMap.put("LANG_SW", "NA,SIFURI,MOJA,MBILI,TATU,NNE,TANO,SITA,SABA,NANE,TISA,KUMI,ISHIRINI,THELATHINI,AROBAINI,HAMSINI,SITINI,SABINI,THEMANINI,TISINI,MIA,ELFU,MILIONI,BILIONI,TRILIONI,KWADRILIONI");
    }


    // This is used to map "language features" to the story resources
    // these are located in the assets/<lang>
    // Note: on Android these are case sensitive filenames

    static public HashMap<String, String> langMap = new HashMap<String, String>();

    public static final String LANG_AUTO   = "LANG_AUTO";
    public static final String LANG_EFFECT = "LANG_EFFECT";
    public static final String LANG_EN     = "LANG_EN";
    public static final String LANG_SW     = "LANG_SW";
    public static final String MEDIA_STORY = "story";

    // This maps features to 2 letter codes used to build filepaths.
    static {
        langMap.put(LANG_EFFECT, "effect");
        langMap.put(LANG_EN,     "en");
        langMap.put(LANG_SW,     "sw");
    }

    // JSON parameter constants


    // Loader Constants
    static final public String TUTORROOT          = "tutors";
    static final public String LTK_PROJECT_ASSETS = "projects";
    static final public String LTK_GLYPH_ASSETS   = "glyphs";

    static final public String LTK_PROJEXCTS      = "projects.zip";
    static final public String LTK_GLYPHS         = "glyphs.zip";
    static final public String LTK_DATA_FOLDER    = "/";                // should terminate in path sep '/'

    // data sources
    public static final String ASSETS          = "ASSETS";
    public static final String RESOURCES       = "RESOURCE";
    public static final String EXTERN          = "EXTERN";
    public static final String DEFINED         = "DEFINED";


    public static final int TIMEDSTART_EVENT   = 0x01;
    public static final int TIMEDSILENCE_EVENT = 0x02;
    public static final int TIMEDSOUND_EVENT   = 0x04;
    public static final int TIMEDWORD_EVENT    = 0x08;

    public static final int ALLTIMED_EVENTS    = 0x0F;

    public static final int SILENCE_EVENT      = 0x10;
    public static final int SOUND_EVENT        = 0x20;
    public static final int WORD_EVENT         = 0x40;

    public static final int ALL_EVENTS         = 0xFFFFFFFF;


    public static final String ASR_TIMED_START_EVENT    = "ASR_TIMED_START_EVENT";
    public static final String ASR_RECOGNITION_EVENT    = "ASR_RECOGNITION_EVENT";
    public static final String ASR_ERROR_EVENT          = "ASR_ERROR_EVENT";
    public static final String ASR_SILENCE_EVENT        = "ASR_SILENCE_EVENT";
    public static final String ASR_SOUND_EVENT          = "ASR_SOUND_EVENT";
    public static final String ASR_WORD_EVENT           = "ASR_WORD_EVENT";
    public static final String ASR_TIMEDSILENCE_EVENT   = "ASR_TIMEDSILENCE_EVENT";
    public static final String ASR_TIMEDSOUND_EVENT     = "ASR_TIMEDSOUND_EVENT";
    public static final String ASR_TIMEDWORD_EVENT      = "ASR_TIMEDWORD_EVENT";
    public static final String UTTERANCE_COMPLETE_EVENT = "UTTERANCE_COMPLETE_EVENT";

    public static final String ASR_ALL_TIMED_EVENTS   = "ASR_ALL_TIMED_EVENTS";
    public static final String ASR_ALL_STATIC_EVENTS  = "ASR_ALL_STATIC_EVENTS";
    public static final String ASR_ALL_EVENTS         = "ASR_ALL_EVENTS";

    // Map script event names to bitmap ASR Listener conatants.
    //
    static public HashMap<String, Integer> ASREventMap = new HashMap<String, Integer>();

    static {
        ASREventMap.put(ASR_SILENCE_EVENT, TCONST.SILENCE_EVENT);
        ASREventMap.put(ASR_SOUND_EVENT, TCONST.SOUND_EVENT);
        ASREventMap.put(ASR_WORD_EVENT, TCONST.WORD_EVENT);
        ASREventMap.put(ASR_TIMEDSILENCE_EVENT, TCONST.TIMEDSILENCE_EVENT);
        ASREventMap.put(ASR_TIMEDSOUND_EVENT, TCONST.TIMEDSOUND_EVENT);
        ASREventMap.put(ASR_TIMEDWORD_EVENT, TCONST.TIMEDWORD_EVENT);
        ASREventMap.put(ASR_TIMED_START_EVENT, TCONST.TIMEDSTART_EVENT);

        ASREventMap.put(ASR_ALL_TIMED_EVENTS,TCONST.ALLTIMED_EVENTS);
        ASREventMap.put(ASR_ALL_STATIC_EVENTS,TCONST.ALL_EVENTS);
        ASREventMap.put(ASR_ALL_EVENTS,TCONST.ALL_EVENTS);
    }


    public static final String TEXT_FIELD       = ".text";

    public static final String EFHOST_FINISHER_INTENT   = "org.edforge.androidhost.EFHOST_FINISHER_INTENT";
    public static final String EFHOME_FINISHER_INTENT   = "org.edforge.efhomescreen.EFHOME_FINISHER_INTENT";
    public static final String EFHOME_STARTER_INTENT    = "org.edforge.efhomescreen.EFHOME_STARTER_INTENT";

    public static final String PLUG_CONNECT     = "android.intent.action.ACTION_POWER_CONNECTED";
    public static final String PLUG_DISCONNECT  = "android.intent.action.ACTION_POWER_DISCONNECTED";

    public static final String EFOWNER_LAUNCH_INTENT  = "org.edforge.efdeviceowner.EF_DEVICE_OWNER";
    public static final String EFHOME_LAUNCH_INTENT   = "org.edforge.efhomescreen.EF_HOME_SCREEN";
    public static final String EFHOST_LAUNCH_INTENT   = "org.edforge.androidhost.EF_ANDROID_HOST";
    public static final String LAUNCH_HOME            = Intent.ACTION_MAIN;

}
