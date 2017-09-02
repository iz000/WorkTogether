package jang.worktogether.chatting.SQLIte;

import android.provider.BaseColumns;

public final class FeedReaderContract {

    public FeedReaderContract(){ }

    public static abstract class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "chatting_log";
        public static final String COLUMN_NAME_CHAT_NUM = "chat_num";
        public static final String COLUMN_NAME_GROUP_ID = "group_id";
        public static final String COLUMN_NAME_CHAT_ID = "chat_id";
        public static final String COLUMN_NAME_CHAT_TIME = "chat_time";
        public static final String COLUMN_NAME_USER_ID = "user_id";
        public static final String COLUMN_NAME_CHAT_CONTENT = "chat_content";
        public static final String COLUMN_NAME_CHAT_TYPE = "chat_type";
        public static final String COLUMN_NAME_CHAT_READ = "chat_read";

        public static final String SQL_CREATE_QUERY =
                "CREATE TABLE " + FeedReaderContract.FeedEntry.TABLE_NAME + "(" +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_NUM + " integer primary key," +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID + " integer not null," +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID + " integer not null," +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TIME + " integer not null," +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_USER_ID + " integer not null," +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_CONTENT + " text not null," +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE + " text check("+
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_TYPE +
                        " IN('text','file','image','enter','out')) not null default 'text'," +
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_READ + " integer not null); " +
                        "CREATE INDEX chatting_index on "+ FeedReaderContract.FeedEntry.TABLE_NAME +
                        "("+ FeedReaderContract.FeedEntry.COLUMN_NAME_GROUP_ID + ", "+
                        FeedReaderContract.FeedEntry.COLUMN_NAME_CHAT_ID+");";

        public static final String SQL_DELETE_QUERY =
                "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;
    }
}
