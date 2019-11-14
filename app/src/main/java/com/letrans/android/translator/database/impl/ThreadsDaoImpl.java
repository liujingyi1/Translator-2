package com.letrans.android.translator.database.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.letrans.android.translator.database.DatabaseHelper;
import com.letrans.android.translator.database.DbConstants;
import com.letrans.android.translator.database.ThreadsDao;
import com.letrans.android.translator.database.beans.MemberBean;
import com.letrans.android.translator.database.beans.ThreadsBean;
import com.letrans.android.translator.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ThreadsDaoImpl implements ThreadsDao {
    private static final String TAG = "RTranslator/ThreadsDaoImpl";
    private Context mContext;
    private DatabaseHelper mDatabaseHelper;

    public ThreadsDaoImpl(Context context, DatabaseHelper databaseHelper) {
        mContext = context;
        mDatabaseHelper = databaseHelper;
    }

    @Override
    public ArrayList<ThreadsBean> getThreadsWithSelection(String selection, String[] selectionArgs) {
        String sql = "SELECT * FROM threads WHERE " + selection + ";";
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, selectionArgs);
        if (cursor == null) {
            return null;
        }
        ArrayList<ThreadsBean> threadsBeanList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                ThreadsBean threadsBean = getThreadsBean(cursor, false);
                threadsBeanList.add(threadsBean);
            }
        } finally {
            cursor.close();
        }
        return threadsBeanList;
    }

    @Override
    public HashMap<String, ThreadsBean> getAllGroupThreads() {
        String sql = "SELECT * FROM threads WHERE thread_type=3 AND deleted=0;";
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null);
        if (cursor == null) {
            return null;
        }
        HashMap<String, ThreadsBean> threadsBeanHashMap = new HashMap<>();
        try {
            while (cursor.moveToNext()) {
                ThreadsBean threadsBean = getThreadsBean(cursor, true);
                threadsBeanHashMap.put(threadsBean.getServerThreadId(), threadsBean);
            }
        } finally {
            cursor.close();
        }
        return threadsBeanHashMap;
    }

    @Override
    public ArrayList<ThreadsBean> getAllThreads() {
        String sql = "SELECT * FROM threads;";
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null);
        if (cursor == null) {
            return null;
        }
        ArrayList<ThreadsBean> threadsBeanList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                ThreadsBean threadsBean = getThreadsBean(cursor, false);
                threadsBeanList.add(threadsBean);
            }
        } finally {
            cursor.close();
        }
        return threadsBeanList;
    }

    @Override
    public ArrayList<ThreadsBean> getAllNoneGroupThreads() {
        String sql = "SELECT * FROM threads WHERE thread_type!=3 AND deleted=0;";
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null);
        if (cursor == null) {
            return null;
        }
        ArrayList<ThreadsBean> threadsBeanList = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                ThreadsBean threadsBean = getThreadsBean(cursor, false);
                threadsBeanList.add(threadsBean);
            }
        } finally {
            cursor.close();
        }
        return threadsBeanList;
    }

    @Override
    public long insert(ThreadsBean threadsBean) {
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        ContentValues values = buildContentValues(threadsBean);
        long id = database.insert(DbConstants.Tables.TABLE_THREADS,
                DbConstants.ThreadsColumns.TITLE, values);
        return id;
    }

    @Override
    public int update(ThreadsBean threadsBean, boolean onlyUpdateMember) {
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        String where = "_id=" + threadsBean.getId();
        if (onlyUpdateMember) {
            ContentValues values = new ContentValues();
            HashMap<String, MemberBean> map = threadsBean.getMembers();
            Iterator<Map.Entry<String, MemberBean>> iterator = map.entrySet().iterator();
            StringBuilder memberIds = new StringBuilder();
            while (iterator.hasNext()) {
                Map.Entry<String, MemberBean> entry = iterator.next();
                MemberBean memberBean = entry.getValue();
                memberIds.append(memberBean.getId());
                memberIds.append(",");
            }
            if (memberIds.length() > 0) {
                memberIds.deleteCharAt(memberIds.length() - 1);
                values.put(DbConstants.ThreadsColumns.MEMBER_ID, memberIds.toString());
            } else {
                Logger.d(TAG, "To clear member.");
                values.put(DbConstants.ThreadsColumns.MEMBER_ID, "");
            }
            return database.update(DbConstants.Tables.TABLE_THREADS, values, where, null);
        } else {
            ContentValues values = buildContentValues(threadsBean);
            return database.update(DbConstants.Tables.TABLE_THREADS, values, where, null);
        }
    }

    private ContentValues buildContentValues(ThreadsBean threadsBean) {
        ContentValues values = new ContentValues();
        if (!TextUtils.isEmpty(threadsBean.getServerThreadId())) {
            values.put(DbConstants.ThreadsColumns.SERVER_THREAD_ID, threadsBean.getServerThreadId());
        }
        if (threadsBean.getDate() != null) {
            values.put(DbConstants.ThreadsColumns.DATE, threadsBean.getDate());
        }
        if (threadsBean.getMessageCount() > 0) {
            values.put(DbConstants.ThreadsColumns.MESSAGE_COUNT, threadsBean.getMessageCount());
        }
        if (!TextUtils.isEmpty(threadsBean.getTitle())) {
            values.put(DbConstants.ThreadsColumns.TITLE, threadsBean.getTitle());
        }
        if (threadsBean.getUnreadCount() > 0) {
            values.put(DbConstants.ThreadsColumns.UNREAD_COUNT, threadsBean.getUnreadCount());
        }
        if (!TextUtils.isEmpty(threadsBean.getStorageFolderName())) {
            values.put(DbConstants.ThreadsColumns.STORAGE_FOLDER, threadsBean.getStorageFolderName());
        }
        int threadType = threadsBean.getThreadType();
        if (threadType > 0 && threadType <= 3) {
            values.put(DbConstants.ThreadsColumns.THREAD_TYPE, threadType);
        }
        values.put(DbConstants.ThreadsColumns.DELETED, threadsBean.getDeleted());
        if (!TextUtils.isEmpty(threadsBean.getLastMsgDate())) {
            values.put(DbConstants.ThreadsColumns.LAST_MSG_DATE, threadsBean.getLastMsgDate());
        }
        return values;
    }

    @NonNull
    private ThreadsBean getThreadsBean(Cursor cursor, boolean isGroup) {
        long id = cursor.getLong(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.ID));
        ThreadsBean threadsBean = new ThreadsBean(id);
        threadsBean.setServerThreadId(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.SERVER_THREAD_ID)));
        threadsBean.setDate(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.DATE)));
        threadsBean.setMessageCount(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.MESSAGE_COUNT)));
        threadsBean.setTitle(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.TITLE)));
        threadsBean.setUnreadCount(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.UNREAD_COUNT)));
        threadsBean.setStorageFolderName(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.STORAGE_FOLDER)));
        threadsBean.setThreadType(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.THREAD_TYPE)));
        threadsBean.setDeleted(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.DELETED)));
        threadsBean.setLastMsgDate(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.LAST_MSG_DATE)));

        if (isGroup) {
            String memberId = cursor.getString(
                    cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.MEMBER_ID));
            String where = "(" + memberId + ")";
            HashMap<String, MemberBean> members = queryMembers(where);
            threadsBean.getMembers().putAll(members);
        }

        return threadsBean;
    }

    private HashMap<String, MemberBean> queryMembers(String where) {
        String sql = "SELECT * FROM member WHERE _id IN " + where + ";";
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null, null);
        HashMap<String, MemberBean> members = new HashMap<>();
        if (cursor == null) {
            return members;
        }
        try {
            while (cursor.moveToNext()) {
                String deviceId = cursor.getString(
                        cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.DEVICE_ID));
                MemberBean memberBean = getMember(cursor, deviceId);
                members.put(deviceId, memberBean);
            }
        } finally {
            cursor.close();
        }
        return members;
    }

    private MemberBean getMember(Cursor cursor, String deviceId) {
        MemberBean memberBean = new MemberBean(cursor.getLong(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.ID)));
        memberBean.setDeviceId(deviceId);
        memberBean.setName(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.NAME)));
        memberBean.setNickName(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.NICK_NAME)));
        memberBean.setSex(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.SEX)));
        memberBean.setPhotoId(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.PHOTO_ID)));
        memberBean.setLanguage(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.LANGUAGE)));
        memberBean.setDescription(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.DESCRIPTION)));
        memberBean.setFavorite(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.FAVORITE)));
        return memberBean;
    }

    @Override
    public int delete(ThreadsBean threadsBean) {
        if (threadsBean == null) {
            return 0;
        }
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        String where = DbConstants.ThreadsColumns.ID + "=" + threadsBean.getId();
        return database.delete(DbConstants.Tables.TABLE_THREADS, where, null);
    }

    @Override
    public int delete(List<ThreadsBean> threadsBeans) {
        if (threadsBeans == null || threadsBeans.size() == 0) {
            return 0;
        }
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        String where = DbConstants.ThreadsColumns.ID + " IN (";
        StringBuilder builder = new StringBuilder();
        builder.append("DELETE FROM threads WHERE ");
        builder.append(where);
        for (ThreadsBean threadsBean : threadsBeans) {
            builder.append(threadsBean.getId());
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(");");
        database.execSQL(builder.toString());
        return threadsBeans.size();
    }

    @Override
    public int deleteAllThreads() {
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        return database.delete(DbConstants.Tables.TABLE_THREADS, null, null);
    }

    @Override
    public void clean() {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        database.execSQL("DELETE FROM threads;");
    }
}
