
package com.letrans.android.translator.settings.about;

import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.letrans.android.translator.AppContext;
import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.BaseDialog;
import com.letrans.android.translator.settings.pair.DigitsEditText;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.view.NumberBoardView;

public class SecretCodeDialog extends BaseDialog {

    private static final String TAG = "RTranslator/SecretCodeDialog";

    private View mView;
    DigitsEditText codeEditText;
    KeyboardView keyboardView;
    NumberBoardView boardView;
    TextView codeCit;

    private static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";

    private final static String ENG_MODE_CODE = "3646633";
    private final static String MTKLOG_CODE = "9646633";
    private final static String CHOSE_EAST_ASIA = "60354441";
    private final static String CHOSE_WEST_USA = "60354442";
    private final static String CHOSE_NORTH_EUROPE = "60354443";
    private final static String CHOSE_NONE = "60354444";
    private final static String CHOSE_BAIDU = "60355551";
    private final static String CHOSE_MICROSOFT = "60355552";
    private final static String CHOSE_ROOBO = "60355553";
    private final static String CHOSE_GOOGLE = "60355554";
    private final static String FILE_MANAGER = "60356666";
    private final static String CIT_CODE_1 = "60357777";
    private final static String COPY_LOG = "60358888";
    private final static String SETTING_MODE = "60359999";

    public SecretCodeDialog() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    protected void bindView() {
        super.bindView();
        initView();
    }

    public void initView() {
        mView = LayoutInflater.from(mContext).inflate(R.layout.secret_code_dialog, null);
        mContentView.setVisibility(View.VISIBLE);
        mContentView.addView(mView);

        codeEditText = (DigitsEditText) mView.findViewById(R.id.code_edit);
        keyboardView = (KeyboardView) mView.findViewById(R.id.keyboard_view);
        codeCit = (TextView) mView.findViewById(R.id.code_cit);
        boardView = new NumberBoardView(getActivity(), keyboardView, codeEditText);
        boardView.showKeyboard();

        StringBuilder sb = new StringBuilder();
        sb.append("FBI WARNING");
        codeCit.setText(sb.toString());

        codeEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == Keyboard.KEYCODE_DONE) {
                    String code = codeEditText.getText().toString();
                    if (!TextUtils.isEmpty(code)) {
                        handleSecretCode(mContext, code);
                    }
                    onBackPressed();
                }
                return false;
            }
        });
    }

    private void onBackPressed() {
        super.dismiss();
    }

    private boolean handleSecretCode(Context context, String input) {
        switch (input) {
            case CIT_CODE_1: {
                Intent intent = new Intent();
                intent.setClassName("com.rgk.factory", "com.rgk.factory.MainActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Logger.i(TAG, "com.rgk.factory.MainActivity is not available");
                }
                break;
            }
            case CHOSE_EAST_ASIA: {
                TStorageManager.getInstance().setTTSKey("asia");
                break;
            }
            case CHOSE_WEST_USA: {
                TStorageManager.getInstance().setTTSKey("us");
                break;
            }
            case CHOSE_NORTH_EUROPE: {
                TStorageManager.getInstance().setTTSKey("eu");
                break;
            }
            case CHOSE_NONE: {
                TStorageManager.getInstance().setTTSKey("");
                break;
            }
            case CHOSE_BAIDU: {
                TStorageManager.getInstance().setSTTType(AppContext.BAIDU);
                break;
            }
            case CHOSE_MICROSOFT: {
                TStorageManager.getInstance().setSTTType(AppContext.MICROSOFT_BING);
                break;
            }
            case CHOSE_ROOBO: {
                TStorageManager.getInstance().setSTTType(AppContext.ROOBO);
                break;
            }
            case CHOSE_GOOGLE: {
                TStorageManager.getInstance().setSTTType(AppContext.GOOGLE);
                break;
            }
            case ENG_MODE_CODE: {
                try {
                    final Intent intent = new Intent(SECRET_CODE_ACTION,
                            Uri.parse("android_secret_code://" + ENG_MODE_CODE));
                    mContext.sendBroadcast(intent);
                    return true;
                } catch (Exception e) {
                    Logger.i(TAG, "ENG_MODE_CODE is not available");
                }
                break;
            }
            case MTKLOG_CODE: {
                Intent intent = new Intent();
                intent.setClassName("com.mediatek.mtklogger", "com.mediatek.mtklogger.MainActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Logger.i(TAG, "com.mediatek.mtklogger.MainActivity is not available");
                }
                break;
            }
            case COPY_LOG: {
                try {
                    final Intent intent = new Intent(SECRET_CODE_ACTION,
                            Uri.parse("android_secret_code://2016001"));
                    mContext.sendBroadcast(intent);
                    return true;
                } catch (Exception e) {
                    Logger.i(TAG, "com.android.sales.SalesActivity is not available");
                }
                break;
            }
            case SETTING_MODE: {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", "com.android.settings.Settings");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Logger.i(TAG, "com.android.settings.Settings is not available");
                }
                break;
            }
            case FILE_MANAGER: {
                Intent intent = new Intent();
                intent.setClassName("com.mediatek.filemanager", "com.mediatek.filemanager.FileManagerOperationActivity");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Logger.i(TAG, "com.mediatek.filemanager.FileManagerOperationActivity is not available");
                }
                break;
            }
            default: {
                return false;
            }
        }
        return false;
    }
}
