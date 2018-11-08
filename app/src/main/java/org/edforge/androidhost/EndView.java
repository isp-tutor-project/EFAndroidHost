package org.edforge.androidhost;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.edforge.engine.UserManager;

import java.io.File;

import static org.edforge.androidhost.TCONST.LAUNCH_TUTOR;

/**
 * Created by kevin on 11/7/2018.
 */

public class EndView extends FrameLayout {

    private Context mContext;

    final private String       TAG = "HostWebView";


    public EndView(Context context) {
        super(context);
        init(context, null);
    }

    public EndView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public EndView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void init(Context context, AttributeSet attrs) {

        mContext = context;
    }
}
