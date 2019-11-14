package com.letrans.android.translator.settings.ota.response;

public class CheckClientVersionResponse extends BaseResponse {

    private AppData data;

    public AppData getData() {
        return data;
    }

    public void setData(AppData data) {
        this.data = data;
    }
}
