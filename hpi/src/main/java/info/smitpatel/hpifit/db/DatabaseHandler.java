package info.smitpatel.hpifit.db;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import info.smitpatel.hpifit.app.HPIApp;
import info.smitpatel.hpifit.models.Milestone;
import info.smitpatel.hpifit.models.Progress;
import info.smitpatel.hpifit.models.UserProfile;

public class DatabaseHandler {
    private static final String PREFIX = DatabaseHandler.class.getSimpleName() + ": ";

    private Context context;

    @SuppressLint("StaticFieldLeak")
    private static DatabaseHandler instance;

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private DatabaseHandler(Context context) {
        this.context = context;
    }

    /**
     * Gets instance of a DatabaseHandler. Returns one if already exists
     * @param context context
     * @return DatabaseHandler
     */
    public static synchronized DatabaseHandler getInstance(final Context context) {
        if(instance == null) {
            instance = new DatabaseHandler(context);
        }
        return instance;
    }

    /**
     * opens a writable connection to the Database
     */
    private void openWritableDb() {
        if(databaseHelper == null) {
            databaseHelper = new DatabaseHelper(context);
        }
        database = databaseHelper.getWritableDatabase();
    }

    /**
     * opens a readable connection to the Database
     */
    private void openReadableDb() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper(context);
        }
        database = databaseHelper.getReadableDatabase();
    }

    /**
     * closes connection to Database and DatabaseHelper
     */
    private void closeDb() {
        if(database != null) {
            database.close();
        }

        if(databaseHelper != null) {
            databaseHelper.close();
        }
    }

    /**
     * Gets UserProfile based on the username
     * @param username username
     * @return UserProfile
     */
    public UserProfile getUserProfile(String username) {
        HPIApp.logger(PREFIX, "getUserProfile()", Log.INFO);

        UserProfile userProfile = null;

        try {
            openReadableDb();

            String[] projections = {
                    Tables.UserInfo.Columns.USERNAME,
                    Tables.UserInfo.Columns.PASSWORD,
                    Tables.UserInfo.Columns.FIRST_NAME,
                    Tables.UserInfo.Columns.LAST_NAME,
                    Tables.UserInfo.Columns.PROFILE_PIC,
                    Tables.UserInfo.Columns.MILESTONES_COUNT
            };

            String selection = Tables.UserInfo.Columns.USERNAME + " = ?";
            String[] selectionArgs = { username };

            Cursor cursor = database.query(
                    Tables.UserInfo.TABLE_NAME,
                    projections, selection, selectionArgs,
                    null, null, null,
                    "1"
            );

            while (cursor.moveToNext()) {
                userProfile = new UserProfile();
                userProfile.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(Tables.UserInfo.Columns.USERNAME)));
                userProfile.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(Tables.UserInfo.Columns.PASSWORD)));
                userProfile.setFirstName(cursor.getString(cursor.getColumnIndexOrThrow(Tables.UserInfo.Columns.FIRST_NAME)));
                userProfile.setLastName(cursor.getString(cursor.getColumnIndexOrThrow(Tables.UserInfo.Columns.LAST_NAME)));

                try {
                    userProfile.setProfilePic(cursor.getString(cursor.getColumnIndexOrThrow(Tables.UserInfo.Columns.PROFILE_PIC)));
                } catch (Exception e) {
                    HPIApp.logger(PREFIX, e.toString(), Log.ERROR);
                    userProfile.setProfilePic("");
                }

                userProfile.setMilestonesCount(cursor.getString(cursor.getColumnIndexOrThrow(Tables.UserInfo.Columns.MILESTONES_COUNT)));
            }

            cursor.close();
            closeDb();
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "getUserProfile exception: " + e, Log.ERROR);
        }

        return userProfile;
    }

    /**
     * saves UserProfile to local database
     * @param userProfile UserProfile
     * @return true if successfully saved, false otherwise
     */
    public boolean saveUserProfile(UserProfile userProfile) {
        HPIApp.logger(PREFIX, "saveUserProfile()", Log.INFO);

        boolean success = false;

        if(userProfile == null) {
            HPIApp.logger(PREFIX, "provided userProfile is null!", Log.ERROR);
            return false;
        }

        try {
            openWritableDb();
            ContentValues values = new ContentValues();
            values.put(Tables.UserInfo.Columns.USERNAME, userProfile.getUsername());
            values.put(Tables.UserInfo.Columns.PASSWORD, userProfile.getPassword());
            values.put(Tables.UserInfo.Columns.FIRST_NAME, userProfile.getFirstName());
            values.put(Tables.UserInfo.Columns.LAST_NAME, userProfile.getLastName());

            if(userProfile.getProfilePic() != null && !userProfile.getProfilePic().isEmpty()) {
                values.put(Tables.UserInfo.Columns.PROFILE_PIC, userProfile.getProfilePic());
            }

            values.put(Tables.UserInfo.Columns.MILESTONES_COUNT, String.valueOf(userProfile.getMilestonesCount()));

            database.insert(Tables.UserInfo.TABLE_NAME, null, values);

            closeDb();
            success = true;
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "saveUserProfile exception: " + e, Log.ERROR);
        }

        return success;
    }

    /**
     * saves path of the user profile pic file
     * @param username username
     * @param profilePic full path that represents a image file
     * @return true if successfully saved, false otherwise
     */
    public boolean saveUserProfilePic(String username, String profilePic) {
        HPIApp.logger(PREFIX, "saveUserProfilePic()", Log.INFO);

        boolean success = false;

        if(username == null || username.isEmpty()) {
            HPIApp.logger(PREFIX, "provided username is null or empty!!", Log.ERROR);
            return false;
        }

        try {
            openWritableDb();
            ContentValues values = new ContentValues();
            values.put(Tables.UserInfo.Columns.PROFILE_PIC, profilePic);

            String selection = Tables.UserInfo.Columns.USERNAME + "=?";
            String[] args = { username };

            database.update(Tables.UserInfo.TABLE_NAME, values, selection, args);

            closeDb();
            success = true;
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "saveUserProfilePic exception: " + e, Log.ERROR);
        }

        return success;
    }

    /**
     * adds a milestone to users history
     * @param milestone milestone
     * @return true if successfully saved, false otherwise
     */
    public boolean addMilestone(Milestone milestone) {
        HPIApp.logger(PREFIX, "addMilestone()", Log.INFO);

        boolean success = false;

        if(milestone == null) {
            HPIApp.logger(PREFIX, "milestone is null!!", Log.ERROR);
            return false;
        }

        try {
            openWritableDb();
            ContentValues values = new ContentValues();
            values.put(Tables.Milestones.Columns.USERNAME, milestone.getUsername());
            values.put(Tables.Milestones.Columns.DATE, milestone.getDate());
            values.put(Tables.Milestones.Columns.TYPE, milestone.getType());

            database.insert(Tables.Milestones.TABLE_NAME, null, values);

            closeDb();
            success = true;
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "addMilestone exception: " + e, Log.ERROR);
        }

        return success;
    }

    /**
     * Gets Progress for a user for a specific day
     * @param username username to get the progress for
     * @param date specific date to get the progress for
     * @return Progress object is data is available, null otherwise
     */
    public Progress getDayProgress(String username, String date) {
        HPIApp.logger(PREFIX, "getDayProgress()", Log.INFO);

        Progress progress = null;

        if(username == null || username.isEmpty() ||
                date == null || date.isEmpty()) {
            HPIApp.logger(PREFIX, "missing params. abort!", Log.ERROR);
            return null;
        }

        try {
            openReadableDb();

            String[] projections = {
                    Tables.Progress.Columns.USERNAME,
                    Tables.Progress.Columns.DATE,
                    Tables.Progress.Columns.STEPS,
                    Tables.Progress.Columns.MILESTONES_DAY
            };

            String selection = Tables.Progress.Columns.USERNAME + "=? and " +
                    Tables.Progress.Columns.DATE + "=?";
            String[] selectionArgs = { username, date };

            Cursor cursor = database.query(
                    Tables.Progress.TABLE_NAME,
                    projections, selection, selectionArgs,
                    null, null, null,
                    "1"
            );

            while (cursor.moveToNext()) {
                progress = new Progress();
                progress.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.USERNAME)));
                progress.setDate(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.DATE)));
                progress.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.STEPS)));
                progress.setMilestones_day(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.MILESTONES_DAY)));
            }

            cursor.close();
            closeDb();
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "getDayProgress exception: " + e, Log.ERROR);
        }

        return progress;
    }

    /**
     * Save or Update a day's progress
     * @param progress object that represents a day's progress
     * @return true is successfully saved, false otherwise
     */
    public boolean updateDayProgress(Progress progress) {
        HPIApp.logger(PREFIX, "getDayProgress()", Log.INFO);

        boolean success = false;

        if(progress == null) {
            HPIApp.logger(PREFIX, "given progress is null! not saved!", Log.DEBUG);
            return false;
        }

        try {
            openWritableDb();
            ContentValues values = new ContentValues();
            values.put(Tables.Progress.Columns.USERNAME, progress.getUsername());
            values.put(Tables.Progress.Columns.DATE, progress.getDate());
            values.put(Tables.Progress.Columns.STEPS, progress.getSteps());
            values.put(Tables.Progress.Columns.MILESTONES_DAY, progress.getMilestones_day());

            database.insert(Tables.Progress.TABLE_NAME, null, values);

            closeDb();
            success = true;
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "updateDayProgress exception: " + e, Log.ERROR);
        }

        return success;
    }

    /**
     * gets all steps history of a user
     * @param username username to get the history for
     * @return ArrayList of Progress that represents total user history
     */
    public ArrayList<Progress> getHistoryByUser(String username) {
        HPIApp.logger(PREFIX, "getHistoryByUser()", Log.INFO);

        ArrayList<Progress> progresses = new ArrayList<>();

        if(username == null || username.isEmpty()) {
            HPIApp.logger(PREFIX, "invalid username. abort!", Log.ERROR);
            return progresses;
        }

        try {
            openReadableDb();

            String [] projections = {
                    Tables.Progress.Columns.USERNAME,
                    Tables.Progress.Columns.DATE,
                    Tables.Progress.Columns.STEPS,
                    Tables.Progress.Columns.MILESTONES_DAY
            };

            String selection = Tables.Progress.Columns.USERNAME + "=?";
            String[] args = { username };

            Cursor cursor = database.query(
                    Tables.Progress.TABLE_NAME,
                    projections, selection, args,
                    null, null, null
            );

            Progress progress;

            while (cursor.moveToNext()) {
                progress = new Progress();
                progress.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.USERNAME)));
                progress.setDate(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.DATE)));
                progress.setSteps(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.STEPS)));
                progress.setMilestones_day(cursor.getString(cursor.getColumnIndexOrThrow(Tables.Progress.Columns.MILESTONES_DAY)));
                progresses.add(progress);
            }

            cursor.close();
            closeDb();
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "getHistoryByUser exception: " + e, Log.ERROR);
        }

        return progresses;
    }

    /**
     * gets all Milestones from the Milestone table based on the provided username
     * @param username username to get the milestones for
     * @return ArrayList of milestones with no date restrictions
     */
    private ArrayList<Milestone> getMilestonesByUser(String username) {
        HPIApp.logger(PREFIX, "getMilestonesByUser()", Log.INFO);

        ArrayList<Milestone> milestones = new ArrayList<>();

        if(username == null || username.isEmpty()) {
            HPIApp.logger(PREFIX, "invalid username. abort!", Log.ERROR);
            return milestones;
        }

        try {
            openReadableDb();

            String[] projections = {
                    Tables.Milestones.Columns.USERNAME,
                    Tables.Milestones.Columns.DATE,
                    Tables.Milestones.Columns.TYPE
            };

            String selection = Tables.Milestones.Columns.USERNAME + "=?";
            String[] args = { username };

            Cursor cursor = database.query(
                    Tables.Progress.TABLE_NAME,
                    projections, selection, args,
                    null, null, null
            );

            Milestone milestone;

            while (cursor.moveToNext()) {
                milestone = new Milestone(
                        cursor.getString(cursor.getColumnIndexOrThrow(Tables.Milestones.Columns.USERNAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Tables.Milestones.Columns.DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(Tables.Milestones.Columns.TYPE))
                );
                milestones.add(milestone);
            }

            cursor.close();
            closeDb();
        } catch (Exception e) {
            HPIApp.logger(PREFIX, "getMilestonesByUser exception: " + e, Log.ERROR);
        }

        return milestones;
    }

}
