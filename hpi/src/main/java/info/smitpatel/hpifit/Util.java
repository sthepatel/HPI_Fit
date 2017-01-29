package info.smitpatel.hpifit;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import info.smitpatel.hpifit.app.HPIApp;

public class Util {
    private static final String PREFIX = Util.class.getSimpleName() + ": ";

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor preferencesEditor;

    /**
     * gets todays date in yyyy-MM-dd format
     * @return date
     */
    public static String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String todaysDate = dateFormat.format(calendar.getTime());
        HPIApp.logger(PREFIX, "todaysDate=" + todaysDate, Log.DEBUG);
        return todaysDate;
    }

    /**
     * gets any previous date in yyyy-MM-dd format. date is based on the provided number
     * @param minusToday number used to subtract todays date with to get the previous date
     * @return previous date
     */
    public static String getPrevDate(int minusToday) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, minusToday);
        String yesterdaysDate = dateFormat.format(calendar.getTime());
        HPIApp.logger(PREFIX, "yesterdaysDate=" + yesterdaysDate, Log.DEBUG);
        return yesterdaysDate;
    }

    /**
     * local flag to determine the last login/logout state of the application
     * @return try if logged in, false otherwise
     */
    public static boolean isLoggedIn() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HPIApp.getAppContext());
        boolean isLoggedIn = sharedPreferences.getBoolean(HPIApp.Prefs.SHARED_KEY_IS_LOGGED_IN, false);
        HPIApp.logger(PREFIX, "isLoggedIn=" + isLoggedIn, Log.DEBUG);
        return isLoggedIn;
    }

    /**
     * variable that represents last user that was logged in
     * @return username
     */
    public static String getUsername() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(HPIApp.getAppContext());
        String username = sharedPreferences.getString(HPIApp.Prefs.SHARED_KEY_LAST_USER, "");
        HPIApp.logger(PREFIX, "username=" + username, Log.DEBUG);
        return username;
    }

    /**
     * changes the local login/logout flag
     * @param flag flag to save
     */
    public static void setLoggedInFlag(boolean flag) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(HPIApp.getAppContext()).edit();
        editor.putBoolean(HPIApp.Prefs.SHARED_KEY_IS_LOGGED_IN, flag);
        editor.apply();
    }

    /**
     * saving the username to SharedPreferences
     * @param username username to save
     */
    public static void saveUsername(String username) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(HPIApp.getAppContext()).edit();
        editor.putString(HPIApp.Prefs.SHARED_KEY_LAST_USER, username);
        editor.apply();
    }

    /**
     * gets SharedPreferences specifically for that user based on the username
     * @return SharedPreferences
     */
    public static SharedPreferences getUserSharedPreferences() {
        String username = getUsername();
        sharedPreferences = HPIApp.getAppContext().getSharedPreferences(username, Context.MODE_PRIVATE);
        return sharedPreferences;
    }

    /**
     * gets SharedPreferences.Editor specifically for that user based on the username
     * @return SharedPreferences.Editor
     */
    public static SharedPreferences.Editor getUserPreferencesEditor() {
        getUserSharedPreferences();
        preferencesEditor = sharedPreferences.edit();
        return preferencesEditor;
    }

    /**
     * returns local flag that determines if the last application state for user for running the service or not
     * @return true if run the service, false otherwise
     */
    public static boolean runService() {
        String username = getUsername();
        sharedPreferences = HPIApp.getAppContext().getSharedPreferences(username, Context.MODE_PRIVATE);
        boolean runService = sharedPreferences.getBoolean(HPIApp.Prefs.SHARED_KEY_RUN_SERVICE, false);
        HPIApp.logger(PREFIX, "runService=" + runService, Log.DEBUG);
        return runService;
    }

    /**
     * saves flag that determines if service should automatically run next time
     * @param run true if run the service, false otherwise
     */
    public static void updateRunService(boolean run) {
        getUserPreferencesEditor();
        preferencesEditor.putBoolean(HPIApp.Prefs.SHARED_KEY_RUN_SERVICE, run);
        preferencesEditor.apply();
    }

    /**
     * this will reset all local values for a user to 0. Basically, start a new day!
     */
    public static void resetTodaysStats() {
        getUserPreferencesEditor();
        preferencesEditor.putInt(HPIApp.Prefs.SHARED_KEY_CURRENT_STEPS, 0);
        preferencesEditor.putInt(HPIApp.Prefs.SHARED_KEY_TOTAL_STEPS, 0);
        preferencesEditor.putInt(HPIApp.Prefs.SHARED_KEY_MILESTONES_TODAY, 0);
        preferencesEditor.apply();
    }
}
