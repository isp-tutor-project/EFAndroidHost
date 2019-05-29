package org.edforge.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;

import org.edforge.androidhost.TCONST;
import org.edforge.util.IReadyListener;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * TODO: this should be a singleton
 * TODO: Add stop / pause / restart
 */
public class CNativeSpeech extends UtteranceProgressListener implements OnInitListener
{
    private final Context context;

    private AppCompatActivity mOwner;
    private WebView           mWebView;

    private Locale          mLocale;
    private String          mCurrentLocale = "";
    private float           mCurrentRate   = 0;
    private TextToSpeech    tts;
    private boolean         readyToSpeak = false;
    private boolean         isSpeaking = false;

    private Set     mVoices;
    private Voice[] arrVoices;

    private IReadyListener  tutorRoot;

    private final Handler   mainHandler = new Handler(Looper.getMainLooper());
    private HashMap         queueMap    = new HashMap();
    private boolean         mDisabled   = false;

    private String          trackSSML   = "";
    private String          trackID     = "";


    static final String TAG="CNativeSpeech";



    public CNativeSpeech(Context baseContext, WebView webView ) {

        context  = baseContext;
        mWebView = webView;
    }

    /**
     * Attach a callback to the tutorRoot to announce
     * service availability
     *
     * @param callback
     */
    @android.webkit.JavascriptInterface
    public void initializeTTS(IReadyListener callback) {

        tutorRoot = callback;

        tts = new TextToSpeech(context, this);
        //tts = new TextToSpeech(context, this);
    }


    /**
     * TextToSpeech OnInitListener Callback
     * @param status
     */
    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            readyToSpeak = true;
            tutorRoot.onServiceReady(TCONST.TTS, "readyToSpeak");
        }
        else {

            tutorRoot.onServiceReady(TCONST.TTS, "Init Failed");
        }

    }


    /**
     * Sets the speech rate.
     *
     * This has no effect on any pre-recorded speech.
     */
    @android.webkit.JavascriptInterface
    public int setSpeechRate(float speechRate) {

        int result = TextToSpeech.SUCCESS;

        if(speechRate != 0 && mCurrentRate != speechRate) {
            result =  tts.setSpeechRate(speechRate);

            if(result == TextToSpeech.SUCCESS)
                    mCurrentRate = speechRate;
        }

        return result;
    }


    /**
     * used by tutor root to test service availability
     * @return
     */
    @android.webkit.JavascriptInterface
    public boolean isReady() {

        return readyToSpeak;
    }


    @android.webkit.JavascriptInterface
    public void registerSpeech(String SML, String TrackID) {

        trackSSML = SML;
        trackID   = TrackID;
    }


    @android.webkit.JavascriptInterface
    public void playTrack() {

        Log.d(TAG, trackSSML);

        speak(trackSSML, trackID);
    }


    /**
     * Set voice by feature string
     *
     * @param langFeature
     */
    @android.webkit.JavascriptInterface
    public void setLanguage(String langFeature) {

        if(langFeature != null && !langFeature.equals("") && mCurrentLocale != langFeature) {

            switch (langFeature) {
                case "LANG_SW":
                    mLocale = new Locale("swa", "TZA", "female;lxk");
                    break;

                case "LANG_EN":
                    mLocale = new Locale("en", "USA", "female;slt");
                    break;
            }
            tts.setLanguage(mLocale);

            mCurrentLocale = langFeature;
        }
    }


    @android.webkit.JavascriptInterface
    public void speak(String text, String UtterId) {

        if (readyToSpeak) {
            tts. setOnUtteranceProgressListener(this);

            tts.speak(text.toLowerCase(Locale.US), TextToSpeech.QUEUE_FLUSH, null, UtterId);
        }
    }


    @android.webkit.JavascriptInterface
    public void record(String text, File outfile, UtteranceProgressListener listener ) {

        if (readyToSpeak) {
            tts. setOnUtteranceProgressListener(listener);

            tts.synthesizeToFile(text, null, outfile, "recorder");
        }
    }


    @android.webkit.JavascriptInterface
    public void getVoices() {

        mVoices = tts.getVoices();

    //    arrVoices = (Voice[])mVoices.toArray();
    }


    @android.webkit.JavascriptInterface
    public void setVoice(String voice) {

        tts.setVoice(arrVoices[0]);
    }


    /**
     * Force stop all utterances and flush the queue
     *
     */
    @android.webkit.JavascriptInterface
    public void stopSpeaking() {

        if (readyToSpeak) {
            tts. stop();
        }
    }


    @android.webkit.JavascriptInterface
    public boolean isSpeaking(){

        // Can get a dead object here - ignore
        //
        try {
            isSpeaking = tts.isSpeaking();

            if(!isSpeaking) {

            }
        }
        catch(Exception ex) {
            Log.d(TAG, "Possible Dead Object: " + ex);

            isSpeaking = false;

        }

        return isSpeaking;
    }


    @android.webkit.JavascriptInterface
    public void shutDown(){
        tts.shutdown();
    }


    @android.webkit.JavascriptInterface
    @Override
    public void onStart(String utteranceId) {
        isSpeaking = true;
    }

    @android.webkit.JavascriptInterface
    @Override
    public void onDone(String utteranceId) {

        isSpeaking = false;
        post("complete");
    }

    @android.webkit.JavascriptInterface
    @Override
    public void onError(String utteranceId) {

        isSpeaking = false;
        post("complete");
    }


    //************************************************************************


    /**
     * This is the central processsing point of CSceneGraph - It is a message driven pattern
     * on the UI thread.
     */
    public class Queue implements Runnable {

        protected String _command;

        public Queue(String command) {
            _command = command;
        }

        @Override
        public void run() {

            try {
                queueMap.remove(this);

                switch (_command) {

                    // On completion fire the EFSoundEvent in the EFLoadManager (see the Adobe Animate linkage - ef_loadManager.js)
                    //
                    case "complete":
                        Log.d(TAG,"NativeSpeech Sending Completion Event");
                        mWebView.evaluateJavascript("javascript:EFSoundEvent('complete');", null);
                        break;
                }
            }
            catch(Exception e) {
            }
        }
    }


    /**
     *  Disable the input queue permenantly in prep for destruction
     *  walks the queue chain to diaable scene queue
     *
     */
    public void terminateQueue() {

        // disable the input queue permenantly in prep for destruction
        //
        mDisabled = true;
        flushQueue();
    }


    /**
     * Remove any pending media commands.
     *
     */
    private void flushQueue() {

        Iterator<?> tObjects = queueMap.entrySet().iterator();

        while(tObjects.hasNext() ) {
            Map.Entry entry = (Map.Entry) tObjects.next();

            mainHandler.removeCallbacks((CMediaPlayer.Queue)(entry.getValue()));
        }

    }

    /**
     * Keep a mapping of pending messages so we can flush the queue if we want to terminate
     * the tutor before it finishes naturally.
     *
     * @param qCommand
     */
    private void enQueue(CNativeSpeech.Queue qCommand) {

        if(!mDisabled) {
            queueMap.put(qCommand, qCommand);

            mainHandler.post(qCommand);
        }
    }

    /**
     * Post a command to this queue
     *
     * @param command
     */
    public void post(String command) {

        enQueue(new CNativeSpeech.Queue(command));
    }



}