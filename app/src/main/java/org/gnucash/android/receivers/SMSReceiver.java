package org.gnucash.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

import org.gnucash.android.service.AutoRegisterService;

/**
 * Created by hkjinlee on 2017. 5. 9..
 */

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = SMSReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            return;
        }

        AutoRegisterService.startActionReceiveSMS(context, intent);
    }
}
