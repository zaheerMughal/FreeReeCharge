package Database;

import android.net.Uri;
import android.provider.BaseColumns;

public class DbContract {

    public static final String AUTHORITY = "Database";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://"+AUTHORITY);




    private DbContract(){}


    public static class CHECKED_NUMBERS implements BaseColumns
    {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(CHECKED_NUMBERS.TABLE_NAME).build();

        public static final String TABLE_NAME = "CheckedNumbersTable"; // path to this table
        public static final String COLUMN_NUMBER = "Number";
    }

}
