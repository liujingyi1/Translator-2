package com.letrans.android.translator.settings.pair;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.letrans.android.translator.R;
import com.letrans.android.translator.mpush.HttpClientListener;
import com.letrans.android.translator.mpush.TMPushService;
import com.letrans.android.translator.mpush.bean.ServerPairInfoBean;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.mpush.domain.ServerResponse;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.Logger;
import com.letrans.android.translator.utils.NetBroadcastReceiver;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.utils.ToastUtils;
import com.letrans.android.translator.view.NumberBoardView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.BIND_AUTO_CREATE;

public class PairSettingFragment extends Fragment implements NetBroadcastReceiver.NetEvent{
    private String TAG = "RTranslator/PairSettingFragment";

    @BindView(R.id.user_id)
    TextView userIdView;
    @BindView(R.id.pair_id)
    DigitsEditText pairIdView;
    @BindView(R.id.my_device_id)
    TextView myDeviceId;
    @BindView(R.id.pair_device_id)
    TextView pairDeviceId;
    @BindView(R.id.pairing_group)
    ViewGroup pairingGroup;
    @BindView(R.id.paired_group)
    ViewGroup pairedGroup;
    @BindView(R.id.pair_btn)
    Button pairBtn;
    @BindView(R.id.code_time)
    Button codeTimeView;
    // @BindView(R.id.no_network)
    // TextView noNetworkView;
    @BindView(R.id.net_error)
    ViewGroup netError;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.unpair_btn)
    Button upairBtn;
    @BindView(R.id.pair_title)
    TextView pairTitle;

    KeyboardView keyboardView;
    NumberBoardView boardView;
    View mRootView;

    String mDeviceId;
    ServerPairInfoBean.PairBean mPairInfoBean = null;

    boolean isPairOK = false;
    private static int SECOND = 200;
    private static final int MSG_SHOW_VIEW = 1;
    private static final int MSG_ON_NETCHANGE = 2;
    private int serverResponseCode = 1;
    private String pairCode = "";
    // private String customPariCode = "";

    private Unbinder unbinder;

    Disposable mPairDisposable;

    List<Disposable> disposableList = new ArrayList<>();

    private HttpClientListener httpClientListener;

    private TMPushService mTMPushService;
    Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        mDeviceId = TStorageManager.getInstance().getDeviceId();

        Intent bindIntent = new Intent(getActivity(), TMPushService.class);
        getActivity().getApplicationContext().bindService(bindIntent, mServiceConnection, BIND_AUTO_CREATE);

        mPairInfoBean = new ServerPairInfoBean.PairBean();
        mPairInfoBean.deviceIdH = mDeviceId;
        NetBroadcastReceiver.registEvent(this,this);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTMPushService = ((TMPushService.TMPushBinder) service).getService();
            Logger.i(TAG, "onServiceConnected:" + mTMPushService);

            initPushApi();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Logger.i(TAG, "onServiceDisconnected:" + name);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.activity_pair, container, false);

        unbinder = ButterKnife.bind(this, mRootView);
        keyboardView = (KeyboardView) mRootView.findViewById(R.id.keyboard_view);

        initViews();

        Logger.i(TAG, "mDeviceId:" + mDeviceId);

        updatePairInfo();

        return mRootView;
    }

    public void updatePairInfo() {
        if (!NetUtil.isNetworkConnected(mContext)) {
            serverResponseCode = -1;
            showView();
        } else {
            if (!ServerApi.getInstance().getIsRegisted()) {
                ServerApi.getInstance()
                        .registClient(new ServerApi.ServerCallback() {
                            @Override
                            public void call(String result, int code) {
                                getPairInfo();
                            }
                        });
            } else {
                getPairInfo();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        Logger.i(TAG, "pair onResume");
        pairIdView.clearFocus();
        super.onResume();
    }

    private void onPairOk(String pairDeviceId) {
        mPairInfoBean.deviceIdW = pairDeviceId;
        TStorageManager.getInstance().setPairedId(pairDeviceId);
        if (TStorageManager.getInstance().getMember(mPairInfoBean.deviceIdW) == null) {
            TStorageManager.getInstance().createMember(mPairInfoBean.deviceIdW, "");
        }
        isPairOK = true;
        serverResponseCode = 1;
        mTMPushService.setPairUser(pairDeviceId);
    }

    private void onUnpairOk() {
        mPairInfoBean.deviceIdW = "";
        TStorageManager.getInstance().setPairedId("");
        isPairOK = false;
        serverResponseCode = 1;
        mTMPushService.setPairUser("");
    }

    private void initPushApi() {
        httpClientListener = new HttpClientListener() {
            @Override
            public void onPaired(String fromDeviceId) {
                Logger.i(TAG, "receive onPaired fromDeviceId=" + fromDeviceId);
                onPairOk(fromDeviceId);
                H.sendEmptyMessage(MSG_SHOW_VIEW);
            }

            @Override
            public void onUnPaired() {
                onUnpairOk();
                H.sendEmptyMessage(MSG_SHOW_VIEW);
            }
        };
        mTMPushService.addHttpClientListener(httpClientListener);
    }

    private void getPairInfo() {
        ServerApi.getInstance()
                .getPairInfo(mDeviceId)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        disposableList.add(disposable);
                        showProgress(true);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ServerPairInfoBean>() {
                    @Override
                    public void accept(ServerPairInfoBean serverResponse) throws Exception {
                        showProgress(false);
                        if (serverResponse != null) {
                            if (serverResponse.code == ServerApi.SR_REQUEST_SUCCESS_CODE) {
                                if (serverResponse.result.size() > 0) {
                                    if (mDeviceId.compareTo(serverResponse.result.get(0).deviceIdW) == 0) {
                                        mPairInfoBean.deviceIdW = serverResponse.result.get(0).deviceIdH;
                                    } else {
                                        mPairInfoBean.deviceIdW = serverResponse.result.get(0).deviceIdW;
                                    }
                                    Logger.i(TAG, "already paired code=" + mPairInfoBean.deviceIdW);
                                    isPairOK = true;
                                    onPairOk(mPairInfoBean.deviceIdW);
                                } else {
                                    Logger.i(TAG, "already paired isPairOK=" + isPairOK);
                                    mPairInfoBean.deviceIdW = "";
                                    isPairOK = false;
                                    onUnpairOk();
                                }
                            }
                            serverResponseCode = serverResponse.code;
                        } else {
                            serverResponseCode = -1;
                        }
                        showView();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Logger.i(TAG, "getPairInfo throwable=" + throwable.getMessage());
                        serverResponseCode = -1;
                        showProgress(false);
                        showView();
                    }
                });
    }

    private void showView() {
        Logger.i(TAG, "showView serverResponseCode=" + serverResponseCode + " isPairOK=" + isPairOK);
        if (netError == null) {
            return;
        }
        if (serverResponseCode == -1) {
            showErrorInfo(serverResponseCode);
            netError.setVisibility(View.VISIBLE);
            pairedGroup.setVisibility(View.GONE);
            pairingGroup.setVisibility(View.GONE);
            boardView.hideKeyboard();
        } else if (serverResponseCode == 1 && isPairOK) {
            netError.setVisibility(View.INVISIBLE);
            pairedGroup.setVisibility(View.VISIBLE);
            pairingGroup.setVisibility(View.GONE);
            boardView.hideKeyboard();
            showPaired();
        } else if (serverResponseCode == 1 && !isPairOK) {
            netError.setVisibility(View.INVISIBLE);
            pairedGroup.setVisibility(View.GONE);
            pairingGroup.setVisibility(View.VISIBLE);
            startPairing();
        } else {
            showErrorInfo(serverResponseCode);
        }
    }

    private void startPairing() {
        getPairCode();
    }

    private void getPairCode() {
        ServerApi.getInstance()
                .getPairCode(mDeviceId)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        disposableList.add(disposable);
                        showProgress(true);
                        RxTextView.text(pairTitle).accept(getString(R.string.requesting_pair_code));
                        RxView.enabled(pairBtn).accept(false);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread()) // 指定主线程
                .subscribe(new Consumer<ServerResponse>() {
                    @Override
                    public void accept(ServerResponse serverResponse) throws Exception {
                        showProgress(false);
                        if (serverResponse != null) {
                            serverResponseCode = serverResponse.code;
                            if (serverResponse.code == ServerApi.SR_REQUEST_SUCCESS_CODE) {
                                pairCode = serverResponse.result;
                                RxTextView.text(pairTitle).accept(getString(R.string.pair_code_title));
                                RxTextView.text(userIdView).accept(pairCode);
                                RxView.enabled(codeTimeView).accept(false);
                                RxView.visibility(userIdView).accept(true);
                                String pairUserId = pairIdView.getText().toString();
                                pairIdView.setText("");
                                if (!TextUtils.isEmpty(pairUserId) && (pairUserId.length() == 4)) {
                                    pairBtn.setEnabled(true);
                                } else {
                                    pairBtn.setEnabled(false);
                                }
                                boardView.showKeyboard();

                                if (mPairDisposable != null) {
                                    mPairDisposable.dispose();
                                }
                                Observable.interval(1, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                                        .take(ServerApi.CODE_INVALID_SECOND)
                                        .subscribe(new Observer<Long>() {
                                            @Override
                                            public void onSubscribe(Disposable d) {
                                                mPairDisposable = d;
                                            }

                                            @Override
                                            public void onNext(Long aLong) {
                                                if (!isPairOK) {
                                                    try {
                                                        RxTextView.text(codeTimeView).accept((SECOND - aLong) + "s");
                                                    } catch (Exception e) {
                                                    }
                                                } else {
                                                    invalidPairCode();
                                                    mPairDisposable.dispose();
                                                }
                                            }

                                            @Override
                                            public void onError(Throwable e) {
                                                Logger.i(TAG, "exception=" + e.getMessage());
                                            }

                                            @Override
                                            public void onComplete() {
                                                try {
                                                    RxView.enabled(codeTimeView).accept(true);
                                                    RxTextView.text(codeTimeView).accept(getString(R.string.reques_pair_code_again));
                                                    RxTextView.text(pairTitle).accept(getString(R.string.pair_code_invalid_title));
                                                    RxView.visibility(userIdView).accept(false);
                                                    invalidPairCode();
                                                } catch (Exception e) {
                                                }
                                            }
                                        });
                            } else if (serverResponse.code == ServerApi.SR_GET_PAIRCODE_FAIL) {
                                RxView.enabled(codeTimeView).accept(true);
                                RxTextView.text(codeTimeView).accept(getString(R.string.reques_pair_code_again));
                                RxView.visibility(userIdView).accept(false);
                                RxTextView.text(pairTitle).accept(getString(R.string.reques_pair_code_fail));
                                pairBtn.setEnabled(false);
                                ToastUtils.showLong(R.string.reques_pair_code_fail_toast);
                            } else {
                                showView();
                            }
                        } else {
                            serverResponseCode = -1;
                            showView();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        showProgress(false);
                        serverResponseCode = -1;
                        showView();
                    }
                });
    }

    private void invalidPairCode() {
        if (!TextUtils.isEmpty(pairCode)) {
            ServerApi.getInstance()
                    .invalidPairCode(mDeviceId, pairCode)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<ServerResponse>() {
                        @Override
                        public void accept(ServerResponse serverResponse) throws Exception {
                            if (serverResponse.code == 1) {
                                pairCode = "";
                                Logger.i(TAG, "invalidPairCode success");
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {

                        }
                    });
        }
    }

    private void pairClient(String clientPairCode) {
        Logger.i(TAG, "pairClient clientPairCode=" + clientPairCode);
        ServerApi.getInstance()
                .pairClient(mDeviceId, clientPairCode)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        disposableList.add(disposable);
                        showProgress(true);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ServerResponse>() {
                    @Override
                    public void accept(ServerResponse serverResponse) throws Exception {
                        Logger.i(TAG, "pairClient subscribe serverResponse=" + serverResponse.toString());
                        showProgress(false);
                        if (serverResponse != null) {
                            if (serverResponse.code == ServerApi.SR_REQUEST_SUCCESS_CODE) {

                                onPairOk(serverResponse.result);

                                Logger.i(TAG, "pairClient subscribe serverResponse.result=" + serverResponse.result);
                                Logger.i(TAG, "pairClient subscribe isPairOK=" + isPairOK);
                            }
                            serverResponseCode = serverResponse.code;
                        } else {
                            serverResponseCode = -1;
                        }
                        pairIdView.setText("");
                        showView();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        serverResponseCode = -1;
                        showProgress(false);
                        showView();
                    }
                });
    }

    private void unPairClient() {
        ServerApi.getInstance()
                .relievePair(mDeviceId, mPairInfoBean.deviceIdW)
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        disposableList.add(disposable);
                    }
                })
                .subscribe(new Consumer<ServerResponse>() {
                    @Override
                    public void accept(ServerResponse serverResponse) throws Exception {
                        if (serverResponse != null) {
                            serverResponseCode = serverResponse.code;
                            if (serverResponse.code == ServerApi.SR_REQUEST_SUCCESS_CODE
                                    || serverResponse.code == ServerApi.SR_CLIENT_NOT_ONLINE_CODE) {
                                onUnpairOk();
                            }
                        } else {
                            serverResponseCode = -1;
                        }
                        showView();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        serverResponseCode = -1;
                        showView();
                    }
                });
    }

    private void showPaired() {
        myDeviceId.setText(getString(R.string.current_device)
                + "\n" + mDeviceId);
        pairDeviceId.setText(getString(R.string.paired_device)
                + "\n" + mPairInfoBean.deviceIdW);
    }

    private void showErrorInfo(int code) {
        Log.i(TAG, "showErrorInfo code="+code);
        switch (code) {
            case ServerApi.SR_PAIR_CODE_INVALID_CODE: {
                ToastUtils.showLong(R.string.pair_fail_toast_invalid_code);
                break;
            }
            case ServerApi.SR_PAIR_ERROR_CODE: {
                ToastUtils.showLong(R.string.pair_fail_toast_fail);
                break;
            }
            case ServerApi.SR_ALREADY_PAIRED_CODE: {
                ToastUtils.showLong(R.string.pair_fail_toast_already_pair);
                break;
            }
            case ServerApi.SR_CLIENT_NOT_ONLINE_CODE: {
                ToastUtils.showLong(R.string.pair_fail_toast_not_online);
                break;
            }
            case ServerApi.SR_SEND_TIMEOUT_CODE: {
                ToastUtils.showLong(R.string.pair_fail_toast_fail);
                break;
            }
            default: {
                if (netError != null) {
                    netError.setVisibility(View.VISIBLE);
                    pairedGroup.setVisibility(View.GONE);
                    pairingGroup.setVisibility(View.GONE);
                    boardView.hideKeyboard();
                }
            }
        }
    }

    private void showProgress(boolean show) {
        // show = false;
        // if (progressBar != null) {
            // progressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        // }
    }

    private void initViews() {
        boardView = new NumberBoardView(getActivity(), keyboardView, pairIdView);
        pairIdView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == Keyboard.KEYCODE_DONE) {
                    String pairUserId = pairIdView.getText().toString();
                    if (pairUserId.length() == 4) {
                        pairClient(pairUserId);
                    }
                }
                return false;
            }
        });

        RxView.clicks(pairBtn)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object serverResponse) throws Exception {
                        String pairUserId = pairIdView.getText().toString();
                        pairClient(pairUserId);
                    }
                });

        RxView.clicks(codeTimeView)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object serverResponse) throws Exception {
                        getPairCode();
                    }
                });

        RxView.clicks(upairBtn)
                .throttleFirst(2, TimeUnit.SECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object serverResponse) throws Exception {
                        unPairClient();

                    }
                });

        pairIdView.addTextChangedListener(new TextWatcher() {
            int selection, beforeCount;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                selection = pairIdView.getSelectionEnd();
                beforeCount = charSequence.length();
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (beforeCount < editable.toString().length()) {
                    if (editable.toString().length() > 4) {
                        if (selection < beforeCount) {
                            pairIdView.setText(editable.delete(selection + 1, selection + 2));
                        } else {
                            pairIdView.setText(editable.delete(selection, selection + 1));
                        }
                        pairIdView.setSelection(selection);
                    }
                }
                if (editable.toString().length() == 4) {
                    pairBtn.setEnabled(true);
                } else {
                    pairBtn.setEnabled(false);
                }
            }
        });
    }

    Handler H = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SHOW_VIEW: {
                    showView();
                    break;
                }
                case MSG_ON_NETCHANGE: {
                    updatePairInfo();
                    break;
                }
            }
        }
    };

    @Override
    public void onPause() {
        Logger.i(TAG, "pair onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {

        boardView.release();
        NetBroadcastReceiver.unRegistEvent(this);

        invalidPairCode();
        Logger.i(TAG, "pair onDestroy");
        //感觉是有问题的..先去掉
//        if (!isPairOK && mTMPushService != null) {
//            mTMPushService.setPairUser(null);
//        }

        if(mTMPushService != null) {
            mTMPushService.removeHttpClientListener(httpClientListener);
        }

        getActivity().getApplicationContext().unbindService(mServiceConnection);

        if (mPairDisposable != null && !mPairDisposable.isDisposed()) {
            mPairDisposable.dispose();
        }
        for (Disposable d : disposableList) {
            if (d != null && !d.isDisposed()) {
                d.dispose();
            }
        }
        if (unbinder != null) {
            unbinder.unbind();
        }

        super.onDestroy();
    }


    @Override
    public void onNetChange(boolean hasNetwork) {
        Log.i(TAG, "hasNetwork="+hasNetwork);
        H.sendEmptyMessageDelayed(MSG_ON_NETCHANGE, 500);
    }
}
