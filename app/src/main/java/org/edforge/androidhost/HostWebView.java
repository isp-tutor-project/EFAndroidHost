package org.edforge.androidhost;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;

import org.edforge.engine.CNativeAudio;
import org.edforge.engine.TTSsynthesizer;
import org.edforge.engine.UserManager;

import java.io.File;

import static org.edforge.androidhost.TCONST.LAUNCH_TUTOR;

/**
 * Created by kevin on 11/5/2018.
 */

public class HostWebView extends FrameLayout {

    private Context mContext;
    private WebView webView;

    private UserManager             mUserManager;
    private CNativeAudio            mNativeAudio;
    private TTSsynthesizer          mNativeSpeech;

    private LocalBroadcastManager   bManager;
    private webViewReceiver         bReceiver;

    private File    folder;
    private String  sdcard;
    private File    basefolder;

    private Intent mLaunch;

    final private String       TAG = "HostWebView";



    public HostWebView(Context context) {
        super(context);
        init(context, null);
    }

    public HostWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HostWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {

        mContext = context;
        mUserManager = UserManager.getInstance();

        folder = Environment.getExternalStorageDirectory();
        sdcard = folder.getAbsolutePath();

        basefolder = new File(sdcard + File.separator + "EdForge" );

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(getContext());

        IntentFilter filter = new IntentFilter(LAUNCH_TUTOR);

        bReceiver = new webViewReceiver();
        bManager.registerReceiver(bReceiver, filter);
    }


    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();

        webView    = (WebView) findViewById(R.id.webview);

        // red-herring
        //http://stackoverflow.com/questions/20675554/webview-rendering-issue-in-android-kitkat
        //webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        webView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        // See - https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        //
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != (mContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            { WebView.setWebContentsDebuggingEnabled(true); }
        }

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);

        // Create a javascript interface for native audio
        mNativeAudio = new CNativeAudio(AndroidHost.ACTIVITY, webView);
        webView.addJavascriptInterface(mNativeAudio, "EFnativeAudio");

//        // Create a javascript interface for native Speech
//        mNativeSpeech = new TTSsynthesizer(AndroidHost.ACTIVITY, webView);
//        webView.addJavascriptInterface(mNativeSpeech, "EFnativeSpeech");

        // Create a javascript interface for native audio
        webView.addJavascriptInterface(mUserManager, "EFnativeUserMgr");
//        webView.addJavascriptInterface(mLogManager, "EFnativeLogMgr");
    }


    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }


    class webViewReceiver extends BroadcastReceiver {

        public void onReceive (Context context, Intent intent) {

            Log.d("nameView", "Broadcast recieved: ");

            switch(intent.getAction()) {

                case LAUNCH_TUTOR:
                    String tutorName = mUserManager.getTutorFileName();

                    Log.d(TAG, "file:///" + basefolder + tutorName);
                    //load file
                    webView.loadUrl("file:///" + basefolder + tutorName);
                    break;
            }
        }
    }



}
