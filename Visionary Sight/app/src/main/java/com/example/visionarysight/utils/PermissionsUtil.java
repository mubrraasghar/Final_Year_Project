package com.example.visionarysight.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionsUtil {

    public static final int PERMISSION_REQUEST_CODE = 100;

    public static final String[] PERMISSIONS = {
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.ACCESS_NETWORK_STATE
    };

    public static boolean checkPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity activity, String permission) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, PERMISSION_REQUEST_CODE);
    }

    public static boolean hasAllPermissions(Context context) {
        for (String permission : PERMISSIONS) {
            if (!checkPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    public static void requestAllPermissions(Activity activity) {
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : PERMISSIONS) {
            if (!checkPermission(activity, permission)) {
                permissionsNeeded.add(permission);
            }
        }
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    public static boolean allPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
