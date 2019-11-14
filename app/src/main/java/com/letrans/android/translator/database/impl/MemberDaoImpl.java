package com.letrans.android.translator.database.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import com.letrans.android.translator.database.DbConstants;
import com.letrans.android.translator.database.DatabaseHelper;
import com.letrans.android.translator.database.MemberDao;
import com.letrans.android.translator.database.beans.MemberBean;

import java.util.ArrayList;
import java.util.List;

public class MemberDaoImpl implements MemberDao {
    private Context mContext;
    private DatabaseHelper mDatabaseHelper;

    public MemberDaoImpl(Context context, DatabaseHelper databaseHelper) {
        mContext = context;
        mDatabaseHelper = databaseHelper;
    }

    @Override
    public List<MemberBean> getAllMembers() {
        String sql = "SELECT * FROM member;";
        return query(sql);
    }

    private List<MemberBean> query(String sql) {
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, null);
        if (cursor == null) {
            return null;
        }
        ArrayList<MemberBean> list = new ArrayList<MemberBean>();
        try {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.ID));
                MemberBean memberBean = new MemberBean(id);
                memberBean.setDeviceId(cursor.getString(
                        cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.DEVICE_ID)));
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
                list.add(memberBean);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @Override
    public MemberBean getMemberById(int id) {
        String sql = "SELECT * FROM member WHERE _id = ?;";
        return query(sql, new String[]{String.valueOf(id)});
    }

    private MemberBean query(String sql, String[] selectionArgs) {
        SQLiteDatabase database = mDatabaseHelper.getReadableDatabase();
        Cursor cursor = database.rawQuery(sql, selectionArgs);
        if (cursor == null) {
            return null;
        }
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            long id = cursor.getLong(
                    cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.ID));
            MemberBean memberBean = new MemberBean(id);
            memberBean.setDeviceId(cursor.getString(
                    cursor.getColumnIndexOrThrow(DbConstants.MemberColumns.DEVICE_ID)));
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
        } finally {
            cursor.close();
        }
    }

    @Override
    public MemberBean getMemberByDeviceId(String deviceId) {
        String sql = "SELECT * FROM member WHERE device_id = ?;";
        return query(sql, new String[]{deviceId});
    }

    @Override
    public long insert(MemberBean memberBean) {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        ContentValues values = buildContentValues(memberBean);
        return database.insert(DbConstants.Tables.TABLE_MEMBER,
                DbConstants.UserColumns.DEVICE_ID, values);
    }

    @NonNull
    private ContentValues buildContentValues(MemberBean memberBean) {
        ContentValues values = new ContentValues();
        values.put(DbConstants.MemberColumns.DEVICE_ID, memberBean.getDeviceId());
        if (memberBean.getName() != null) {
            values.put(DbConstants.MemberColumns.NAME, memberBean.getName());
        }
        if (memberBean.getNickName() != null) {
            values.put(DbConstants.MemberColumns.NICK_NAME, memberBean.getNickName());
        }
        if (memberBean.getSex() > 0) {
            values.put(DbConstants.MemberColumns.SEX, memberBean.getSex());
        }
        if (memberBean.getPhotoId() > 0) {
            values.put(DbConstants.MemberColumns.PHOTO_ID, memberBean.getPhotoId());
        }
        if (memberBean.getLanguage() != null) {
            values.put(DbConstants.MemberColumns.LANGUAGE, memberBean.getLanguage());
        }
        if (memberBean.getDescription() != null) {
            values.put(DbConstants.MemberColumns.DESCRIPTION, memberBean.getDescription());
        }
        if (memberBean.getFavorite() > -1) {
            values.put(DbConstants.MemberColumns.FAVORITE, memberBean.getFavorite());
        }
        return values;
    }

    @Override
    public int update(MemberBean memberBean) {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        ContentValues values = buildContentValues(memberBean);
        return database.update(DbConstants.Tables.TABLE_MEMBER, values,
                "_id=" + memberBean.getId(), null);
    }

    @Override
    public int delete(MemberBean memberBean) {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        int count = database.delete(DbConstants.Tables.TABLE_MEMBER,
                "_id=" + memberBean.getId(), null);
        return count;
    }

    @Override
    public void clean() {
        SQLiteDatabase database = mDatabaseHelper.getWritableDatabase();
        database.execSQL("DELETE FROM member;");
    }
}
