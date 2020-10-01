package com.reactnativejitsimeet;

import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.WritableNativeMap;

import org.jitsi.meet.sdk.JitsiMeet;

import java.util.Date;

public class CallReceiver extends PhonecallReceiver {

    private static final String TAG = "CallReceiver";

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start)
    {
        //
        Log.d(TAG, "onIncomingCallReceived");


    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start)
    {
        //
        Log.d(TAG, "onIncomingCallAnswered");
        WritableNativeMap params = new WritableNativeMap();
        params.putString("number", number + " " + start.toString());

        RNJitsiMeetModule.sendEvent("onIncomingCallAnswered", params);

//        JitsiMeetCustom2Activity activity = (JitsiMeetCustom2Activity) ctx;
//        activity.getView().leave();

    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
        //
        Log.d(TAG, "onIncomingCallEnded");
        WritableNativeMap params = new WritableNativeMap();
        params.putString("number", number + " " + start.toString() + " " + end.toString());

        RNJitsiMeetModule.sendEvent("onIncomingCallEnded", params);


    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
        //
        Log.d(TAG, "onOutgoingCallStarted");
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
        //
        Log.d(TAG, "onOutgoingCallEnded");
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start)
    {
        //
        Log.d(TAG, "onMissedCall");
    }


}
