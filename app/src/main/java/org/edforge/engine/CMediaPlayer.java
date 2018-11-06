package org.edforge.engine;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CMediaPlayer extends MediaPlayer implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private WebView mWebView;

    private boolean      mPlaying       = false;
    private boolean      mIsReady       = false;
    private boolean      mDeferredStart = false;
    private boolean      mDeferredSeek  = false;
    private long         mSeekPoint     = 0;

    private boolean      _loop          = false;
    public long          index = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private HashMap queueMap    = new HashMap();
    private boolean       mDisabled   = false;



    public CMediaPlayer(WebView webView) {
        mWebView = webView;
    }


    public void play() {

        if(!mPlaying) {
            if(mIsReady) {
                start();
                mPlaying = true;
            }
            else
                mDeferredStart = true;
        }
    }


    public void stop() {

        pause();
        seek(0L);
    }


    public void pause() {
        if(mPlaying)
            super.pause();

        mPlaying = false;
    }


    public void seek(long frame) {

        // calc relative frame to seek to

        mSeekPoint = frame - index;

        if(mIsReady) {
            int iframe = (int) (frame * 1000 / 30);

            seekTo(iframe);
        }
        else
            mDeferredSeek = true;

    }


    public void seekTo(int frameTime) {

        // No errors occur - but don't try to seek past the end
        if(frameTime < super.getDuration())
            super.seekTo(frameTime);
    }

    public void setLooping(boolean loop) {
        _loop = loop;

        super.setLooping(loop);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

        mIsReady = true;

        // If seek was called before we were ready play
        if(mDeferredSeek)
            seek(mSeekPoint);

        // If play was called before we were ready play
        if(mDeferredStart)
            play();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        if(mPlaying) {

            if(_loop) {
            }
            else {
                pause();
                seekTo(0);
                mPlaying = false;

               post("complete");
            }
        }
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
                    case "complete":
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
    private void enQueue(CMediaPlayer.Queue qCommand) {

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

        enQueue(new CMediaPlayer.Queue(command));
    }




}
