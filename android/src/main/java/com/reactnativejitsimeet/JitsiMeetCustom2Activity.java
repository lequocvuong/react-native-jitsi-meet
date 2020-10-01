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
import androidx.fragment.app.FragmentActivity;

import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.PermissionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetUserInfo;
import org.jitsi.meet.sdk.JitsiMeetView;
import org.jitsi.meet.sdk.JitsiMeetViewListener;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;


public class JitsiMeetCustom2Activity extends FragmentActivity implements JitsiMeetActivityInterface, JitsiMeetViewListener {

    private static final String TAG = "JitsiMeetCustom2";


    private JitsiMeetView view;

    private String serverURL;
    private String audioMuted;
    private String videoMuted;
    private RNJitsiMeetUserInfo rnUserInfo;
    private JitsiMeetUserInfo userInfo;

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        JitsiMeetActivityDelegate.onActivityResult(
                this, requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        JitsiMeetActivityDelegate.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        serverURL = getIntent().getExtras().getString("SERVER_URL", "");
        audioMuted = getIntent().getExtras().getString("AUDIO_MUTED", "");
        videoMuted = getIntent().getExtras().getString("VIDEO_MUTED", "");
        rnUserInfo = ((RNJitsiMeetUserInfo) getIntent().getSerializableExtra("USER_INFO"));

        userInfo = new JitsiMeetUserInfo();
        userInfo.setEmail(rnUserInfo.getEmail());
        userInfo.setDisplayName(rnUserInfo.getDisplayName());
        userInfo.setAvatar(rnUserInfo.getAvatar());

        view = new JitsiMeetView(this);
        JitsiMeetConferenceOptions options = getOptions();
        view.join(options);

        setContentView(view);
        view.setListener(this);

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        view.dispose();
        view = null;

        JitsiMeetActivityDelegate.onHostDestroy(this);

        EventBus.getDefault().unregister(this);

    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        JitsiMeetActivityDelegate.onNewIntent(intent);
    }

    @Override
    public void onRequestPermissionsResult(
            final int requestCode,
            final String[] permissions,
            final int[] grantResults) {
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();

        JitsiMeetActivityDelegate.onHostResume(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        JitsiMeetActivityDelegate.onHostPause(this);
    }

    @Override
    public void requestPermissions(String[] strings, int i, PermissionListener permissionListener) {

    }

    protected JitsiMeetConferenceOptions getOptions(){


        return new JitsiMeetConferenceOptions.Builder()
                .setWelcomePageEnabled(false)
                .setServerURL(buildURL("https://mobimeeting.mobifone.vn"))
                .setAudioOnly(false)
                .setAudioMuted(audioMuted.equals("1"))
                .setVideoMuted(videoMuted.equals("1"))
                .setRoom(serverURL)
                .setUserInfo(userInfo)
                .setFeatureFlag("call-integration.enabled", false)
                .setFeatureFlag("resolution", 360)
                .build();

    }

    @Override
    public void onConferenceJoined(Map<String, Object> map) {
        WritableNativeMap params = new WritableNativeMap();
        params.putString("data", map.toString());
        RNJitsiMeetModule.sendEvent("onConferenceJoined", params);
    }

    @Override
    public void onConferenceTerminated(Map<String, Object> map) {
        Log.d(TAG,"TERMINATED");
        WritableNativeMap params = new WritableNativeMap();
        params.putString("data", map.toString());
        RNJitsiMeetModule.sendEvent("onConferenceTerminated", params);

        finish();
    }

    @Override
    public void onConferenceWillJoin(Map<String, Object> map) {

    }

    private @Nullable URL buildURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(String command) {
        Log.d(TAG, "eventBus");

        this.view.leave();
    }
}