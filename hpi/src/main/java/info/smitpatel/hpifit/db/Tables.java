package info.smitpatel.hpifit.db;

public class Tables {
    private static final String COMMA = ",";
    private static final String TYPE_TEXT = " TEXT";

    /**
     * Table name, column names, create and drop query for UserInfo table
     */
    interface UserInfo {
        String TABLE_NAME = "user_info";

        interface Columns {
            String USERNAME = "username";
            String PASSWORD = "password";
            String FIRST_NAME = "first_name";
            String LAST_NAME = "last_name";
            String PROFILE_PIC = "profile_pic";
            String MILESTONES_COUNT = "milestones_count";
        }

        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + "(" + Columns.USERNAME + TYPE_TEXT +
                COMMA + Columns.PASSWORD + TYPE_TEXT +
                COMMA + Columns.FIRST_NAME + TYPE_TEXT +
                COMMA + Columns.LAST_NAME + TYPE_TEXT +
                COMMA + Columns.PROFILE_PIC + TYPE_TEXT +
                COMMA + Columns.MILESTONES_COUNT + TYPE_TEXT +
                COMMA + "UNIQUE (" + Columns.USERNAME + ") ON CONFLICT REPLACE);";

        String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Table name, column names, create and drop query for Milestones table
     */
    interface Milestones {
        String TABLE_NAME = "milestones";

        interface Columns {
            String USERNAME = "username";
            String DATE = "date";
            String TYPE = "type";
        }

        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + "(" + Columns.USERNAME + TYPE_TEXT +
                COMMA + Columns.DATE + TYPE_TEXT +
                COMMA + Columns.TYPE + TYPE_TEXT + ");";

        String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }

    /**
     * Table name, column names, create and drop query for Progress table
     */
    interface Progress {
        String TABLE_NAME = "progress";

        interface Columns {
            String USERNAME = "username";
            String DATE = "date";
            String STEPS = "steps";
            String MILESTONES_DAY = "milestones_day";
        }

        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME
                + "(" + Columns.USERNAME + TYPE_TEXT +
                COMMA + Columns.DATE + TYPE_TEXT +
                COMMA + Columns.STEPS + TYPE_TEXT +
                COMMA + Columns.MILESTONES_DAY + TYPE_TEXT +
                COMMA + "UNIQUE (" + Columns.USERNAME +
                COMMA + Columns.DATE + ") ON CONFLICT REPLACE);";

        String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    }
}
