package com.letrans.android.translator.reproduce;

import com.letrans.android.translator.database.beans.ThreadsBean;
import com.letrans.android.translator.mvpbase.BasePresenter;

import java.util.ArrayList;

public class ReproduceThreadContact {
    public interface IReproduceThreadView {

    }

    public interface IReproduceThreadPresenter extends BasePresenter {
        // 根据日期查询会话
        ArrayList<ThreadsBean> searchThread(String date);
    }
}
