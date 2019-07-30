package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private Button takePicButton;
    private Button lockTaskButton;
    private Button enableAdmin;
    private ImageView imageView;
    private String mCurrentPhotoPath;
    private int permissionCheck;
    private PackageManager mPackageManager;
    private boolean toggle = false;

    private DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private static final String FILE_TAG = "File Creation";
    private static final int REQUEST_CODE_ENABLE_ADMIN = 3;

    public static final String EXTRA_FILEPATH =
            "com.example.myapplication.EXTRA_FILEPATH";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePicButton = (Button) findViewById(R.id.pic_button);
        takePicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDevicePolicyManager.clearDeviceOwnerApp(getApplicationContext().getPackageName());
                /*
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    File photoFile=null;
                    try{
                        photoFile = createImageFile();
                    } catch (IOException e) {
                        Log.e(FILE_TAG,e.getMessage());
                    }
                    if (photoFile != null) {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                else{
                    Toast.makeText(
                            getApplicationContext(),R.string.no_camera_apps,
                            Toast.LENGTH_SHORT)
                            .show();
                }
                */
            }
        });

        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);

        mPackageManager = this.getPackageManager();

        lockTaskButton = (Button) findViewById(R.id.start_lock_button);
        lockTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if ( mDevicePolicyManager.isDeviceOwnerApp(
                        getApplicationContext().getPackageName())) {
                    Intent lockIntent = new Intent(getApplicationContext(),
                            LockedActivity.class);
                    lockIntent.putExtra(EXTRA_FILEPATH,mCurrentPhotoPath);

                    mPackageManager.setComponentEnabledSetting(
                            new ComponentName(getApplicationContext(),
                                    LockedActivity.class),
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            PackageManager.DONT_KILL_APP);
                    startActivity(lockIntent);
                    finish();
                } else {
                    Toast.makeText(getApplicationContext(),
                            R.string.not_lock_whitelisted,Toast.LENGTH_SHORT)
                            .show();
                }

            }
        });

        enableAdmin = (Button) findViewById(R.id.enable_admin);
        enableAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /**
                 * Clears the current device owner. The caller must be the device owner. This function should be
                 * used cautiously as once it is called it cannot be undone. The device owner can only be set as
                 * a part of device setup, before it completes.
                 * <p>
                 * While some policies previously set by the device owner will be cleared by this method, it is
                 * a best-effort process and some other policies will still remain in place after the device
                 * owner is cleared.
                 */
                //mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());

                if(!mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) return;

                if(!toggle) {
                    mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, true);
                    toggle = true;
                } else {
                    mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, false);
                    toggle = false;
                }
                /*
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminComponentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "HELLO WORLD!");
                startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);
                */

            }
        });

        imageView = (ImageView) findViewById(R.id.main_imageView);

        // Check to see if permission to access external storage is granted,
        // and request if not

        permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String []{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }

        // Check to see if started by LockActivity and disable LockActivity if so

        Intent intent = getIntent();

        if(intent.getIntExtra(LockedActivity.LOCK_ACTIVITY_KEY,0) ==
                LockedActivity.FROM_LOCK_ACTIVITY){
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName,getPackageName());
            mPackageManager.setComponentEnabledSetting(
                    new ComponentName(getApplicationContext(), LockedActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
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

    private File createImageFile() throws IOException {

        //Check for storage permission
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // Create an image file name
            String timeStamp =
                    new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getApplicationContext().getExternalFilesDir(
                    null);
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = image.getAbsolutePath();
            return image;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            setImageToView();
        }
        if(requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            System.out.println("HAHA");
            System.out.println(resultCode == RESULT_OK);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, results array is empty
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionCheck=grantResults[0];
                } else {
                    takePicButton.setEnabled(false);
                }
                return;
            }
        }
    }

    private void setImageToView(){
        //Save the file in gallery

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);

        // Get the dimensions of the view

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

        Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        imageView.setImageBitmap(imageBitmap);

        // enable lock task button
        lockTaskButton.setEnabled(true);
    }
}
