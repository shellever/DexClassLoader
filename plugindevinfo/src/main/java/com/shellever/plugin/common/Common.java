package com.shellever.plugin.common;

import android.content.Context;

/**
 * Created by winfor on 8/27/2018.
 */

public class Common {

    private Context mContext;

    public Common(Context context){
        mContext = context;
    }

    public String getDeviceInfo(){
        return "Shellever.HPF";
    }
}
