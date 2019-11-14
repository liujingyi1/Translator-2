package com.letrans.android.translator.database;

import com.letrans.android.translator.database.beans.ThreadsBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface ThreadsDao {
    ArrayList<ThreadsBean> getThreadsWithSelection(String selection, String[] selectionArgs);

    HashMap<String, ThreadsBean> getAllGroupThreads();

    ArrayList<ThreadsBean> getAllThreads();

    ArrayList<ThreadsBean> getAllNoneGroupThreads();

    long insert(ThreadsBean threadsBean);

    int update(ThreadsBean threadsBean, boolean onlyUpdateMember);

    int delete(ThreadsBean threadsBean);

    int delete(List<ThreadsBean> threadsBeans);

    int deleteAllThreads();

    void clean();
}
