package com.reactnativejitsimeet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.Nullable;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.jitsi.meet.sdk.JitsiMeetView;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;


public class JitsiMeetCustomActivity extends JitsiMeetActivity {
    /**
     * The request code identifying requests for the permission to draw on top
     * of other apps. The value must be 16-bit and is arbitrarily chosen here.
     */
    private static final int OVERLAY_PERMISSION_REQUEST_CODE
            = (int) (Math.random() * Short.MAX_VALUE);

    /**
     * ServerURL configuration key for restriction configuration using {@link android.content.RestrictionsManager}
     */
    public static final String RESTRICTION_SERVER_URL = "SERVER_URL";

    /**
     * Broadcast receiver for restrictions handling
     */
    private BroadcastReceiver broadcastReceiver;

    /**
     * Flag if configuration is provided by RestrictionManager
     */
    private boolean configurationByRestrictions = false;

    /**
     * Default URL as could be obtained from RestrictionManager
     */
    private String defaultURL;


    // JitsiMeetActivity overrides
    //
    private String serverURL;
    private String audioMuted;
    private String videoMuted;
    private RNJitsiMeetUserInfo rnUserInfo;
    private JitsiMeetUserInfo userInfo;


    @Override
    protected boolean extraInitialize() {
        Log.d(this.getClass().getSimpleName(), "LIBRE_BUILD=BuildConfig.LIBRE_BUILD");

        // Setup Crashlytics and Firebase Dynamic Links
        // Here we are using reflection since it may have been disabled at compile time.
        try {
            Class<?> cls = Class.forName("org.jitsi.meet.GoogleServicesHelper");
            Method m = cls.getMethod("initialize", JitsiMeetActivity.class);
            m.invoke(null, this);
        } catch (Exception e) {
            // Ignore any error, the module is not compiled when LIBRE_BUILD is enabled.
        }

        // In Debug builds React needs permission to write over other apps in
        // order to display the warning and error overlays.
        if (BuildConfig.DEBUG) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent
                        = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));

                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);

                return true;
            }
        }

        return false;
    }

    @Override
    protected void initialize() {
        serverURL = getIntent().getExtras().getString("SERVER_URL", "");
        audioMuted = getIntent().getExtras().getString("AUDIO_MUTED", "");
        videoMuted = getIntent().getExtras().getString("VIDEO_MUTED", "");
        rnUserInfo = ((RNJitsiMeetUserInfo) getIntent().getSerializableExtra("USER_INFO"));

        userInfo = new JitsiMeetUserInfo();
        userInfo.setEmail(rnUserInfo.getEmail());
        userInfo.setDisplayName(rnUserInfo.getDisplayName());
        userInfo.setAvatar(rnUserInfo.getAvatar());

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // As new restrictions including server URL are received,
                // conference should be restarted with new configuration.
                leave();
                recreate();
            }
        };
        registerReceiver(broadcastReceiver,
                new IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED));

        resolveRestrictions();
        setJitsiMeetConferenceDefaultOptions();
        super.initialize();
    }

    @Override
    public void onDestroy() {
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

        super.onDestroy();
    }

    private void setJitsiMeetConferenceDefaultOptions() {
        // Set default options
        JitsiMeetConferenceOptions defaultOptions
                = new JitsiMeetConferenceOptions.Builder()
                .setWelcomePageEnabled(false)
                .setServerURL(buildURL(defaultURL))
                .setAudioOnly(false)
                .setAudioMuted(audioMuted.equals("1"))
                .setVideoMuted(videoMuted.equals("1"))
                .setRoom(serverURL)
                .setUserInfo(userInfo)
                .setFeatureFlag("call-integration.enabled", false)
                .setFeatureFlag("resolution", 360)
                .setFeatureFlag("server-url-change.enabled", !configurationByRestrictions)
                .build();

        JitsiMeetActivity.launch(this, defaultOptions);
        this.finish();

    }

    private void resolveRestrictions() {
        RestrictionsManager manager =
                (RestrictionsManager) getSystemService(Context.RESTRICTIONS_SERVICE);
        Bundle restrictions = manager.getApplicationRestrictions();
        Collection<RestrictionEntry> entries = manager.getManifestRestrictions(
                getApplicationContext().getPackageName());
        for (RestrictionEntry restrictionEntry : entries) {
            String key = restrictionEntry.getKey();
            if (RESTRICTION_SERVER_URL.equals(key)) {
                // If restrictions are passed to the application.
                if (restrictions != null &&
                        restrictions.containsKey(RESTRICTION_SERVER_URL)) {
                    defaultURL = restrictions.getString(RESTRICTION_SERVER_URL);
                    configurationByRestrictions = true;
                    // Otherwise use default URL from app-restrictions.xml.
                } else {
                    defaultURL = restrictionEntry.getSelectedString();
                    configurationByRestrictions = false;
                }
            }
        }
    }

    @Override
    public void onConferenceTerminated(Map<String, Object> data) {
        Log.d(TAG, "Conference terminated: " + data);
    }

    // Activity lifecycle method overrides
    //

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                initialize();
                return;
            }

            throw new RuntimeException("Overlay permission is required when running in Debug mode.");
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // ReactAndroid/src/main/java/com/facebook/react/ReactActivity.java
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (BuildConfig.DEBUG && keyCode == KeyEvent.KEYCODE_MENU) {
            JitsiMeet.showDevOptions();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);

        Log.d(TAG, "Is in picture-in-picture mode: " + isInPictureInPictureMode);

        if (!isInPictureInPictureMode) {
            this.startActivity(new Intent(this, getClass())
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        }
    }

    // Helper methods
    //

    private @Nullable URL buildURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public void onConferenceJoined(Map<String, Object> data) {
        Log.d(TAG, "Conference joined: " + data);
    }


}