# Documentation

Following items are considered while integrating pedometer step counter application:

- Add _lite-orm-1.7.0.jar_ inside lib folder
- Add dependencies line in _build.gradle_ file of app level

```markdown
    implementation 'com.jakewharton.timber:timber:4.7.1'
    implementation files('lib/lite-orm-1.7.0.jar')
```

- Add permission, uses-feature and service declaration in _AndroidManifest.xml_ file.

```markdown
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <uses-feature
        android:name="android.hardware.sensor.stepdetector"
        android:required="true" />
    <uses-feature android:name="android.hardware.sensor.accelerometer" />

    <application>
    ...
        <!--Step Counter Services-->
        <service
            android:name=".steps.service.StepService"
            android:priority="1000">
            <intent-filter>
                <!-- Will be called after the system is started-->
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.DATE_CHANGED" />
                <action android:name="android.intent.action.MEDIA_MOUNTED" />
                <action android:name="android.intent.action.USER_PRESENT" />
                <action android:name="android.intent.action.ACTION_TIME_TICK" />
                <action android:name="android.intent.action.ACTION_POWER_CONNECTED" />
                <action android:name="android.intent.action.ACTION_POWER_DISCONNECTED" />
            </intent-filter>
        </service>
    ...
    </application>
```

- Add steps module inside your _java package_.
- Add package of step module inside _MainApplication.java_ file.
    >packages.add(new RNStepCounterPackage());
- Integrate native bridge and call methods that are specified in _HomeScreen.js_ file.
