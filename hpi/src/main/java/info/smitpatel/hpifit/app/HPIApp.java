package info.smitpatel.hpifit.app;

import android.app.Application;
import android.content.Context;
import android.support.compat.BuildConfig;
import android.util.Log;

import com.google.common.io.Files;
import com.google.firebase.crash.FirebaseCrash;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import info.smitpatel.hpifit.R;

public class HPIApp extends Application {
    private static final String TAG = "HPIApp";
    private static final String PREFIX = HPIApp.class.getSimpleName() + ": ";
    private static Context context;

    private static File logFile;

    /**
     * all keys and codes requires within the application for any type of preference
     */
    public interface Prefs {
        String SHARED_KEY_IS_LOGGED_IN = "is_logged_in";
        String SHARED_KEY_LAST_USER = "last_user";
        String SHARED_KEY_CURRENT_STEPS = "current_steps";
        String SHARED_KEY_TOTAL_STEPS = "total_steps";
        String SHARED_KEY_MILESTONES_TODAY = "milestones_today";
        String SHARED_KEY_DATE = "todays_date";
        String SHARED_KEY_RUN_SERVICE = "run_service";

        String BUNDLE_KEY_USER_PROFILE = "user_profile";

        int CODE_IMAGE_SELECT = 0x99;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(HPIApp.context == null) {
            HPIApp.context = getApplicationContext();
        }

        if(BuildConfig.DEBUG) {
            initializeLogFile();
        }
    }

    /**
     * @return Application Context
     */
    public static Context getAppContext() {
        return HPIApp.context;
    }

    /**
     * Method to log data to the system console.
     * Also, it will log the data to FirebaseConsole.
     * If the logType is ERROR, it will report a crash to Firebase API
     * @param prefix name of the class trying to log
     * @param log any message
     * @param logType type of the log
     */
    public static void logger(String prefix, String log, int logType) {
        FirebaseCrash.logcat(logType, TAG, prefix + log);

        if(logType == Log.ERROR) {
            FirebaseCrash.report(new Exception(prefix + log));
        }

        if(BuildConfig.DEBUG) {
            try {
                if(logFile == null) {
                    initializeLogFile();
                }

                String newLine = System.getProperty("line.separator");

                if(!logFile.exists()) {
                    Files.write(getLogTime() + prefix + log + newLine, logFile, Charset.forName("UTF-8"));
                } else {
                    Files.append(getLogTime() + prefix + log + newLine, logFile, Charset.forName("UTF-8"));
                }
            } catch (IOException e) {
                Log.e(TAG, PREFIX + "logger IOException: " + e);
            } catch (Exception e) {
                Log.e(TAG, PREFIX + "logger Exception: " + e);
            }
        }
    }

    /**
     * gets the current date time for debug log purposes
     * @return date and time in the following format: MM-dd-yyyy'T'h:mm:ss.
     */
    private static String getLogTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy'T'HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date()) + ": ";
    }

    private static void initializeLogFile() {
        File logFolder = context.getFilesDir();

        boolean folderAvail = true;

        if(!logFolder.exists()) {
            folderAvail = logFolder.mkdir();
        }

        if(folderAvail && logFile == null) {
            logFile = new File(logFolder, context.getString(R.string.app_log_fileName));
        }
    }
}
