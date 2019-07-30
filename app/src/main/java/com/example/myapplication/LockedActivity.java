package com.example.myapplication;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class LockedActivity  extends Activity {
    private ImageView imageView;
    private Button stopLockButton;
    private String mCurrentPhotoPath;
    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;

    private static final String PREFS_FILE_NAME = "MyPrefsFile";
    private static final String PHOTO_PATH = "Photo Path";
    public static final String LOCK_ACTIVITY_KEY = "lock_activity";
    public static final int FROM_LOCK_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locked);

        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Setup stop lock task button
        stopLockButton = (Button) findViewById(R.id.stop_lock_button);
        stopLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityManager am = (ActivityManager) getSystemService(
                        Context.ACTIVITY_SERVICE);

                if (am.getLockTaskModeState() ==
                        ActivityManager.LOCK_TASK_MODE_LOCKED) {
                    stopLockTask();
                }

                setDefaultCosuPolicies(false);

                Intent intent = new Intent(
                        getApplicationContext(), MainActivity.class);

                intent.putExtra(LOCK_ACTIVITY_KEY,FROM_LOCK_ACTIVITY);
                startActivity(intent);
                finish();
            }
        });

        // set image to View
        setImageToView();

        // Set Default COSU policy
        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mPackageManager = getPackageManager();
        if(mDevicePolicyManager.isDeviceOwnerApp(getPackageName())){
            setDefaultCosuPolicies(true);
        }
        else {
            Toast.makeText(getApplicationContext(),
                    R.string.not_device_owner,Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK
                || keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // start lock task mode if its not already active
        if(mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())){
            ActivityManager am = (ActivityManager) getSystemService(
                    Context.ACTIVITY_SERVICE);
            if(am.getLockTaskModeState() ==
                    ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }
    }

    @Override
    protected void onStop(){
        super.onStop();

        // get editor object and make preference changes to save photo filepath
        SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PHOTO_PATH, mCurrentPhotoPath);
        editor.commit();
    }

    private void setImageToView(){
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        String savedPhotoPath = settings.getString(PHOTO_PATH, null);

        //Initialize the image view and display the picture if one exists
        imageView = (ImageView) findViewById(R.id.lock_imageView);
        Intent intent = getIntent();

        String passedPhotoPath = intent.getStringExtra(
                MainActivity.EXTRA_FILEPATH);
        if (passedPhotoPath != null) {
            mCurrentPhotoPath = passedPhotoPath;
        } else {
            mCurrentPhotoPath = savedPhotoPath;
        }

        if (mCurrentPhotoPath != null) {
            int targetH = imageView.getMaxHeight();
            int targetW = imageView.getMaxWidth();

            // Get the dimensions of the bitmap

            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            int photoH = bmOptions.outHeight;
            int photoW = bmOptions.outWidth;

            // Determine how much to scale down image
            int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;

            Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,
                    bmOptions);
            imageView.setImageBitmap(imageBitmap);
        }
    }

    private void setDefaultCosuPolicies(boolean active){
        /**
         * Specifies if the user is not allowed to reboot the device into safe boot mode.
         * This can only be set by device owners and profile owners on the primary user.
         * The default value is <code>false</code>.
         *
         */
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        /**
         * Specifies if a user is disallowed from factory resetting
         * from Settings. This can only be set by device owners and profile owners on the primary user.
         * The default value is <code>false</code>.
         * <p>This restriction has no effect on secondary users and managed profiles since only the
         * primary user can factory reset the device.
         */
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        /**
         * Specifies if a user is disallowed from adding new users. This can only be set by device
         * owners and profile owners on the primary user.
         * The default value is <code>false</code>.
         * <p>This restriction has no effect on secondary users and managed profiles since only the
         * primary user can add other users.
         */
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        /**
         * Specifies if a user is disallowed from mounting
         * physical external media. This can only be set by device owners and profile owners on the
         * primary user. The default value is <code>false</code>.
         */
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        /**
         * Specifies if a user is disallowed from adjusting the master volume. If set, the master volume
         * will be muted. This can be set by device owners from API 21 and profile owners from API 24.
         * The default value is <code>false</code>.
         */
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);



        /**
         * Called by a device owner or profile owner of secondary users that is affiliated with the
         * device to disable the keyguard altogether.
         * <p>
         * Setting the keyguard to disabled has the same effect as choosing "None" as the screen lock
         * type. However, this call has no effect if a password, pin or pattern is currently set. If a
         * password, pin or pattern is set after the keyguard was disabled, the keyguard stops being
         * disabled.
         *
         */
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);

        /**
         * Called by device owner or profile owner of secondary users  that is affiliated with the
         * device to disable the status bar. Disabling the status bar blocks notifications, quick
         * settings and other screen overlays that allow escaping from a single use device.
         * <p>
         * <strong>Note:</strong> This method has no effect for LockTask mode. The behavior of the
         * status bar in LockTask mode can be configured with
         * {@link #setLockTaskFeatures(ComponentName, int)}. Calls to this method when the device is in
         * LockTask mode will be registered, but will only take effect when the device leaves LockTask
         * mode.
         */
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);

        // enable STAY_ON_WHILE_PLUGGED_IN
        enableStayOnWhilePluggedIn(active);

        // set system update policy
        /**
         * Called by device owners to set a local system update policy. When a new policy is set,
         * {@link #ACTION_SYSTEM_UPDATE_POLICY_CHANGED} is broadcasted.
         * <p>
         * If the supplied system update policy has freeze periods set but the freeze periods do not
         * meet 90-day maximum length or 60-day minimum separation requirement set out in
         * {@link SystemUpdatePolicy#setFreezePeriods},
         * {@link SystemUpdatePolicy.ValidationFailedException} will the thrown. Note that the system
         * keeps a record of freeze periods the device experienced previously, and combines them with
         * the new freeze periods to be set when checking the maximum freeze length and minimum freeze
         * separation constraints. As a result, freeze periods that passed validation during
         * {@link SystemUpdatePolicy#setFreezePeriods} might fail the additional checks here due to
         * the freeze period history. If this is causing issues during development,
         * {@code adb shell dpm clear-freeze-period-record} can be used to clear the record.
         */
        if (active){
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    SystemUpdatePolicy.createWindowedInstallPolicy(60, 120));
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    null);
        }

        /**
         * Sets which packages may enter lock task mode.
         * <p>
         * Any packages that share uid with an allowed package will also be allowed to activate lock
         * task. From {@link android.os.Build.VERSION_CODES#M} removing packages from the lock task
         * package list results in locked tasks belonging to those packages to be finished.
         * <p>
         * This function can only be called by the device owner, a profile owner of an affiliated user
         * or profile, or the profile owner when no device owner is set. See {@link #isAffiliatedUser}.
         * Any package set via this method will be cleared if the user becomes unaffiliated.
         */
        mDevicePolicyManager.setLockTaskPackages(mAdminComponentName,
                active ? new String[]{getPackageName()} : new String[]{});

        /**
         * Called by a profile owner or device owner to set a default activity that the system selects
         * to handle intents that match the given {@link IntentFilter}. This activity will remain the
         * default intent handler even if the set of potential event handlers for the intent filter
         * changes and if the intent preferences are reset.
         * <p>
         * Note that the caller should still declare the activity in the manifest, the API just sets
         * the activity to be the default one to handle the given intent filter.
         * <p>
         * The default disambiguation mechanism takes over if the activity is not installed (anymore).
         * When the activity is (re)installed, it is automatically reset as default intent handler for
         * the filter.
         * <p>
         * The calling device admin must be a profile owner or device owner. If it is not, a security
         * exception will be thrown.
         */

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        if (active) {
            // set Cosu activity as home intent receiver so that it is started
            // on reboot
            mDevicePolicyManager.addPersistentPreferredActivity(
                    mAdminComponentName, intentFilter, new ComponentName(
                            getPackageName(), LockedActivity.class.getName()));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName, getPackageName());
        }

    }

    private void setUserRestriction(String restriction, boolean disallow){
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName,
                    restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName,
                    restriction);
        }
    }

    private void enableStayOnWhilePluggedIn(boolean enabled){
        if (enabled) {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
                            | BatteryManager.BATTERY_PLUGGED_USB
                            | BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    "0"
            );
        }
    }
}
