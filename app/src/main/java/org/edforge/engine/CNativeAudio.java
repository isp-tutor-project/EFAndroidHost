package org.edforge.engine;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CNativeAudio {

    private Activity    mOwner;
    private WebView     mWebView;

    private Runnable endTask;
    private Handler handler;

    private CMediaPlayer                    mPlayer;
    private HashMap<String, CMediaPlayer> cachedSource = new HashMap<>();

    private MediaPlayer.OnCompletionListener listener;

    private String mMusic = null;

    static       public String language = "LANG_EN";



    // This is used to map Language identifiers in tutor_decriptor to audio subdir names
    static public HashMap<String, String> langMap = new HashMap<String, String>();
    static {
        langMap.put("LANG_EN", "en");
        langMap.put("LANG_SW", "sw");
    }

    private static final String TAG   = "CNativeAudio";


    public CNativeAudio(Activity app, WebView webView) {

        mOwner   = app;
        mWebView = webView;
    }


    static public String getLanguage() {

        return langMap.get(language);
    }


    @android.webkit.JavascriptInterface
    public void end() {
    }

    @android.webkit.JavascriptInterface
    public float getEffectsVolume() {
        return 1.0f;
    }

    @android.webkit.JavascriptInterface
    public float getMusicVolume() {
        return 1.0f;
    }

    @android.webkit.JavascriptInterface
    public boolean isMusicPlaying() {
        return false;
    }

    @android.webkit.JavascriptInterface
    public void pauseAllEffects() {

    }

    @android.webkit.JavascriptInterface
    public int pauseEffect(int effectID) {
        return 1;
    }

    @android.webkit.JavascriptInterface
    public void pauseMusic() {

    }

    @android.webkit.JavascriptInterface
    public int playEffect(String soundsource, boolean loop) {
        play(soundsource);

        mPlayer.setLooping(loop);

        return 1;
    }

    @android.webkit.JavascriptInterface
    public void playMusic(String soundsource, boolean loop) {

        if(soundsource == null) {
            play(mMusic);
        }
        else {
            play(soundsource);
            mMusic = soundsource;
        }

        mPlayer.setLooping(loop);
    }


    @android.webkit.JavascriptInterface
    public void resumeAllEffects() {

    }

    @android.webkit.JavascriptInterface
    public void resumeEffect(int effectID) {

    }

    @android.webkit.JavascriptInterface
    public void resumeMusic() {

    }

    @android.webkit.JavascriptInterface
    public void rewindMusic() {

    }

    @android.webkit.JavascriptInterface
    public void setEffectsVolume(float vol) {

    }

    @android.webkit.JavascriptInterface
    public void setMusicVolume(float vol) {

    }

    @android.webkit.JavascriptInterface
    public void stopAllEffects() {
        Iterator entries = cachedSource.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String key = (String)entry.getKey();

            cachedSource.get(key).stop();
        }
    }

    @android.webkit.JavascriptInterface
    public void stopEffect(int effectID) {

    }

    @android.webkit.JavascriptInterface
    public void stopMusic() {

    }

    @android.webkit.JavascriptInterface
    public void unloadEffect(String soundsource) {

    }


    @android.webkit.JavascriptInterface
    public void willPlayMusic() {

    }


    @android.webkit.JavascriptInterface
    public void clearAllSounds() {

        Log.i(TAG, "LJSCR Releasing Sound Sources: ");

        Iterator entries = cachedSource.entrySet().iterator();

        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String key = (String)entry.getKey();

            cachedSource.get(key).release();
        }

        cachedSource = new HashMap<>();
    }


    @android.webkit.JavascriptInterface
    public void registerSounds(String soundPath, String fileId, String basePath) {

        Log.i(TAG, "LJSCR Register Sound Source: " + fileId);

        try {

            String soundsource = Environment.getExternalStorageDirectory().getAbsolutePath() +
                                File.separator + "EdForge" + File.separator +  basePath + soundPath;

            Log.i(TAG, "Sound Source: " + soundsource);

            mPlayer = cachedSource.get(fileId);

            // If the sound source doesn't change then we play the source we have already loaded
            // reduces play latency.
            //
            if(mPlayer == null) {

                mPlayer = new CMediaPlayer(mWebView);

                cachedSource.put(fileId, mPlayer);

                File soundData = new File(soundsource);

                Log.d(TAG, "Audio Loading: " + soundsource);
//                Log.i(TAG, "Sound Source Offset: " + soundData.getStartOffset() + " Length: " + soundData.getLength());

                mPlayer.setDataSource(soundsource);

                mPlayer.setOnPreparedListener(mPlayer);
                mPlayer.setOnCompletionListener(mPlayer);
                mPlayer.prepareAsync();
            }

        } catch (Exception e) {
            Log.d(TAG, "Audio frame format error: " + e);
        }
    }


    @android.webkit.JavascriptInterface
    public void play(String soundsource) {

        Log.i(TAG, "LJSCR Play Sound Source: " + soundsource);

        try {
            mPlayer = cachedSource.get(soundsource);

            if(mPlayer == null) {

                Log.e(TAG,"Error:" + "soundSource not registered!!");
                throw(new Exception("Error:" + "soundSource not registered!!"));
            }

            // iTry to play immediately - the defer will start it when loaded
            //
            mPlayer.play();

        } catch (Exception e) {
            Log.d(TAG, "Audio frame format error: " + e);
        }
    }

}
