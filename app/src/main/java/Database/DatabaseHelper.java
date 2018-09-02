package Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "CheckedNumbers.db";
    public static final int DATABASE_VERSION = 1;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        final String CREATE_CHECKED_NUMBERS_TABLE = "CREATE TABLE "+DbContract.CHECKED_NUMBERS.TABLE_NAME+
                "(" +
                DbContract.CHECKED_NUMBERS._ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
                DbContract.CHECKED_NUMBERS.COLUMN_NUMBER+" INTEGER NOT NULL" +
                ");";
        db.execSQL(CREATE_CHECKED_NUMBERS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String DROP_CHECKED_NUMBERS_TABLE = "DROP TABLE IF EXISTS "+ DbContract.CHECKED_NUMBERS.TABLE_NAME;
        db.execSQL(DROP_CHECKED_NUMBERS_TABLE);
        onCreate(db);
    }

}
