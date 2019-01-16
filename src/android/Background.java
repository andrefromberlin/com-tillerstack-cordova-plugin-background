/*
       Licensed under MIT. 
       
       Contact Tillerstack GmbH for additional information
       regarding further information

         http://www.tillerstack.com

       Unless required by applicable law or agreed to in writing,
       software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
       CONDITIONS OF ANY KIND, either express or implied.

 */

package com.tillerstack.cordova.plugin.background;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.os.Build;
import android.util.Log;

import java.util.Date;

import android.annotation.TargetApi;

/*
 * A Condat AG Cordova 3.0.0+ Android Plugin to inform a calling App context about
 * environment state changes. Internally this plugin observers power state changes
 * (dreaming state off/on, screen off/on) via a custom BroadcastReceiver implementation.
 * Additionally it sets and cancels system alarms to be woken up on certain events.
 *
 * This plugin can also be used to trigger and publish any environment state change
 * (e.g. activity went to background) from the calling app.
 */
public class Background extends CordovaPlugin  {

    // Constant value identifying the requested action to register to device power state changes
    private static final String ACTION_REGISTER_DEVICE_POWER_CHANGES = "registerDevicePowerChanges";
    // Constant value identifying the requested action to unregister from device power state changes
    private static final String ACTION_UNREGISTER_DEVICE_POWER_CHANGES = "unregisterDevicePowerChanges";
    // Constant value identifying the requested action to set a system alarm via Android AlarmManager
    private static final String ACTION_SET_ALARM = "setAlarm";
    // Constant value identifying the requested action to cancel a system alarm from Android AlarmManager
    private static final String ACTION_CANCEL_ALARM = "cancelAlarm";
    // Constant value identifying the requested action to report the last StartUp Timestamp
    private static final String ACTION_GET_STARTUP_TIMESTAMP = "getStartupTimestamp";

    // Constant value identifying the unique application intent to use for wakeup-calls from Android system to the Activity
    private static final String INTENT_ALARM_WAKEUP_ONCE = "de.condat.ssc.mobile.WAKEUP_ONCE";

    // Constant value identifying the device state change, that the dreaming mode was started
    private static final String STATE_DEVICE_DREAMING_STARTED = "DEVICE_DREAMING_STARTED";
    // Constant value identifying the device state change, that the dreaming mode was stopped
    private static final String STATE_DEVICE_DREAMING_STOPPED = "DEVICE_DREAMING_STOPPED";
    // Constant value identifying the device state change, that the screen has been switched off (both manually and automatically)
    private static final String STATE_DEVICE_SCREEN_OFF = "DEVICE_SCREEN_OFF";
    // Constant value identifying the device state change, that the screen has been switched on (both manually and automatically)
    private static final String STATE_DEVICE_SCREEN_ON = "DEVICE_SCREEN_ON";
    // Constant value identifying the state change, that the Activity has been woken up externally from the Android AlarmManager
    private static final String STATE_ALARM_WAKEUP_ONCE = "ALARM_WAKEUP_ONCE";

    // Constant value identifying the app state change, that the container activity was started
    private static final String STATE_ACTIVITY_STARTED = "ACTIVITY_STARTED";
    // Constant value identifying the app state change, that the container activity was paused
    private static final String STATE_ACTIVITY_PAUSED  = "ACTIVITY_PAUSED";
    // Constant value identifying the app state change, that the container activity was resumed
    private static final String STATE_ACTIVITY_RESUMED = "ACTIVITY_RESUMED";
    // Constant value identifying the app state change, that the container activity was stopped
    private static final String STATE_ACTIVITY_STOPPED = "ACTIVITY_STOPPED";
    // Constant value identifying the app state change, that the container activity has destroyed
    private static final String STATE_ACTIVITY_DESTROYED = "ACTIVITY_DESTROYED";

    // Constant value representing the unique log label for this plugin class
    private static final String LOG_TAG = "BackgroundPlugin";
    // Constant value representing the unique JSON parameter name
    private static final String JSON_KEY_NAME = "state";

    // reference to the wrapping broadcast receiver implementation for device state changes
    public BackgroundBroadcastReceiver backgroundReceiver;
    // reference to the wrapping broadcast receiver implementation for AlarmManager events
    public AlarmBroadcastReceiver alarmReceiver;

    // class variable storing the plugin callback context for BackgroundBroadcastReceiver, to be referenceable from different calling objects
    // (both Android App Wrapper Activity and this custom Intent Listener implementation)
    private static CallbackContext callbackContext_backgroundReceiver;
    // class variable storing the plugin callback context for AlarmBroadcastReceiver
    private static CallbackContext callbackContext_alarmReceiver;
    // class variable switching between both available plugin contexts, always holding the proper one to use for callbacks
    private static CallbackContext callbackContext;
    // reference to the underlying application context of the activity
    private Context applicationContext = null;

    // class variable indicating any successful requested plugin usage registration of BackgroundBroadcastReceiver, to be referencable from different calling objects
    // (both Android App Wrapper Activity and this custom Intent Listener implementation)
    private static boolean registered__backgroundReceiver = false;
    // class variable indicating any successful requested plugin usage registration of AlarmBroadcastReceiver
    private static boolean registered__alarmReceiver = false;

    // class variable as a pseudo Singleton-Pattern implementation
    private static Background instance = null;

    // Das einmalig beim instanziieren gesetzte Datum soll Neustarts erkennbar machen.
    private final Date startUpTime;

    // Hashcode des "this" Objekts, um paralelle Instanzen im Log zu triggern.
    private final int hash;

    /*
     * Constructor.
     * Is default-called by Android-Plugin-Mechanism. Therefore, has to be public and does not allow
     * any Singleton-Pattern implementation
     */
    public Background(){
        this.startUpTime = new Date();
        this.backgroundReceiver = null;
        this.alarmReceiver = null;
        //set the pseudo Singleton reference
        instance = this;
        this.hash = this.hashCode();

        Log.i(LOG_TAG, "Constructor 'Background()' called @" + this.hash);
    }

    /*
     * Pseudo Singleton implementation for hook-in of the app lifecycle wrapper of the Android activity into
     * the plugin context and its initialised class members
     */
    public static Background getInstance(){
        if (instance == null)
        {
            synchronized(Background.class) {
                if (instance == null)
                    instance = new Background();
            }
        }
        Log.i(LOG_TAG, "Method 'getInstance()' called: will return @" + instance.hashCode());
        return instance;
    }

    /*
     * Overridden implementation of the standard entry-point method for any external plugin calls
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext newCallbackContext) throws JSONException {

        Log.i(LOG_TAG, "Method 'execute()' called @" + this.hash);

        Log.d(LOG_TAG, "Executing action " + action);

        if (action.equals(ACTION_REGISTER_DEVICE_POWER_CHANGES))
        {
            // setting the callback context for accessing the callbacks
            callbackContext_backgroundReceiver = newCallbackContext;
            callbackContext = callbackContext_backgroundReceiver;
            // start broadcast receiving of device power changes
            startBackgroundPowerListener();

            // preparing a PluginResult for submitting return value
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            // keep the callbacks and their context for constant reuse in communictation
            pluginResult.setKeepCallback(true);
            // send a result back to the calling Cordova implementation
            callbackContext.sendPluginResult(pluginResult);

            return true;
        }
        else if (action.equals(ACTION_UNREGISTER_DEVICE_POWER_CHANGES))
        {
            // stop broadcast receiving of device power changes
            removeBackgroundPowerListener();

            // preparing a PluginResult for submitting return value
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            // releasing the callbacks and their context, so no reuse is possible
            pluginResult.setKeepCallback(true);
            // send a result back to the calling Cordova implementation
            callbackContext.sendPluginResult(pluginResult);

            // resetting the callback contexts
            callbackContext_backgroundReceiver = null;
            callbackContext = null;

            return true;
        }
        else if (action.equals(ACTION_SET_ALARM))
        {
            // setting the callback context for accessing the callbacks
            callbackContext_alarmReceiver = newCallbackContext;
            callbackContext = callbackContext_alarmReceiver;

            setAlarmReceiver();
            this.applicationContext = cordova.getActivity().getApplicationContext();

            int secondsForNextAlarm;
            try
            {
                // expects the relevant info in first - and only - position
                secondsForNextAlarm = args.getInt(0);
                Log.i(LOG_TAG,"secondsForNextAlarm:"+secondsForNextAlarm);
            }
            catch (Exception ex)
            {
                Log.w(LOG_TAG,"Parameter konnte nicht extrahiert werden. Verwende default 60 Sekunden. args:"+(args != null ? args : "null") + ". ("+ex.getMessage()+")",ex);
                // default: 60
                secondsForNextAlarm = 60;
            }

            AlarmManager am = (AlarmManager)this.applicationContext.getSystemService(Context.ALARM_SERVICE);
            //Intent intent = new Intent(this.applicationContext, AlarmBroadcastReceiver.class);
            Intent intent = new Intent(INTENT_ALARM_WAKEUP_ONCE);
            PendingIntent pi = PendingIntent.getBroadcast(this.applicationContext, 0, intent, 0);
            
            // Caution: set exact alarm differently on Android Versions greater and equal to API Level 19 (Android 4.4)
            
            /* API Level Overwiew:
             * SDK_INT value        Build.VERSION_CODES        Human Version Name       
             *     1                  BASE                      Android 1.0 (no codename)
             *     2                  BASE_1_1                  Android 1.1 Petit Four
             *     3                  CUPCAKE                   Android 1.5 Cupcake
             *     4                  DONUT                     Android 1.6 Donut
             *     5                  ECLAIR                    Android 2.0 Eclair
             *     6                  ECLAIR_0_1                Android 2.0.1 Eclair                  
             *     7                  ECLAIR_MR1                Android 2.1 Eclair
             *     8                  FROYO                     Android 2.2 Froyo
             *     9                  GINGERBREAD               Android 2.3 Gingerbread
             *    10                  GINGERBREAD_MR1           Android 2.3.3 Gingerbread
             *    11                  HONEYCOMB                 Android 3.0 Honeycomb
             *    12                  HONEYCOMB_MR1             Android 3.1 Honeycomb
             *    13                  HONEYCOMB_MR2             Android 3.2 Honeycomb
             *    14                  ICE_CREAM_SANDWICH        Android 4.0 Ice Cream Sandwich
             *    15                  ICE_CREAM_SANDWICH_MR1    Android 4.0.3 Ice Cream Sandwich
             *    16                  JELLY_BEAN                Android 4.1 Jellybean
             *    17                  JELLY_BEAN_MR1            Android 4.2 Jellybean
             *    18                  JELLY_BEAN_MR2            Android 4.3 Jellybean
             *    19                  KITKAT                    Android 4.4 KitKat
             *    20                  KITKAT_WATCH              Android 4.4 KitKat Watch
             *    21                  LOLLIPOP                  Android 5.0 Lollipop
             *    22                  LOLLIPOP_MR1              Android 5.1 Lollipop
             *    23                  M                         Android 6.0 Marshamallow
             *    23                  M                         Android 6.0.1 Marshamallow
             *    24                  N                         Android 7.0 Nougat
             *    25                  N_MR1                     Android 7.1 Nougat
             *    25                  N_MR1                     Android 7.1.1 Nougat
             *   10000                CUR_DEVELOPMENT           Current Development Build
             */

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                Log.i(LOG_TAG, "Set alarm mode pre Api-Level 19");
                // fire in x seconds from now
                am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000*secondsForNextAlarm, pi);
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.i(LOG_TAG, "Set alarm mode pre Api-Level 23");
                // fire in x seconds from now
                setAlarmFromKitkat(am, System.currentTimeMillis() + 1000*secondsForNextAlarm, pi);
            } else {
                Log.i(LOG_TAG, "Set alarm mode for Api-Level 23+");
                // fire in x seconds from now
                setAlarmFromMarshmallow(am, System.currentTimeMillis() + 1000*secondsForNextAlarm, pi);
            }

            // preparing a PluginResult for submitting return value
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            // keep the callbacks and their context for constant reuse in communictation
            pluginResult.setKeepCallback(true);
            // send a result back to the calling Cordova implementation
            callbackContext.sendPluginResult(pluginResult);

            //resetting contexts
            callbackContext_alarmReceiver = null;
            callbackContext = callbackContext_backgroundReceiver;

            return true;
        }
        else if (action.equals(ACTION_CANCEL_ALARM))
        {
            //setting contexts
            callbackContext_alarmReceiver = newCallbackContext;
            callbackContext = callbackContext_alarmReceiver;

            this.applicationContext = cordova.getActivity().getApplicationContext();

            //Intent intent = new Intent(this.applicationContext, AlarmBroadcastReceiver.class);
            Intent intent = new Intent(INTENT_ALARM_WAKEUP_ONCE);
            PendingIntent sender = PendingIntent.getBroadcast(this.applicationContext, 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) this.applicationContext.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(sender);

            // preparing a PluginResult for submitting return value
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
            // keep the callbacks and their context for constant reuse in communictation
            pluginResult.setKeepCallback(true);
            // send a result back to the calling Cordova implementation
            callbackContext.sendPluginResult(pluginResult);

            removeAlarmReceiver();
            //resetting contexts
            callbackContext_alarmReceiver = null;
            callbackContext = callbackContext_backgroundReceiver;

            return true;
        }
        else if (action.equals(ACTION_GET_STARTUP_TIMESTAMP))
        {
            if (this.startUpTime != null) {
                newCallbackContext.success(Long.toString(this.startUpTime.getTime()));
            } else {
                newCallbackContext.error("startUpTime not set");
            }
        }
        return false;
    }

    /**
     * Provide a seperate Method implementation for API Level 19+ to annotate with proper TargetAPI. This avoids LINTing and linking errors.
     */
    @TargetApi(19)
    private void setAlarmFromKitkat(AlarmManager am, long ms, PendingIntent pi){
        Log.i(LOG_TAG, "Method 'setAlarmFromKitkat()' called @" + this.hash);
        am.setExact(AlarmManager.RTC, ms, pi);
    }

    /**
     * Provide a seperate Method implementation for API Level 23+ to annotate with proper TargetAPI. This avoids LINTing and linking errors.
     */
    @TargetApi(23)
    private void setAlarmFromMarshmallow(AlarmManager am, long ms, PendingIntent pi){
        Log.i(LOG_TAG, "Method 'setAlarmFromMarshmallow()' called @" + this.hash);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC, ms, pi);
    }

    /**
     * Overridden implementation for ensuring a defined plugin garbage collection.
     *
     * This references the calling activity's lifecycle methods.
     *
     * As for the missing methods (onCreate, onRestart, etc), having them would make no sense,
     * as the cordova activity is already created when whatever plugins enters the game.
     *
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy()
    {
        Log.i(LOG_TAG, "Method 'onDestroy()' called @" + this.hash);
        // Inform the app
        this.sendUpdate(STATE_ACTIVITY_DESTROYED);

        removeBackgroundPowerListener();
        removeAlarmReceiver();
    }

    /**
     * Overridden implementation for detecting a foreground mode switching of the app.
     *
     * This references the calling activity's lifecycle methods.
     *
     * As for the missing methods (onCreate, onRestart, etc), having them would make no sense,
     * as the cordova activity is already created when whatever plugins enters the game.
     *
     * Called when the activity is becoming visible to the user.
     */
    // https://github.com/apache/cordova-android/commit/a652d892ca93d077038310af50d2a40ab5fabfd6
    // @Override
    // public void onStart() {
    //     Log.i(LOG_TAG, "Method 'onStart()' called @" + this.hash);
    //     // Inform the app
    //     this.sendUpdate(STATE_ACTIVITY_STARTED);
    // }

    /**
     * Overridden implementation for detecting a background mode switching of the app.
     *
     * This references the calling activity's lifecycle methods.
     *
     * As for the missing methods (onCreate, onRestart, etc), having them would make no sense,
     * as the cordova activity is already created when whatever plugins enters the game.
     *
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onPause(boolean multitasking)
    {
        Log.i(LOG_TAG, "Method 'onPause()' called @" + this.hash);
        // Inform the app
        this.sendUpdate(STATE_ACTIVITY_PAUSED);
    }

    /**
     * Overridden implementation for detecting a foreground mode switching of the app.
     *
     * This references the calling activity's lifecycle methods.
     *
     * As for the missing methods (onCreate, onRestart, etc), having them would make no sense,
     * as the cordova activity is already created when whatever plugins enters the game.
     *
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onResume(boolean multitasking) {
        Log.i(LOG_TAG, "Method 'onResume()' called @" + this.hash);
        // Inform the app
        this.sendUpdate(STATE_ACTIVITY_RESUMED);
    }

    /**
     * Overridden implementation for detecting a background mode switching of the app.
     *
     * This references the calling activity's lifecycle methods.
     *
     * As for the missing methods (onCreate, onRestart, etc), having them would make no sense,
     * as the cordova activity is already created when whatever plugins enters the game.
     *
     * Called when the activity is no longer visible to the user.
     */
    // https://github.com/apache/cordova-android/commit/a652d892ca93d077038310af50d2a40ab5fabfd6
    // @Override
    // public void onStop() {
    //     Log.i(LOG_TAG, "Method 'onStop()' called @" + this.hash);
    //     // Inform the app
    //     this.sendUpdate(STATE_ACTIVITY_STOPPED);
    // }

    /*
     * Create a new plugin result and send it back to JavaScript
     * by constantly reusing the referenced unique callback context.
     *
     * This method is both used from inner Plugin BroadcastReceiver implementation and
     * Android app wrapper Activity
     */
    public void sendUpdate(String state)
    {
        Log.i(LOG_TAG, "Method 'sendUpdate(" + state + ")' called @" + this.hash);

        try
        {
            // creating a JSON object holding a single key-value-pair with changed state information
            JSONObject status = new JSONObject();
            status.put(JSON_KEY_NAME, state);

            //Log.i(LOG_TAG, "sendUpdate " + state);
            //Log.i(LOG_TAG, "registered__backgroundReceiver: " + registered__backgroundReceiver);
            //Log.i(LOG_TAG, "callbackContext_backgroundReceiver: " + (callbackContext_backgroundReceiver != null ? "OK" : "NULL"));

            if (callbackContext != null)
            {
                // preparing a PluginResult for submitting return value JSON object
                PluginResult result = new PluginResult(PluginResult.Status.OK, status);
                // keep the callbacks and their context for constant reuse in communictation
                result.setKeepCallback(true);
                // send a result back to the calling Cordova implementation
                callbackContext.sendPluginResult(result);
            }
            else
            {
                Log.e(LOG_TAG,"CallbackContext not set.");
            }
        }
        catch (Exception ex)
        {
            Log.e(LOG_TAG,"JSON Error - " + ex.getMessage(), ex);
        }
    }

    /*
     * Encapsulated initialization of the BroadcastReceiver implementation, including adding
     * the requested Intent.Actions as a registered device state changed listener
     */
    private void startBackgroundPowerListener()
    {
        Log.i(LOG_TAG, "Method 'startBackgroundPowerListener()' called @" + this.hash);
        Log.i(LOG_TAG, "Method 'startBackgroundPowerListener()': this.backgroundReceiver is "+ (this.backgroundReceiver == null ? "NULL" : "NOT NULL"));

        if(this.backgroundReceiver == null)
        {
            this.backgroundReceiver = new BackgroundBroadcastReceiver(this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_DREAMING_STARTED);
            intentFilter.addAction(Intent.ACTION_DREAMING_STOPPED);
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);
            this.cordova.getActivity().registerReceiver(backgroundReceiver, intentFilter);
            registered__backgroundReceiver = true;
        }
    }

    /*
     * Encapsulated removal of the BroadcastReceiver implementation from the device state changed listener
     */
    private void removeBackgroundPowerListener()
    {
        Log.i(LOG_TAG, "Method 'removeBackgroundPowerListener()' called @" + this.hash);

        if(this.backgroundReceiver != null && registered__backgroundReceiver)
        {
            try
            {
                this.cordova.getActivity().unregisterReceiver(this.backgroundReceiver);
                this.backgroundReceiver = null;
                registered__backgroundReceiver = false;
            }
            catch (final Exception e)
            {
                Log.w(LOG_TAG, "Error unregistering background power listener receiver: " + e.getMessage(), e);
            }
        }
    }

    /*
     * Encapsulated initialization of the AlarmBroadcastReceiver implementation, including adding
     * the requested unique Intent.Action INTENT_ALARM_WAKEUP_ONCE as a registered device broadcast listener
     */
    private void setAlarmReceiver()
    {
        Log.i(LOG_TAG, "Method 'setAlarmReceiver()' called @" + this.hash);
        Log.i(LOG_TAG, "Method 'setAlarmReceiver()': this.alarmReceiver is " + (this.alarmReceiver == null ? "NULL" : "NOT NULL"));

        if(this.alarmReceiver == null)
        {
            this.alarmReceiver = new AlarmBroadcastReceiver(this);
            IntentFilter intentFilter = new IntentFilter(INTENT_ALARM_WAKEUP_ONCE);
            this.cordova.getActivity().registerReceiver(this.alarmReceiver, intentFilter);
            registered__alarmReceiver = true;
        }
    }

    /*
     * Encapsulated removal of the AlarmBroadcastReceiver implementation from the device broadcast listener
     */
    private void removeAlarmReceiver()
    {
        Log.i(LOG_TAG, "Method 'removeAlarmReceiver()' called @" + this.hash);

        if(this.alarmReceiver != null && registered__alarmReceiver)
        {
            try
            {
                this.cordova.getActivity().unregisterReceiver(this.alarmReceiver);
                this.alarmReceiver = null;
                this.applicationContext = null;
                registered__alarmReceiver = false;
            }
            catch (final Exception e)
            {
                Log.w(LOG_TAG, "Error unregistering alarm receiver: " + e.getMessage(), e);
            }
        }
    }

    /*
     * Class implementing functionality to listen to the broadcast receiver for specific device power events.
     * In case a relevant event was caught, the calling Background is informed, which populates the
     * news to the javascript Cordova part.
     */
    public class BackgroundBroadcastReceiver extends BroadcastReceiver
    {
        // local reference to the calling plugin object
        protected Background watcher;

        /*
         * Constructor
         */
        public BackgroundBroadcastReceiver(Background watcher)
        {
            super();
            Log.i(LOG_TAG, "Constructor 'BackgroundBroadcastReceiver()' called @" + watcher.hash);
            // saving reference to calling plugin object for reuse
            this.watcher = watcher;
        }

        /*
         * Overridden implementation of the standard entry-point method for receiving
         * any published device broadcast events.
         *
         * Only the following Intent.Actions are consumed from the plugin and sent back
         * to the calling plugin and its context:
         * - ACTION_DREAMING_STARTED
         * - ACTION_DREAMING_STOPPED
         * - ACTION_SCREEN_OFF
         * - ACTION_SCREEN_ON
         */
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(LOG_TAG, "Method 'onReceive()' called @" + this.watcher.hash);

            String action = intent.getAction();

            Log.d(LOG_TAG, "Intent received: " + action);

            if( (action.compareTo(Intent.ACTION_DREAMING_STARTED)) == 0 )
            {
                watcher.sendUpdate(STATE_DEVICE_DREAMING_STARTED);
            }
            else if( (action.compareTo(Intent.ACTION_DREAMING_STOPPED)) == 0 )
            {
                watcher.sendUpdate(STATE_DEVICE_DREAMING_STOPPED);
            }
            else if( (action.compareTo(Intent.ACTION_SCREEN_OFF)) == 0 )
            {
                watcher.sendUpdate(STATE_DEVICE_SCREEN_OFF);
            }
            else if( (action.compareTo(Intent.ACTION_SCREEN_ON)) == 0 )
            {
                watcher.sendUpdate(STATE_DEVICE_SCREEN_ON);
            }
        }
    }

    /*
     * Class implementing functionality to listen to the broadcast receiver for specific broadcast events.
     * In case the expected custom event released from the Android AlarmManager was caught, the calling Background is informed, which populates the
     * news to the javascript Cordova part.
     */
    public class AlarmBroadcastReceiver extends BroadcastReceiver
    {
        // local reference to the calling plugin object
        protected Background watcher;

        /*
         * Constructor
         */
        public AlarmBroadcastReceiver(Background watcher)
        {
            super();
            Log.i(LOG_TAG, "Constructor 'AlarmBroadcastReceiver()' called @" + watcher.hash);
            // saving reference to calling plugin object for reuse
            this.watcher = watcher;
        }

        /* Overridden implementation of the standard entry-point method for receiving
         * any published device broadcast events.
         *
         * Only the Intent.Action with the custom action STATE_ALARM_WAKEUP_ONCE is consumed
         * from the receiver and sent back to the calling plugin and its context.
         */
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Log.i(LOG_TAG, "Method 'onReceive()' called @" + this.watcher.hash);

            Log.i(LOG_TAG, "AlarmBroadcastReceiver - Intent caught: " + intent.getAction());

            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            //Acquire the lock
            wl.acquire();

            //Log.i(LOG_TAG,"AlarmBroadcastReceiver - send event to Cordova Plugin Background Container");
            this.watcher.sendUpdate(STATE_ALARM_WAKEUP_ONCE);
            Log.i(LOG_TAG, "AlarmBroadcastReceiver - event was sent to Cordova Plugin Background Container");

            //Release the lock
            wl.release();
        }
    }
}
