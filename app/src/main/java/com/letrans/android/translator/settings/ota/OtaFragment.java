package com.letrans.android.translator.settings.ota;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

import com.letrans.android.translator.R;
import com.letrans.android.translator.settings.ota.dialog.DownloadProgressDialog;
import com.letrans.android.translator.settings.ota.download.DownloadTask;
import com.letrans.android.translator.settings.ota.request.CheckTranslatorOta;
import com.letrans.android.translator.settings.ota.response.AppData;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.utils.ToastUtils;

import io.reactivex.disposables.Disposable;


public class OtaFragment extends Fragment implements CheckTranslatorOta.OnCheckListener, DownloadTask.DownloadListener {

    private static final String TAG = "RTranslator/OtaFragment";
    private View mView;
    private TextView otaText, versionText;
    private TextView otaButton, checkota_button;
    private ProgressBar progressBar;
    private DownloadProgressDialog pBar;
    DownloadTask downloadTask;
    private boolean isAttached = false;
    private TextView ota_message;
    private RadioButton ota_radio_auto;
    private RadioButton ota_radio;
    CheckTranslatorOta mCheckTranslatorOta;
    List<Disposable> disposableList = new ArrayList<>();
    private TextView ota_message_title;
    private String downloadUri;
    private String newVersionName;
    private String newVersionCode;
    private String downloadType;
    private boolean diffUpdate;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Logger.d(TAG,"onCreateView ");
        mView = inflater.inflate(R.layout.activity_ota, container, false);
        initViews();
        mCheckTranslatorOta = new CheckTranslatorOta(getActivity(),this);
        initData();
        return mView;
    }

    private void initData() {
        String versionName = OtaTools.getVersionName(getActivity());
        versionText.setText("v" + versionName);
        if (TStorageManager.getInstance().isOtaAutoCheck()) {
            ota_radio.setChecked(false);
            ota_radio_auto.setChecked(true);
        } else {
            ota_radio.setChecked(true);
            ota_radio_auto.setChecked(false);
        }
    }

    private void checkOTA() {
        ota_message_title.setVisibility(View.GONE);
        ota_message.setVisibility(View.GONE);
        if (!NetUtil.isNetworkConnected(getActivity())) {
            otaText.setText(R.string.ota_latest_version);
            otaText.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        if (mCheckTranslatorOta != null) {
            mCheckTranslatorOta.startCheckOta();
        }
    }

    private void initViews() {
        checkota_button = (TextView) mView.findViewById(R.id.checkota_button);
        progressBar = (ProgressBar) mView.findViewById(R.id.ota_progressBar);
        otaText = (TextView) mView.findViewById(R.id.ota_version_message);
        ota_message_title = (TextView) mView.findViewById(R.id.ota_message_title);
        ota_message = (TextView) mView.findViewById(R.id.ota_message);
        versionText = (TextView) mView.findViewById(R.id.ota_version);
        otaButton = (TextView) mView.findViewById(R.id.ota_button);
        ota_radio = (RadioButton) mView.findViewById(R.id.ota_radio_button);
        ota_radio_auto = (RadioButton) mView.findViewById(R.id.ota_radio_button1);
        RadioGroup mOta_auto_group = (RadioGroup) mView.findViewById(R.id.ota_auto_group);
        otaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDownloadDialog(newVersionName, newVersionCode, downloadUri, diffUpdate, downloadType);
            }
        });
        checkota_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOTA();
            }
        });
        mOta_auto_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.ota_radio_button: {
                        ota_radio.setChecked(true);
                        ota_radio_auto.setChecked(false);
                        checkota_button.setVisibility(View.VISIBLE);
                        otaButton.setVisibility(View.GONE);
                        otaText.setVisibility(View.GONE);
                        ota_message.setVisibility(View.GONE);
                        ota_message_title.setVisibility(View.GONE);
                        TStorageManager.getInstance().setOtaAutoCheck(false);
                        break;
                    }
                    case R.id.ota_radio_button1: {
                        ota_radio.setChecked(false);
                        ota_radio_auto.setChecked(true);
                        checkota_button.setVisibility(View.GONE);
                        otaText.setVisibility(View.GONE);
                        otaButton.setVisibility(View.GONE);
                        ota_message.setVisibility(View.GONE);
                        ota_message_title.setVisibility(View.GONE);
                        TStorageManager.getInstance().setOtaAutoCheck(true);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //checkOTA();
        Logger.d(TAG,"onResume ");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isAttached = true;
        Logger.d(TAG,"onAttach ");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;
        Logger.d(TAG,"onDetach ");
    }

    @Override
    public void onStop() {
        super.onStop();
        Logger.d(TAG,"onStop ");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG,"onDestroy ");
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
        if (pBar != null) {
            pBar.dismiss();
        }
        if (disposableList != null && disposableList.size() > 0) {
            for (Disposable d : disposableList) {
                if (d != null && !d.isDisposed()) {
                    d.dispose();
                }
            }
        }
    }

    @Override
    public void onSuccess(AppData client) {
        if (!isAttached) return;
        progressBar.setVisibility(View.GONE);
        otaButton.setVisibility(View.GONE);
        if (client != null) {
            String versionCode = client.getVersionCode();
            if (!TextUtils.isEmpty(versionCode) && OtaTools.isNewVersion(getActivity(), Integer.parseInt(versionCode))) {
                String versionName = client.getVersionName();
                String url = client.getDownloadUrl();
                String diffUrl = client.getDiffDownloadUrl();
                String msg = client.getDescribe();
                updateMessage(versionCode, versionName, msg);
                newVersionCode = versionCode;
                newVersionName = versionName;
                downloadType = client.getInstallType();
                diffUpdate = client.isDiffUpdate();
                downloadUri = diffUpdate ? diffUrl : url;
            } else {
                otaText.setText(R.string.ota_latest_version);
                otaText.setVisibility(View.VISIBLE);
                ToastUtils.showShort(R.string.ota_latest_version);
            }
        } else {
            ToastUtils.showShort(R.string.ota_check_failed);
            otaText.setText(R.string.ota_latest_version);
        }
    }

    private void updateMessage(String versionCode, String versionName, String message) {
        ota_message.setVisibility(View.VISIBLE);
        ota_message_title.setVisibility(View.VISIBLE);
        otaText.setVisibility(View.GONE);
        otaButton.setVisibility(View.VISIBLE);
        ota_message.setText(message);
        checkota_button.setVisibility(View.GONE);
    }

    @Override
    public void onFailed() {
        Logger.d(TAG, "onFailed " + isAttached);
        if (!isAttached) return;
        otaText.setText(R.string.ota_latest_version);
        otaText.setVisibility(View.VISIBLE);
        otaButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onAddDisposable(Disposable d) {
        if (disposableList != null) {
            disposableList.add(d);
        }
    }

    private void showMessageDialog(boolean forceUpdate, final String versionName, final String versionCode, final String url, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle(getContext().getString(R.string.ota_new_version) + versionName);
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ota_positive_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                showDownloadDialog(versionName, versionCode, url, diffUpdate, downloadType);
            }
        });
        if (!forceUpdate) {
            builder.setNegativeButton(R.string.ota_negative_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
        }
        builder.create().show();
    }

    private void showDownloadDialog(String versionName, String versionCode, String url, boolean diffUpdate, String type) {
        Logger.d(TAG,"showDownloadDialog versionName : " + versionName + " url : " + url);
        pBar = new DownloadProgressDialog(getContext());
        pBar.setCanceledOnTouchOutside(true);
        pBar.setMessage(getContext().getString(R.string.ota_download_title));
        pBar.setProgressNumberFormat("%1d KB/%2d KB");
        pBar.setIndeterminate(false);
        pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pBar.setCancelable(true);
        downloadTask = new DownloadTask(
                getActivity(), this);
        downloadTask.executeOnExecutor(Executors.newCachedThreadPool(), url, versionName, diffUpdate ? "1" : "0", type);
        //downloadTask.execute(url, versionName, diffUpdate ? "1" : "0", type);
        pBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadTask.cancel(true);
            }
        });
    }


    @Override
    public void onPreDownload() {
        if (pBar != null) {
            pBar.show();
        }
    }

    @Override
    public void onUpdate(int total, int bytes, boolean isDone) {
        if (pBar != null) {
            if (isDone) {
                pBar.setMessage(getContext().getString(R.string.ota_install_title));
                pBar.setCancelable(false);
                pBar.setCanceledOnTouchOutside(false);
                pBar.setProgressNumberFormat(null);
                pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pBar.setIndeterminate(true);
            } else {
                pBar.setMax(total);
                pBar.setProgress(bytes);
            }
        }
    }

    @Override
    public void onCompleted(String result, String file) {
        if (pBar != null && pBar.isShowing()) {
            pBar.dismiss();
        }
        if (result != null) {
            Logger.w(TAG, " result : " + result);
            ToastUtils.showLong(R.string.ota_update_failed);
        }
    }

}
