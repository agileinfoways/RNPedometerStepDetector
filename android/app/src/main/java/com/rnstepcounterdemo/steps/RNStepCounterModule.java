package com.rnstepcounterdemo.steps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.IllegalViewOperationException;
import com.rnstepcounterdemo.steps.bean.DbUtils;
import com.rnstepcounterdemo.steps.bean.SharedPreferencesUtils;
import com.rnstepcounterdemo.steps.bean.StepData;
import com.rnstepcounterdemo.steps.config.Constant;
import com.rnstepcounterdemo.steps.service.StepService;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RNStepCounterModule extends ReactContextBaseJavaModule {
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final ReactApplicationContext reactContext;
//    private static final String TAG = "RNStepCounterModule";

    private SharedPreferencesUtils sp;
    private StepService mStepService;
    private boolean isBind = false;
    private Intent mServiceIntent;
    private ComponentName mName;

    RNStepCounterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @NotNull
    @Override
    public String getName() {
        return "RNStepCounterModule";
    }

    @ReactMethod
    public void initStepper() {
        initialization();
    }

    //this method is like default android onStart method
    @ReactMethod
    public void onStart() {
        if (mStepService != null) {
            int stepCount = mStepService.getStepCount();
            sp.setParam(Constant.PLAN_WALK_COUNT, stepCount);
        }
    }

    //this method is like default android onStop method
    @ReactMethod
    public void onStop() {
        if (mStepService != null) {
            int stepCount = mStepService.getStepCount();
            sp.setParam(Constant.PLAN_WALK_COUNT, stepCount);
        }
    }

    //this method is like default android onDestroy method
    @ReactMethod
    public void onDestroy() {
        if (mStepService != null) {
            int stepCount = mStepService.getStepCount();
            sp.setParam(Constant.PLAN_WALK_COUNT, stepCount);
        }
    }

    @ReactMethod
    public void startStepper() {
        setupService();
    }

    @ReactMethod
    public void stopStepper() {
        if (mServiceIntent != null) {
            serviceConnection.onServiceDisconnected(mName);
        } else {
            if (mStepService != null && mStepService.getStepCount() > 0) {
                mServiceIntent = new Intent(reactContext, StepService.class);
                removeData();
            }
        }
    }

    @ReactMethod
    public void getPastTime(int pastDays, Callback successCallback, Callback errorCallback) {
        try {
            Date date = new Date(System.currentTimeMillis());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_YEAR, -(pastDays));
            Date newDate = calendar.getTime();
//            Timber.e("SKK" + date);
            SimpleDateFormat sdf = new SimpleDateFormat(Constant.STEP_DATE_FORMAT, DEFAULT_LOCALE);

            List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "stepsDate",
                    new String[]{sdf.format(newDate)});

            if (!list.isEmpty()) {
                StepData data = list.get(0);
                successCallback.invoke(Integer.parseInt(data.getStepsCount()));
            } else {
                successCallback.invoke(0);
            }
        } catch (IllegalViewOperationException e) {
            errorCallback.invoke(e.getMessage());
        }
    }

    private void initialization() {
        sp = new SharedPreferencesUtils(reactContext);
        int stepCount = (int) sp.getParam(Constant.PLAN_WALK_COUNT, 0);
        returnSteps(stepCount);

        setupService();
    }

    private void sendEvent(ReactContext reactContext,
                           String eventName,
                           @Nullable WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    private void returnSteps(int stepCount) {
        WritableMap params = Arguments.createMap();
        params.putString("stepCount", MessageFormat.format("{0} STEPS", stepCount));

        sendEvent(reactContext, "StepReminder", params);
    }

    private void setupService() {
        if (mServiceIntent == null) {
            mServiceIntent = new Intent(reactContext, StepService.class);
            isBind = reactContext.bindService(mServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            reactContext.startService(mServiceIntent);
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        /**
         *This method is called when establishing a connection to the Service. At present, Android implements the connection with the service through the IBind mechanism.
         * @param name The name of the Service component to which it is actually connected
         * @param service IBind of the communication channel of the service, which can access the corresponding service through the Service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mName = name;
            mStepService = ((StepService.StepBinder) service).getService();

            mStepService.registerCallback(stepCount -> {
//                Toast.makeText(reactContext, "return in service", Toast.LENGTH_SHORT).show();
                sp.setParam(Constant.PLAN_WALK_COUNT, stepCount);
                returnSteps(stepCount);
            });
        }

        /**
         * This method is called when the connection to the Service is lost.，
         * This often happens when the process where the Service is located crashes or is called by Kill.
         * This method does not remove the connection to the Service and will still call onServiceConnected() when the service is restarted.
         * @param name Missing connected component name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            removeData();
        }
    };

    private void removeData() {
        //update db
        List<StepData> list = DbUtils.getQueryByWhere(StepData.class, "stepsDate", new String[]{getTodayDate()});

        if (!list.isEmpty()) {
            returnSteps(0);

            save(list);

            sp.remove(Constant.PLAN_WALK_COUNT);
            reactContext.stopService(mServiceIntent);
            mStepService.onTaskRemoved(mServiceIntent);
            mServiceIntent = null;

            if (isBind) {
                reactContext.unbindService(serviceConnection);
                isBind = false;
            }
        }
    }

    private void save(List<StepData> list) {
        if (list.size() == 0) {
            StepData data = new StepData();
            data.setStepsDate(getTodayDate());
            data.setStepsCount(0 + "");
            data.setCaloriesBurned("0");
            data.setDistanceWalked("0");
            data.setWalkTime("0");
            data.setSyncToServer(false);

            DbUtils.insert(data);
        } else if (list.size() == 1) {
            StepData data = list.get(0);
            data.setStepsCount(0 + "");
            data.setCaloriesBurned("0");
            data.setDistanceWalked("0");
            data.setWalkTime("0");
            data.setSyncToServer(false);

            DbUtils.update(data);
        }
    }

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat(Constant.STEP_DATE_FORMAT, DEFAULT_LOCALE);
        return sdf.format(date);
    }
}
