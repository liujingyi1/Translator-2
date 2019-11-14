package com.letrans.android.translator.reproduce;

import com.letrans.android.translator.database.beans.ThreadsBean;
import com.letrans.android.translator.storage.TStorageManager;

import java.util.ArrayList;

public class ReproduceThreadPresenterImpl implements ReproduceThreadContact.IReproduceThreadPresenter {

    private static final String TAG = "RTranslator/ReproduceThreadPresenterImpl";

    private TStorageManager mStorageManager;

    public ReproduceThreadPresenterImpl() {
        mStorageManager = TStorageManager.getInstance();
    }

    @Override
    public ArrayList<ThreadsBean> searchThread(String date) {
        return mStorageManager.getThreadsByDate(date);
    }

    @Override
    public void onDestroy() {

    }
}
