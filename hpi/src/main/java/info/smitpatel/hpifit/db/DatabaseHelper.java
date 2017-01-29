package info.smitpatel.hpifit.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import info.smitpatel.hpifit.app.HPIApp;

public class DatabaseHelper extends SQLiteOpenHelper{
    private static final String PREFIX = DatabaseHelper.class.getSimpleName() + ": ";

    private static final String DATABASE_NAME = "hpi_fit.db";
    private static final int DATABASE_VERSION = 1;

    DatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        HPIApp.logger(PREFIX, "onCreate()", Log.INFO);
        db.execSQL(Tables.UserInfo.CREATE_TABLE);
        db.execSQL(Tables.Milestones.CREATE_TABLE);
        db.execSQL(Tables.Progress.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: 1/27/17 transfer, drop and recreate tables based on version #
    }
}
