package com.rnstepcounterdemo.steps.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.rnstepcounterdemo.MainActivity;
import com.rnstepcounterdemo.R;
import com.rnstepcounterdemo.steps.RNStepCounterModule;
import com.rnstepcounterdemo.steps.accelerometer.StepCount;
import com.rnstepcounterdemo.steps.accelerometer.UpdateUiCallBack;
import com.rnstepcounterdemo.steps.bean.DbUtils;
import com.rnstepcounterdemo.steps.bean.StepData;
import com.rnstepcounterdemo.steps.config.Constant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;


public class StepService extends Service implements SensorEventListener {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;
    private String TAG = "StepService";

    /**
     * Current date
     */
    private static String CURRENT_DATE = "";
    /**
     * Sensor management object
     */
    private SensorManager sensorManager;
    /**
     * Broadcast recipient
     */
    private BroadcastReceiver mBatInfoReceiver;
    /**
     * The number of steps currently taken
     */
    private int CURRENT_STEP;
    /**
     * Step sensor type Sensor.TYPE_STEP_COUNTER or Sensor.TYPE_STEP_DETECTOR
     */
    private static int stepSensorType = -1;
    /**
     * Whether the existing step count is obtained from the system each time the stepping service is started for the first time
     */
    private boolean hasRecord = false;
    /**
     * The number of existing steps obtained in the system
     */
    private int hasStepCount = 0;
    /**
     * Last step
     */
    private int previousStepCount = 0;
    /**
     * Number of steps taken in the acceleration sensor
     */
    private StepCount mStepCount;
    /**
     * IBinder object, a bridge to pass data to Activity
     */
    private StepBinder stepBinder = new StepBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d(TAG, "onCreate()");
        initNotification();
        initTodayData();
        initBroadcastReceiver();
        new Thread(new Runnable() {
            public void run() {
                startStepDetector();
            }
        }).start();
    }

    /**
     * Get today's date
     */
    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat(Constant.STEP_DATE_FORMAT, DEFAULT_LOCALE);
        return sdf.format(date);
    }

    /**
     * Initialize the notification bar
     */
    private void initNotification() {
        // Sets an ID for the notification, so it can be updated.
        int notifyID = 1;

        //Set click jump
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // The id of the channel.
            String channelId = "default_step";

            NotificationChannel mChannel = new NotificationChannel(channelId, getResources().getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);

            Notification notification = new Notification.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setContentTitle(getResources().getString(R.string.app_name))//Title or App name
                    .setContentText("Today's steps " + CURRENT_STEP + " step") // Message
                    .setChannelId(channelId)
                    .setAutoCancel(true)
                    .build();

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
                notificationManager.notify(notifyID, notification);
                startForeground(notifyId_Step, notification);
            }
            Timber.d(TAG, "initNotification()");
        } else {
            Intent hangIntent = new Intent(this, MainActivity.class);
            PendingIntent hangPendingIntent = PendingIntent.getActivity(this, 0, hangIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setContentTitle(getResources().getString(R.string.app_name))//Title or App name
                    .setContentText("Today's steps " + CURRENT_STEP + " step")
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())//The time when the notification is generated will be displayed in the notification message.
                    .setContentIntent(hangPendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(notifyID, notification.build());
                startForeground(notifyId_Step, notification.build());
            }
            Timber.d(TAG, "initNotification()");
        }
    }

    /**
     * Initialize the number of steps on the day
     */
    private void initTodayData() {
        CURRENT_DATE = getTodayDate();
        DbUtils.createDb(this, Constant.DB_NAME);
        DbUtils.getLiteOrm().setDebugged(false);
        //Get the data for the day for display
        List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "stepsDate", new String[]{CURRENT_DATE});
        if (list.size() == 0) {
            CURRENT_STEP = 0;
            StepData data = new StepData();
            data.setStepsDate(CURRENT_DATE);
            data.setStepsCount(CURRENT_STEP + "");

            data.setCaloriesBurned("0");
            data.setDistanceWalked("0");
            data.setWalkTime("0");
            data.setSyncToServer(false);

            DbUtils.insert(data);
        } else if (list.size() == 1) {
            Timber.v(TAG, "StepData=%s", list.get(0).toString());
            CURRENT_STEP = Integer.parseInt(list.get(0).getStepsCount());
        } else {
            Timber.v(TAG, "error!");
        }
        if (mStepCount != null) {
            mStepCount.setSteps(CURRENT_STEP);
        }
        updateNotification();
    }

    /**
     * Registration broadcast
     */
    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        // Screen off screen broadcast
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //Shutdown broadcast
        filter.addAction(Intent.ACTION_SHUTDOWN);
        // Screen bright broadcast
        filter.addAction(Intent.ACTION_SCREEN_ON);
        // Screen unlock broadcast
        // filter.addAction(Intent.ACTION_USER_PRESENT);
        // This broadcast will be issued when the power button is pressed for a long time to pop up the "Shutdown" dialog or lock screen.
        // example: Sometimes the system dialog is used, the permissions may be high, and it will be overlaid on the lock screen or on the "Shutdown" dialog.
        // So listen to this broadcast and hide your conversation when you receive it, such as clicking the pop-up dialog in the lower right corner of the pad.
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //Listening date change
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIME_TICK);

        mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    Timber.d(TAG, "screen on");
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    Timber.d(TAG, "screen off");
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    Timber.d(TAG, "screen unlock");
                } else if (Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(intent.getAction())) {
                    Timber.i(TAG, " receive Intent.ACTION_CLOSE_SYSTEM_DIALOGS");
                    //Save once
                    save();
                } else if (Intent.ACTION_SHUTDOWN.equals(intent.getAction())) {
                    Timber.i(TAG, " receive ACTION_SHUTDOWN");
                    save();
                } else if (Intent.ACTION_DATE_CHANGED.equals(action)) {//The date change step is reset to 0
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_CHANGED.equals(action)) {
                    //Time change step is reset to 0
                    save();
                    isNewDay();
                } else if (Intent.ACTION_TIME_TICK.equals(action)) {//The date change step is reset to 0
                    save();
                    isNewDay();
                }
            }
        };
        registerReceiver(mBatInfoReceiver, filter);
    }


    /**
     * Monitor the 0-point change initialization data at night
     */
    private void isNewDay() {
        String time = "00:00";
        if (time.equals(new SimpleDateFormat(Constant.STEP_TIME_FORMAT, DEFAULT_LOCALE).format(new Date())) || !CURRENT_DATE.equals(getTodayDate())) {
            initTodayData();
        }
    }

    /**
     * Update step notification
     */
    private void updateNotification() {
        //Set click jump
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Sets an ID for the notification, so it can be updated.
            int notifyID = 1;

            // The id of the channel.
            String channelId = "default_step";

            NotificationChannel mChannel = new NotificationChannel(channelId, getResources().getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            mChannel.enableVibration(false);

            Notification notification = new Notification.Builder(this, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setContentTitle(getResources().getString(R.string.app_name))//Title or App name
                    .setContentText("Today's steps " + CURRENT_STEP + " step") // Message
                    .setChannelId(channelId)
                    .setAutoCancel(true)
                    .build();

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
                notificationManager.notify(notifyID, notification);
            }

            if (mCallback != null) {
                mCallback.updateUi(CURRENT_STEP);
            }
            Timber.d(TAG, "updateNotification()");
        } else {
            Intent hangIntent = new Intent(this, MainActivity.class);
            PendingIntent hangPendingIntent = PendingIntent.getActivity(this, 0, hangIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                    .setContentTitle(getResources().getString(R.string.app_name))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setContentTitle(getResources().getString(R.string.app_name))//Title or App name
                    .setContentText("Today's steps " + CURRENT_STEP + " step")
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())//The time when the notification is generated will be displayed in the notification message.
                    .setContentIntent(hangPendingIntent);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(notifyId_Step, notification.build());
            }

            if (mCallback != null) {
                mCallback.updateUi(CURRENT_STEP);
            }
            Timber.d(TAG, "updateNotification()");
        }
        save();
    }

    /**
     * UI listener object
     */
    private UpdateUiCallBack mCallback;

    /**
     * Register UI update listener
     */
    public void registerCallback(UpdateUiCallBack paramICallback) {
        this.mCallback = paramICallback;
    }

    /**
     * Step ID of Notification
     */
    int notifyId_Step = 1;

    @Override
    public IBinder onBind(Intent intent) {
        return stepBinder;
    }

    /**
     * The link to pass data to the Activity
     */
    public class StepBinder extends Binder {

        /**
         * Get the current service object
         *
         * @return StepService
         */
        public StepService getService() {
            return StepService.this;
        }
    }

    /**
     * Get the current number of steps
     */
    public int getStepCount() {
        return CURRENT_STEP;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Get sensor instance
     */
    private void startStepDetector() {
        if (sensorManager != null) {
            sensorManager = null;
        }
        // Get an instance of the Sensor Manager
        sensorManager = (SensorManager) this
                .getSystemService(SENSOR_SERVICE);
        //Step counter sensor can be used after android4.4
        addCountStepListener();
    }

    /**
     * Add sensor monitoring
     * 1. The interpretation of the TYPE_STEP_COUNTER API says to return the number of steps counted since the boot was activated. When the phone is restarted, the data is zeroed.
     * The sensor is a hardware sensor so it is low power.
     * In order to continue the counting step, please do not register the event, even if the phone is in a dormant state, it still counts.
     * The number of steps will still be reported when activated. The sensor is suitable for long-term step counting needs.
     * <p>
     * 2. TYPE_STEP_DETECTOR translates to walk detection,
     * The API documentation does say this, the sensor is only used to monitor the walk, returning the number 1.0 each time.
     * Use TYPE_STEP_COUNTER if you need a step count for long events.
     */
    private void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_COUNTER;
            Timber.v("SKK Sensor.TYPE_STEP_COUNTER");
            Timber.v(TAG, "Sensor.TYPE_STEP_COUNTER");
            sensorManager.registerListener(StepService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (detectorSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_DETECTOR;
            Timber.v("SKK Sensor.TYPE_STEP_DETECTOR");
            Timber.v(TAG, "Sensor.TYPE_STEP_DETECTOR");
            sensorManager.registerListener(StepService.this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Timber.v(TAG, "Count sensor not available!");
            addBasePedometerListener();
        }
    }

    /**
     * Sensor monitoring callback
     * Key code for step
     * 1. The interpretation of the TYPE_STEP_COUNTER API says to return the number of steps counted since the boot was activated. When the phone is restarted, the data is zeroed.
     * The sensor is a hardware sensor so it is low power.
     * In order to continue the counting step, please do not register the event, even if the phone is in a dormant state, it still counts.
     * The number of steps will still be reported when activated. The sensor is suitable for long-term step counting needs.
     * <p>
     * 2. TYPE_STEP_DETECTOR translates to walk detection,
     * The API documentation does say this, the sensor is only used to monitor the walk, returning the number 1.0 each time.
     * Use TYPE_STEP_COUNTER if you need a step count for long events.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mBatInfoReceiver != null) {
            if (stepSensorType == Sensor.TYPE_STEP_COUNTER) {
                //Get the number of temporary steps returned by the current sensor
                int tempStep = (int) event.values[0];
                //For the first time, if you have not obtained the number of steps already in the mobile phone system, you will get the number of steps in the system that the APP has not started counting.
                if (!hasRecord) {
                    hasRecord = true;
                    hasStepCount = tempStep;
                    Timber.d("SKK -----------------------------------");
                    Timber.d("SKK inside has record");
                    Timber.d("SKK hasStepCount %s", hasStepCount);
                    Timber.d("SKK tempStep1 %s", tempStep);
                } else {
                    //
                    //Get the total number of steps that the APP opens to now = the total number of steps of the system callback - the number of steps that exist before the APP is opened
                    int thisStepCount = tempStep - hasStepCount;
                    //The number of valid steps = (the total number of steps recorded after the APP is opened - the total number of steps recorded after the last APP is opened)
                    int thisStep = thisStepCount - previousStepCount;
                    //Total number of steps = existing steps + current effective steps
                    CURRENT_STEP += (thisStep);
                    //Record the total number of steps since the last APP was opened
                    previousStepCount = thisStepCount;
                    Timber.d("SKK -----------------------------------");
                    Timber.d("SKK has record already true");
                    Timber.d("SKK tempStep2 %s", tempStep);
                    Timber.d("SKK CURRENT_STEP %s", CURRENT_STEP);
                    Timber.d("SKK previousStepCount %s", previousStepCount);
                }
            } else if (stepSensorType == Sensor.TYPE_STEP_DETECTOR) {
                if (event.values[0] == 1.0) {
                    CURRENT_STEP++;
                    Timber.d("SKK -----------------------------------");
                    Timber.d("SKK TYPE_STEP_DETECTOR");
                    Timber.d("SKK CURRENT_STEP %s", CURRENT_STEP);
                }
            }
            updateNotification();
        }
    }

    /**
     * Step by accelerometer
     */
    private void addBasePedometerListener() {
        mStepCount = new StepCount();
        mStepCount.setSteps(CURRENT_STEP);

        // Get the type of sensor, the type obtained here is the acceleration sensor
        // This method is used for registration and will only take effect if it is registered. Parameters: instance of SensorEventListener, instance of Sensor, update rate
        Sensor sensor = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        boolean isAvailable = sensorManager.registerListener(mStepCount.getStepDetector(), sensor,
                SensorManager.SENSOR_DELAY_UI);
        mStepCount.initListener(steps -> {
            CURRENT_STEP = steps;
            updateNotification();
        });
        if (isAvailable) {
            Timber.v(TAG, "Acceleration sensor can be used");
        } else {
            Timber.v(TAG, "Acceleration sensor is not available");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Save step data
     */
    private void save() {
        int tempStep = CURRENT_STEP;

        List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "stepsDate", new String[]{CURRENT_DATE});
        if (list.size() == 0) {
            StepData data = new StepData();
            data.setStepsDate(CURRENT_DATE);
            data.setStepsCount(tempStep + "");

            data.setCaloriesBurned("0");
            data.setDistanceWalked("0");
            data.setWalkTime("0");
            data.setSyncToServer(false);

            DbUtils.insert(data);
        } else if (list.size() == 1) {
            StepData data = list.get(0);
            data.setStepsCount(tempStep + "");

            data.setCaloriesBurned("0");
            data.setDistanceWalked("0");
            data.setWalkTime("0");
            data.setSyncToServer(false);

            DbUtils.update(data);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        //Cancel the foreground process
        stopForeground(true);
        DbUtils.closeDb();

        if (mBatInfoReceiver != null) {
            Timber.d("SKK %s", "unregisterReceiver mBatInfoReceiver");

            unregisterReceiver(mBatInfoReceiver);
            mBatInfoReceiver = null;
        }
        sensorManager = null;
        if (mStepCount != null)
            mStepCount.initListener(null);
        Timber.d("SKK %s", " stepService close");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }
}
