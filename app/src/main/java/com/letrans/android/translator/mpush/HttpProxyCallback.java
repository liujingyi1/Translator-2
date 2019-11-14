package com.letrans.android.translator.mpush;

import com.letrans.android.translator.mpush.domain.ServerResponse;

public interface HttpProxyCallback {
    public void onResponse(ServerResponse httpResponse);

    public void onCancelled();
}
