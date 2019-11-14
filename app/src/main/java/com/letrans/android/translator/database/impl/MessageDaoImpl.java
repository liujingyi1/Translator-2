package com.letrans.android.translator.database.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.letrans.android.translator.database.DatabaseHelper;
import com.letrans.android.translator.database.DbConstants;
import com.letrans.android.translator.database.MessageDao;
import com.letrans.android.translator.database.beans.MessageBean;

import java.util.ArrayList;
import java.util.List;

public class MessageDaoImpl implements MessageDao {
    private Context mContext;
    private DatabaseHelper mDatabaseHelper;

    public MessageDaoImpl(Context context, DatabaseHelper databaseHelper) {
        mContext = context;
        mDatabaseHelper = databaseHelper;
    }

    @Override
    public MessageBean getMessageById(long id) {
        String sql = "SELECT * FROM message_view WHERE _id=?;";
        String[] selectionArgs = {String.valueOf(id)};
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, selectionArgs);
        if (cursor == null) {
            return null;
        }
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            MessageBean messageBean = getMessageBean(cursor);
            return messageBean;
        } finally {
            cursor.close();
        }
    }

    @Override
    public List<MessageBean> getMessageByThreadId(int threadId) {
        String sql = "SELECT * FROM message_view WHERE thread_id=?;";
        String[] selectionArgs = {String.valueOf(threadId)};
        return query(sql, selectionArgs);
    }

    @Override
    public List<MessageBean> getMessageByMemberId(int memberId) {
        String sql = "SELECT * FROM message_view WHERE member_id=?;";
        String[] selectionArgs = {String.valueOf(memberId)};
        return query(sql, selectionArgs);
    }

    @Override
    public List<MessageBean> getMessageByThreadIdAndMemberId(int threadId, int memberId) {
        String sql = "SELECT * FROM message_view WHERE thread_id=? AND member_id=?;";
        String[] selectionArgs = {String.valueOf(threadId), String.valueOf(memberId)};
        return query(sql, selectionArgs);
    }

    private List<MessageBean> query(String sql, String[] selectionArgs) {
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, selectionArgs);
        if (cursor == null) {
            return null;
        }
        ArrayList<MessageBean> list = new ArrayList<MessageBean>();
        try {
            while (cursor.moveToNext()) {
                MessageBean messageBean = getMessageBean(cursor);
                list.add(messageBean);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @NonNull
    private MessageBean getMessageBean(Cursor cursor) {
        long id = cursor.getLong(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.ID));
        MessageBean messageBean = new MessageBean(id);
        messageBean.setThreadId(cursor.getLong(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.THREAD_ID)));
        messageBean.setServerThreadId(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.ThreadsColumns.SERVER_THREAD_ID)));
        messageBean.setMemberId(cursor.getLong(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.MEMBER_ID)));
        messageBean.setDeviceId(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.DEVICE_ID)));
        messageBean.setDate(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.DATE)));
        messageBean.setRead(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.READ)));
        messageBean.setType(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.TYPE)));
        messageBean.setText(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.TEXT)));
        messageBean.setUrl(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.URL)));
        messageBean.setLanguage(cursor.getString(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.LANGUAGE)));
        messageBean.setErrorCode(cursor.getInt(
                cursor.getColumnIndexOrThrow(DbConstants.MessageColumns.ERROR_CODE)));
        return messageBean;
    }

    @Override
    public long insert(MessageBean messageBean) {
        ContentValues values = buildContentValues(messageBean);
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        return database.insert(DbConstants.Tables.TABLE_MESSAGE,
                DbConstants.MessageColumns.MEMBER_ID, values);
    }

    @NonNull
    private ContentValues buildContentValues(MessageBean messageBean) {
        ContentValues values = new ContentValues();
        if (messageBean.getThreadId() > 0) {
            values.put(DbConstants.MessageColumns.THREAD_ID, messageBean.getThreadId());
        }
        values.put(DbConstants.MessageColumns.MEMBER_ID, messageBean.getMemberId());
        if (messageBean.getDate() != null) {
            values.put(DbConstants.MessageColumns.DATE, messageBean.getDate());
        }
        values.put(DbConstants.MessageColumns.READ, messageBean.getRead());
        if (messageBean.getType() >= DbConstants.MessageType.TYPE_SOUND &&
                messageBean.getType() <= DbConstants.MessageType.TYPE_SOUND_TEXT) {
            values.put(DbConstants.MessageColumns.TYPE, messageBean.getType());
        }
        if (messageBean.getText() != null) {
            values.put(DbConstants.MessageColumns.TEXT, messageBean.getText());
        }
        if (messageBean.getUrl() != null) {
            values.put(DbConstants.MessageColumns.URL, messageBean.getUrl());
        }
        if (messageBean.getLanguage() != null) {
            values.put(DbConstants.MessageColumns.LANGUAGE, messageBean.getLanguage());
        }
        values.put(DbConstants.MessageColumns.ERROR_CODE, messageBean.getErrorCode());
        return values;
    }

    @Override
    public int update(MessageBean messageBean) {
        ContentValues values = buildContentValues(messageBean);
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        String where = DbConstants.MessageColumns.ID + "=" + messageBean.getId();
        return database.update(DbConstants.Tables.TABLE_MESSAGE,
                values, where, null);
    }

    @Override
    public int delete(MessageBean messageBean) {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        String where = DbConstants.MessageColumns.ID + "=" + messageBean.getId();
        return database.delete(DbConstants.Tables.TABLE_MESSAGE, where, null);
    }

    @Override
    public int deleteMessageByThreadId(int threadId) {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        String where = DbConstants.MessageColumns.THREAD_ID + "=" + threadId;
        return database.delete(DbConstants.Tables.TABLE_MESSAGE, where, null);
    }

    @Override
    public void clean() {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        database.execSQL("DELETE FROM message;");
    }
}
