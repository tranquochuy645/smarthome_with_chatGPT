package com.example.alexucana;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

/**
 * The PermissionUtils class contains static methods to handle runtime permissions in Android applications.
 */
public class PermissionUtils {

    /**
     * Constant representing the request code for requesting all permissions.
     */
    public static final int REQUEST_CODE_ALL_PERMISSIONS = 100;

    /**
     * Requests all permissions defined in the application's manifest.
     *
     * @param activity The activity in which permissions should be requested.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestAllPermissions(Activity activity) {
        // Retrieve all permissions defined in the manifest
        String[] permissions = getAllPermissions(activity);

        // If there are permissions to request, initiate the permission request
        if (permissions.length > 0) {
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE_ALL_PERMISSIONS);
        }
    }

    /**
     * Retrieves all the permissions declared in the application's manifest.
     *
     * @param context The context of the application.
     * @return An array of permission strings or an empty array if no permissions are declared.
     */
    public static String[] getAllPermissions(Context context) {
        try {
            // Get package information including requested permissions
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_PERMISSIONS);
            return packageInfo.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            // Handle exception if package information is not found
            e.printStackTrace();
            return new String[0];
        }
    }

    /**
     * Checks if all the requested permissions have been granted.
     *
     * @param grantResults The results of the permission request.
     * @return True if all permissions are granted, false otherwise.
     */
    public static boolean checkAllPermissionsGranted(int[] grantResults) {
        // Check each result in grantResults array
        for (int result : grantResults) {
            // If any permission is not granted, return false
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        // All permissions are granted
        return true;
    }
}
