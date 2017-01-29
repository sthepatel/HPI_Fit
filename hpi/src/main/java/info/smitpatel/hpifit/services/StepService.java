package info.smitpatel.hpifit.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

import info.smitpatel.hpifit.HomeActivity;
import info.smitpatel.hpifit.R;
import info.smitpatel.hpifit.SplashActivity;
import info.smitpatel.hpifit.Util;
import info.smitpatel.hpifit.app.HPIApp;
import info.smitpatel.hpifit.db.DatabaseHandler;
import info.smitpatel.hpifit.models.Milestone;
import info.smitpatel.hpifit.models.UserProfile;

public class StepService extends Service implements SensorEventListener {
    private static final String PREFIX = StepService.class.getSimpleName() + ": ";

    private UserProfile userProfile;
    private SensorManager sensorManager;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private final int HOUR_IN_MILLIS = 3600000;
    private final int SEC_IN_MILLIS = 1000;
    private int TIME_SINCE_ACTION = 0;

    private boolean isNotificationShowing;

    private interface NotificationType {
        String MILESTONE = "199";
        String NO_ACTION = "299";
    }

//    private static final int MILESTONE_1000_FEET = 10;        // 10 for just testing
    private static final int MILESTONE_1000_FEET = 400;        // 400 steps == 1000 feet

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        HPIApp.logger(PREFIX, "onBind()", Log.INFO);
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_STEP_DETECTOR:
                updateStepValue(event.values.length);
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        HPIApp.logger(PREFIX, "onAccuracyChanged()", Log.INFO);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HPIApp.logger(PREFIX, "onDestroy()", Log.INFO);
        unregisterListener();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HPIApp.logger(PREFIX, "onCreate()", Log.INFO);

        sensorManager = (SensorManager) HPIApp.getAppContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
//        Sensor counterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (detectorSensor == null) {
            HPIApp.logger(PREFIX, "STEP_DETECTOR sensor is missing!! Service cannot continue!", Log.ERROR);
            stopSelf();
            return;
        }

        sensorManager.registerListener(this, detectorSensor, SensorManager.SENSOR_DELAY_FASTEST, 0);
//        sensorManager.registerListener(this, counterSensor, SensorManager.SENSOR_DELAY_FASTEST, 0);

        sharedPreferences = Util.getUserSharedPreferences();
        editor = Util.getUserPreferencesEditor();
        getUserProfile();

        editor.putString(HPIApp.Prefs.SHARED_KEY_DATE, Util.getTodaysDate());
        editor.apply();

        new CountDown(HOUR_IN_MILLIS, SEC_IN_MILLIS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }

    /**
     * this will unregister the step detector sensor
     */
    private void unregisterListener() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    /**
     * Returns UserProfile based on the username
     * @return UserProfile
     */
    private UserProfile getUserProfile() {
        if (userProfile == null) {
            String username = Util.getUsername();

            DatabaseHandler databaseHandler = DatabaseHandler.getInstance(StepService.this);
            userProfile = databaseHandler.getUserProfile(username);
        }

        return userProfile;
    }

    /**
     * method to update current and total steps values in SharedPreferences
     * @param count amount of steps to add to current and total steps
     */
    public void updateStepValue(int count) {
        if (editor == null) {
            editor = Util.getUserPreferencesEditor();
        }
        updateTotalStepsValue(count);
        updateCurrentStepsValue(count);
    }

    /**
     * Updates total daily steps values in SharedPreferences for the user
     * @param count amount of steps to add to current steps
     */
    private void updateTotalStepsValue(int count) {
        if (editor == null) {
            editor = Util.getUserPreferencesEditor();
        }
        int prevSteps = sharedPreferences.getInt(HPIApp.Prefs.SHARED_KEY_TOTAL_STEPS, 0);
        int newSteps = prevSteps + count;
        editor.putInt(HPIApp.Prefs.SHARED_KEY_TOTAL_STEPS, newSteps);
        editor.apply();
    }

    /**
     * Updates current steps values in SharedPreferences for the user.
     * Also, show notification if the new steps count is equal to a milestone
     * @param count amount of steps to add to current steps
     */
    private void updateCurrentStepsValue(int count) {
        int prevSteps = sharedPreferences.getInt(HPIApp.Prefs.SHARED_KEY_CURRENT_STEPS, 0);
        int newSteps = prevSteps + count;

        TIME_SINCE_ACTION = 0;

        if(isNotificationShowing) {
            cancelNotification(Integer.valueOf(NotificationType.NO_ACTION));
        }

        if (newSteps >= MILESTONE_1000_FEET) {
            HPIApp.logger(PREFIX, "MILESTONE reached", Log.DEBUG);
            newSteps = 0;

            int prevMilestones = sharedPreferences.getInt(HPIApp.Prefs.SHARED_KEY_MILESTONES_TODAY, 0);
            int newMilestones = ++prevMilestones;

            HPIApp.logger(PREFIX, "Milestones Today: " + newMilestones, Log.DEBUG);

            editor.putInt(HPIApp.Prefs.SHARED_KEY_MILESTONES_TODAY, newMilestones);
            editor.apply();

            showNotification(NotificationType.MILESTONE);

            DatabaseHandler databaseHandler = DatabaseHandler.getInstance(StepService.this);
            databaseHandler.addMilestone(new Milestone(Util.getUsername(), getLogTime(), Milestone.Type.FEET_1000));
        }

        editor.putInt(HPIApp.Prefs.SHARED_KEY_CURRENT_STEPS, newSteps);
        editor.apply();
    }

    /**
     * gets the current time for in MM-dd-yyyy'T'HH:mm:ss format
     * @return string of current time
     */
    private static String getLogTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy'T'HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date()) + ": ";
    }

    /**
     * shows notification based on the provided type
     * @param type one of the declared notification types. See {@code NotificationType}
     */
    private void showNotification(String type) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(this);

        if(type.equals(NotificationType.MILESTONE)) {
            builder.setContentTitle("Milestone");
            builder.setContentText("Great job! You've reached a milestone.");
        } else if (type.equals(NotificationType.NO_ACTION)) {
            builder.setContentTitle("It's been too long!");
            builder.setContentText("Get up and walk about a bit.");
        }

        builder.setSmallIcon(R.drawable.ic_notification);
        builder.setAutoCancel(true);
        builder.setWhen(System.currentTimeMillis());

        // this should be set as home activity, however handles are not done for this sample
        Intent intent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setContentIntent(pendingIntent);

        // this should be done for deleteIntent so when user clears it, timer can be restarted
//        Intent delIntent = new Intent()

        if(type.equals(NotificationType.MILESTONE)) {
            notificationManager.notify(Integer.valueOf(NotificationType.MILESTONE), builder.build());
        } else {
            notificationManager.notify(Integer.valueOf(NotificationType.NO_ACTION), builder.build());
            isNotificationShowing = true;
        }
    }

    /**
     * clears notification from top bar
     * @param messageId id of the notification to clear from the top bar
     */
    private void cancelNotification(int messageId) {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(messageId);
        isNotificationShowing = false;
    }

    /**
     * this will add min
     */
    private class CountDown extends CountDownTimer {
        CountDown(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

            // FIXME: 1/29/17 THIS is a HACK!!
            // Instead of just checking the hour, AlarmManager service should be used
            // to start the timer at 9am and stop at 5pm

            if(isItOfficeHours()) {
                // time is between 9am and 5pm so add a second to the time
                TIME_SINCE_ACTION += SEC_IN_MILLIS;
            } else {
                // time is not between 9am and 5pm so reset the timer
                TIME_SINCE_ACTION = 0;
            }
        }

        @Override
        public void onFinish() {
            showNotification(NotificationType.NO_ACTION);
        }
    }

    /**
     * checks if current time is between 9am and 5pm.
     * This feature can be a user setting where user can modify the time
     * @return true if current time is between 9am and 5pm, false otherwise
     */
    private boolean isItOfficeHours() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour >= 9 && hour < 17;
    }
}
