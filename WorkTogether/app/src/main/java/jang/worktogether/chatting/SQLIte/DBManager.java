package jang.worktogether.chatting.SQLIte;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBManager extends SQLiteOpenHelper{

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "chatting_log.db";

    public DBManager(Context context, String id) {
        super(context, id + DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FeedReaderContract.FeedEntry.SQL_CREATE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(FeedReaderContract.FeedEntry.SQL_DELETE_QUERY);
        onCreate(db);
    }
}
