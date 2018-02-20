package com.gae.scaffolder.plugin;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.Map;

public class FCMPlugin extends CordovaPlugin {
    private static final String TAG = "FCMPlugin";
    
    public static CordovaWebView gWebView;
    public static String notificationCallBack = "FCMPlugin.onNotificationReceived";
    public static Boolean notificationCallBackReady = false;
    public static Map<String, Object> lastPush = null;
     
    public FCMPlugin() {}
    
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        gWebView = webView;
        Log.d(TAG, "==> FCMPlugin initialize");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.createNotificationChannels();
        }
    }
     
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

        Log.d(TAG,"==> FCMPlugin execute: "+ action);
        
        try{
            // READY //
            if (action.equals("ready")) {
                // no-op on Android
                callbackContext.success();
            }
            // GET TOKEN //
            else if (action.equals("getToken")) {
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        try{
                            String token = FirebaseInstanceId.getInstance().getToken();
                            callbackContext.success( FirebaseInstanceId.getInstance().getToken() );
                            Log.d(TAG,"\tToken: "+ token);
                        }catch(Exception e){
                            Log.d(TAG,"\tError retrieving token");
                        }
                    }
                });
            }
            // NOTIFICATION CALLBACK REGISTER //
            else if (action.equals("onNotification")) {
                notificationCallBackReady = true;
                cordova.getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if(lastPush != null) FCMPlugin.sendPushPayload( lastPush );
                        lastPush = null;
                        callbackContext.success();
                    }
                });
            }
            // UN/SUBSCRIBE TOPICS //
            else if (action.equals("subscribeToTopic")) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try{
                            FirebaseMessaging.getInstance().subscribeToTopic( args.getString(0) );
                            callbackContext.success();
                        }catch(Exception e){
                            callbackContext.error(e.getMessage());
                        }
                    }
                });
            }
            else if (action.equals("unsubscribeFromTopic")) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try{
                            FirebaseMessaging.getInstance().unsubscribeFromTopic( args.getString(0) );
                            callbackContext.success();
                        }catch(Exception e){
                            callbackContext.error(e.getMessage());
                        }
                    }
                });
            }
            else{
                callbackContext.error("Method not found");
                return false;
            }
        }catch(Exception e){
            Log.d(TAG, "ERROR: onPluginAction: " + e.getMessage());
            callbackContext.error(e.getMessage());
            return false;
        }
        
        //cordova.getThreadPool().execute(new Runnable() {
        //    public void run() {
        //      //
        //    }
        //});
        
        //cordova.getActivity().runOnUiThread(new Runnable() {
        //    public void run() {
        //      //
        //    }
        //});
        return true;
    }
    
    public static void sendPushPayload(Map<String, Object> payload) {
        Log.d(TAG, "==> FCMPlugin sendPushPayload");
        Log.d(TAG, "\tnotificationCallBackReady: " + notificationCallBackReady);
        Log.d(TAG, "\tgWebView: " + gWebView);
        try {
            JSONObject jo = new JSONObject();
            for (String key : payload.keySet()) {
                jo.put(key, payload.get(key));
                Log.d(TAG, "\tpayload: " + key + " => " + payload.get(key));
            }
            String callBack = "javascript:" + notificationCallBack + "(" + jo.toString() + ")";
            if(notificationCallBackReady && gWebView != null){
                Log.d(TAG, "\tSent PUSH to view: " + callBack);
                gWebView.sendJavascript(callBack);
            }else {
                Log.d(TAG, "\tView not ready. SAVED NOTIFICATION: " + callBack);
                lastPush = payload;
            }
        } catch (Exception e) {
            Log.d(TAG, "\tERROR sendPushToView. SAVED NOTIFICATION: " + e.getMessage());
            lastPush = payload;
        }
    }


    private void createNotificationChannels() {
        // Create channel to show notifications.
        final Context ctx = cordova.getActivity().getApplicationContext();
        final String pkgName = ctx.getPackageName();
        final int default_notification_channel_id = ctx.getResources().getIdentifier(
            "default_notification_channel_id", "string", pkgName);
        final int default_notification_channel_name = ctx.getResources().getIdentifier(
            "default_notification_channel_name", "string", pkgName);
        final String channelId  = ctx.getString(default_notification_channel_id);
        final String channelName = ctx.getString(default_notification_channel_name);
        Log.d(TAG, "Creating notification channel " + channelId + " with name " + channelName);
        NotificationManager notificationManager =
            ctx.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);
    }

} 
