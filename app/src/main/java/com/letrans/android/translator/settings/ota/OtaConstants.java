package com.letrans.android.translator.settings.ota;

import android.os.Environment;

public class OtaConstants {

    public static final String DOWNLOAD_NAME = "ota/";
    public static final int MESSAGE_SUCCESS = 1;
    public static final int MESSAGE_NETWORK_ERROR = -1;
    public static final int MESSAGE_SYSTEM_ERROR = -2;
    public static final int MESSAGE_JSON_FORMAT_ERROR = -3;
    public static final String DOWNLOAD_OTA_NAME = Environment.getExternalStorageDirectory().getPath() + "/ota/";
    public static final int INSTALL_SUCCEEDED = 0;
    public static final int INSTALL_FAILED = -1;
    public static final int INSTALL_FAILED_INVALID_URI = -2;
}
