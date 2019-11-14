package com.letrans.android.translator.mpush;

import android.content.Context;

public class MPushApi {

    private static IMPushApi mPushApi = null;
    private MPushApi(){
        
    }

    public synchronized static IMPushApi get(Context context) {
            if (mPushApi == null)
                mPushApi = new MPushApiProxy(context);
            return mPushApi;
    }

}
