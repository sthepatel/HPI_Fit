package info.smitpatel.hpifit.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import info.smitpatel.hpifit.Util;
import info.smitpatel.hpifit.app.HPIApp;
import info.smitpatel.hpifit.db.DatabaseHandler;
import info.smitpatel.hpifit.models.Progress;
import info.smitpatel.hpifit.services.StepService;

public class MyMainReceiver extends BroadcastReceiver {
    private static final String PREFIX = MyMainReceiver.class.getSimpleName() + ": ";

    @Override
    public void onReceive(Context context, Intent intent) {
        HPIApp.logger(PREFIX, "onReceive()", Log.INFO);

        if(!Util.runService()) {
            return;
        }

        String action = intent.getAction();
        HPIApp.logger(PREFIX, "action=" + action, Log.DEBUG);

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                checkDates();
                startService();
                break;
            case Intent.ACTION_DATE_CHANGED:
            case Intent.ACTION_TIME_CHANGED:
                checkDates();
                break;
            default:
                break;
        }
    }

    /**
     * Compares saved date is SharedPreferences with Current (Todays) date.
     * If dates do NOT match, it will save SharedPreferences data to Database and
     * update the SharedPreferences date
     */
    private void checkDates() {
        String newDate = Util.getTodaysDate();
        String savedDate = getSavedDate();

        HPIApp.logger(PREFIX, "newDate=" + newDate, Log.DEBUG);
        HPIApp.logger(PREFIX, "savedDate=" + savedDate, Log.DEBUG);

        if(!newDate.equals(savedDate)) {
            updateSavedDate(newDate);
            saveYesterdaysData(savedDate);
        } else {
            HPIApp.logger(PREFIX, "Still the same day! ignore it!", Log.DEBUG);
        }
    }

    /**
     * Start Step service
     */
    private void startService() {
        Intent intent = new Intent(HPIApp.getAppContext(), StepService.class);
        HPIApp.getAppContext().startService(intent);
    }

    /**
     * gets the saved date in SharedPreferences
     * @return date that was last saved in SharedPreferences
     */
    private String getSavedDate() {
        SharedPreferences preferences = Util.getUserSharedPreferences();
        String date = preferences.getString(HPIApp.Prefs.SHARED_KEY_DATE, "");
        HPIApp.logger(PREFIX, "savedDate=" + date, Log.DEBUG);
        return date;
    }

    /**
     * saved the provided date to SharedPreferences
     * @param date date
     */
    private void updateSavedDate(String date) {
        SharedPreferences.Editor editor = Util.getUserPreferencesEditor();
        editor.putString(HPIApp.Prefs.SHARED_KEY_DATE, date);
        editor.apply();
    }

    /**
     * saved the data from SharedPreferences for a user for the provided date
     * @param date date
     */
    private void saveYesterdaysData(String date) {
        SharedPreferences preferences = Util.getUserSharedPreferences();

        String username = Util.getUsername();

        int currentSteps = preferences.getInt(HPIApp.Prefs.SHARED_KEY_CURRENT_STEPS, 0);
        int daySteps = preferences.getInt(HPIApp.Prefs.SHARED_KEY_TOTAL_STEPS, 0);
        int milestones = preferences.getInt(HPIApp.Prefs.SHARED_KEY_MILESTONES_TODAY, 0);

        int totalDaySteps = currentSteps + daySteps;

        Util.resetTodaysStats();

        DatabaseHandler databaseHandler = DatabaseHandler.getInstance(HPIApp.getAppContext());
        Progress progress = new Progress(username, date, totalDaySteps, milestones);
        databaseHandler.updateDayProgress(progress);
    }
}
