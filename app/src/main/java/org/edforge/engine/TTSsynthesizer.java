package org.edforge.engine;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;

import org.edforge.util.IReadyListener;

import java.io.File;
import java.util.Locale;
import java.util.Set;

/**
 * TODO: this should be a singleton
 * TODO: Add stop / pause / restart
 */
public class TTSsynthesizer extends UtteranceProgressListener implements OnInitListener
{
    private final Context context;

    private AppCompatActivity mOwner;
    private WebView mWebView;

    private Locale mLocale;
    private String mCurrentLocale = "";
    private float           mCurrentRate   = 0;
    private TextToSpeech tts;
    private boolean         readyToSpeak = false;
    private boolean         isSpeaking = false;

    private Set mVoices;
    private Voice[] arrVoices;

    private IReadyListener  tutorRoot;

    static final String TAG="TTSsynthesizer";



    public TTSsynthesizer(Context baseContext, WebView webView ) {

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
        }
        else {
            // TODO: Manage Flite Not Present
        }

        tutorRoot.onServiceReady("TTS", readyToSpeak? 1:0);
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

        arrVoices = (Voice[])mVoices.toArray();
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
    }

    @android.webkit.JavascriptInterface
    @Override
    public void onError(String utteranceId) {
        isSpeaking = false;
    }
}