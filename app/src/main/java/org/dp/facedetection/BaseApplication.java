package org.dp.facedetection;

import android.app.Application;
import android.content.Context;

public class BaseApplication extends Application{

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        FaceDetect.init();
        mContext=this;
    }

    public static Context getContext(){
        return mContext;
    }
}
