package com.letrans.android.translator.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "translator.db";
    private static final int DATABASE_VERSION = 1;
    private Context mContext;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
        createTriggers(db);
    }

    private void createTriggers(SQLiteDatabase db) {
        db.execSQL(DbConstants.INSERT_MESSAGE_TRIGGER);
        db.execSQL(DbConstants.UPDATE_MESSAGE_TRIGGER);
        db.execSQL(DbConstants.DELETE_MESSAGE_TRIGGER);
        db.execSQL(DbConstants.DELETE_THREADS_TRIGGER);
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL(DbConstants.CREATE_USER_TABLE);
        db.execSQL(DbConstants.CREATE_MEMBER_TABLE);
        db.execSQL(DbConstants.CREATE_MESSAGE_TABLE);
        db.execSQL(DbConstants.CREATE_THREADS_TABLE);
        db.execSQL(DbConstants.CREATE_PHOTO_TABLE);
        db.execSQL(DbConstants.CREATE_MESSAGE_VIEW);

        db.execSQL(DbConstants.CREATE_CITY_TABLE);
        db.execSQL(DbConstants.CREATE_PAIR_CODE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // dropTables(db);
            // When upgrade database, must delete trigger and create trigger again.
            // dropTriggers(db);
        }
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.Tables.TABLE_USER+ ";");
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.Tables.TABLE_MEMBER + ";");
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.Tables.TABLE_MESSAGE + ";");
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.Tables.TABLE_THREADS + ";");
        db.execSQL("DROP TABLE IF EXISTS " + DbConstants.Tables.TABLE_PHOTO + ";");
    }

    private void dropTriggers(SQLiteDatabase db) {
        db.execSQL("DROP TRIGGER IF EXISTS insert_message_trigger;");
        db.execSQL("DROP TRIGGER IF EXISTS update_message_trigger;");
        db.execSQL("DROP TRIGGER IF EXISTS delete_message_trigger;");
        db.execSQL("DROP TRIGGER IF EXISTS delete_threads_trigger;");
    }
}
