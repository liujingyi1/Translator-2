package com.letrans.android.translator.home;

import android.content.Context;

import com.letrans.android.translator.R;
import com.letrans.android.translator.database.beans.UserBean;
import com.letrans.android.translator.languagemodel.LanguageItem;
import com.letrans.android.translator.mpush.domain.ServerApi;
import com.letrans.android.translator.storage.TStorageManager;
import com.letrans.android.translator.utils.NetUtil;
import com.letrans.android.translator.utils.ToastUtils;

import java.util.List;

public class HomePresenterImpl implements IHomePresenter {

    private static final String TAG = "RTranslator/HomePresenterImpl";

    private IHomeView mHomeView;

    private TStorageManager mStorageManager;

    public HomePresenterImpl(IHomeView view) {
        mHomeView = view;
        mStorageManager = TStorageManager.getInstance();
    }

    @Override
    public String getPairedId() {
        return mStorageManager.getPairedId();
    }

    @Override
    public boolean isNetworkConnected(Context context) {
        return NetUtil.isNetworkConnected(context);
    }

    @Override
    public void gotoComposemessage(final int index) {

        ServerApi.getInstance().enterCompose(TStorageManager.getInstance().getDeviceId(),
                getLanguageItemList().get(index).getCode
                        (), new ServerApi.ServerCallback() {
                    @Override
                    public void call(String result, int code) {
                        if (code == ServerApi.SR_SEND_TIMEOUT_CODE) {

                            ServerApi.getInstance().enterCompose(TStorageManager.getInstance().getDeviceId(),
                                    getLanguageItemList().get(index).getCode
                                            (), new ServerApi.ServerCallback() {
                                        @Override
                                        public void call(String result, int code) {
                                            setPrimaryLanguage(getLanguageItemList().get(index).getCode
                                                    ());

                                            if (mHomeView != null) {
                                                mHomeView.gotoComposemessage();
                                            }
                                        }
                                    });

                        } else {
                            setPrimaryLanguage(getLanguageItemList().get(index).getCode
                                    ());

                            if (mHomeView != null) {
                                mHomeView.gotoComposemessage();
                            }
                        }
                    }
                });
    }

    @Override
    public void isBothWorker(final int index) {

        final int userRole = UserBean.getUser().getRole();
        ServerApi.getInstance()
                .getRole(TStorageManager.getInstance().getPairedId(), new ServerApi.ServerCallback() {
                    @Override
                    public void call(String result, int code) {
                        if (code == ServerApi.SR_REQUEST_SUCCESS_CODE) {
                            int pairRole = Integer.parseInt(result);
                            if (userRole == UserBean.ROLE_TYPE_OWNER && pairRole == UserBean.ROLE_TYPE_OWNER) {
                                ToastUtils.showLong(R.string.only_one_worker_accept);
                            } else if (mHomeView != null) {
                                mHomeView.isBothWorkerFeedback(index);
                            }
                        } else {
                            mHomeView.isBothWorkerFeedback(index);
                        }
                    }
                });
    }

    @Override
    public List<LanguageItem> getLanguageItemList() {
        return mStorageManager.getLanguageItemList();
    }

    @Override
    public LanguageItem getLanguageItem(String code) {
        return mStorageManager.getLanguageItem(code);
    }

    @Override
    public void setPrimaryLanguage(String code) {
        UserBean user = mStorageManager.getUser();
        user.setLanguage(code);
    }

    @Override
    public void setSecondaryLanguage(String language) {
        // TODO for Handheld device
    }

    @Override
    public void onDestroy() {
        if (mHomeView != null) {
            mHomeView = null;
        }

        System.gc();
    }
}
