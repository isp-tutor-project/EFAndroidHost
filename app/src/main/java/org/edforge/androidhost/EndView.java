package org.edforge.androidhost;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.edforge.engine.UserManager;

import java.io.File;

import static org.edforge.androidhost.TCONST.LAUNCH_TUTOR;

/**
 * Created by kevin on 11/7/2018.
 */

public class EndView extends FrameLayout {

    private Context mContext;

    private TextView mTitle1;
    private TextView mTitle2;

    private Boolean alive = true;

    final private String       TAG = "EndView";


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

    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();

        mTitle1    = (TextView) findViewById(R.id.endTitle1);
        mTitle2     = (TextView) findViewById(R.id.endTitle2);

    }

    public void AllComplete() {

        mTitle1.setText("You're done with the tablet portion of the lesson.");
        mTitle2.setText("Please raise your hand.");
    }

    public void onDestroy() {

        alive = false;
    }


}
