package com.android.plugins;

import android.os.Build;
import android.support.v4.app.ActivityCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by JasonYang on 2016/3/11.
 * Modified by L0laapk3 on 19/9/2017.
 */
public class Permissions extends CordovaPlugin {

    private static final String ACTION_CHECK_PERMISSION = "checkPermission";
    private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
    private static final String ACTION_REQUEST_PERMISSIONS = "requestPermissions";

    private static final int REQUEST_CODE_ENABLE_PERMISSION = 55433;

    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_RESULT_PERMISSION = "hasPermission";
    private static final String KEY_RESULT_AUTODENY = "neverAskAgainFlag";

    private CallbackContext permissionsCallback;

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (ACTION_CHECK_PERMISSION.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    checkPermissionAction(callbackContext, args);
                }
            });
            return true;
        } else if (ACTION_REQUEST_PERMISSION.equals(action) || ACTION_REQUEST_PERMISSIONS.equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        requestPermissionAction(callbackContext, args);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JSONObject returnObj = new JSONObject();
                        addProperty(returnObj, KEY_ERROR, ACTION_REQUEST_PERMISSION);
                        addProperty(returnObj, KEY_MESSAGE, "Request permission has been denied.");
                        callbackContext.error(returnObj);
                        permissionsCallback = null;
                    }
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (permissionsCallback == null) {
            return;
        }

        JSONObject returnObj = new JSONObject();
        if (permissions != null && permissions.length > 0) {
            //Call checkPermission again to verify
            boolean hasAllPermissions = hasAllPermissions(permissions);
            addProperty(returnObj, KEY_RESULT_PERMISSION, hasAllPermissions);
            if (hasAllPermissions)
                addProperty(returnObj, KEY_RESULT_AUTODENY, false);
            else
                addProperty(returnObj, KEY_RESULT_AUTODENY, hasAnyNeverAskAgainFlag(permissions));
            permissionsCallback.success(returnObj);
        } else {
            addProperty(returnObj, KEY_ERROR, ACTION_REQUEST_PERMISSION);
            addProperty(returnObj, KEY_MESSAGE, "Unknown error.");
            permissionsCallback.error(returnObj);
        }
        permissionsCallback = null;
    }

    private void checkPermissionAction(CallbackContext callbackContext, JSONArray permission) {
        if (permission == null || permission.length() == 0 || permission.length() > 1) {
            JSONObject returnObj = new JSONObject();
            addProperty(returnObj, KEY_ERROR, ACTION_CHECK_PERMISSION);
            addProperty(returnObj, KEY_MESSAGE, "One time one permission only.");
            callbackContext.error(returnObj);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            JSONObject returnObj = new JSONObject();
            addProperty(returnObj, KEY_RESULT_PERMISSION, true);
            addProperty(returnObj, KEY_RESULT_AUTODENY, false);
            callbackContext.success(returnObj);
        } else {
            try {
                JSONObject returnObj = new JSONObject();
                boolean hasPermission = cordova.hasPermission(permission.getString(0));
                addProperty(returnObj, KEY_RESULT_PERMISSION, hasPermission);
                callbackContext.success(returnObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestPermissionAction(CallbackContext callbackContext, JSONArray permissions) throws Exception {
        if (permissions == null || permissions.length() == 0) {
            JSONObject returnObj = new JSONObject();
            addProperty(returnObj, KEY_ERROR, ACTION_REQUEST_PERMISSION);
            addProperty(returnObj, KEY_MESSAGE, "At least one permission.");
            callbackContext.error(returnObj);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            JSONObject returnObj = new JSONObject();
            addProperty(returnObj, KEY_RESULT_PERMISSION, true);
            addProperty(returnObj, KEY_RESULT_AUTODENY, false);
            callbackContext.success(returnObj);
        } else if (hasAllPermissions(permissions)) {
            JSONObject returnObj = new JSONObject();
            addProperty(returnObj, KEY_RESULT_PERMISSION, true);
            addProperty(returnObj, KEY_RESULT_AUTODENY, false);
            callbackContext.success(returnObj);
        } else {
            permissionsCallback = callbackContext;
            String[] permissionArray = getPermissions(permissions);
            cordova.requestPermissions(this, REQUEST_CODE_ENABLE_PERMISSION, permissionArray);
        }
    }

    private String[] getPermissions(JSONArray permissions) {
        String[] stringArray = new String[permissions.length()];
        for (int i = 0; i < permissions.length(); i++) {
            try {
                stringArray[i] = permissions.getString(i);
            } catch (JSONException ignored) {
                //Believe exception only occurs when adding duplicate keys, so just ignore it
            }
        }
        return stringArray;
    }

    private boolean hasAllPermissions(JSONArray permissions) throws JSONException {
        return hasAllPermissions(getPermissions(permissions));
    }

    private boolean hasAllPermissions(String[] permissions) throws JSONException {

        for (String permission : permissions) {
            if(!cordova.hasPermission(permission)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasAnyNeverAskAgainFlag(String[] permissions) {

        for (String permission : permissions) {
            if(GetNeverAskAgainFlag(permission)) {
                return true;
            }
        }

        return false;
    }

    //only call on permissions that are denied!
    private boolean GetNeverAskAgainFlag(String permission) {
        try {
            if (cordova.hasPermission(permission)) return false;
            return Build.VERSION.SDK_INT >= 23 && !ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(), permission);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void addProperty(JSONObject obj, String key, Object value) {
        try {
            if (value == null) {
                obj.put(key, JSONObject.NULL);
            } else {
                obj.put(key, value);
            }
        } catch (JSONException ignored) {
            //Believe exception only occurs when adding duplicate keys, so just ignore it
        }
    }
}
