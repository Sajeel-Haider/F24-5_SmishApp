package com.example.f24_5;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "CallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state == null) {
                return;
            }
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                Log.d(TAG, "Incoming call from: " + incomingNumber);
                // Launch VishingDetectionActivity when a call is incoming
                Intent vishingIntent = new Intent(context, VishingDetectionActivity.class);
                vishingIntent.putExtra("incoming_number", incomingNumber);
                vishingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(vishingIntent);
            }
        }
    }
}
