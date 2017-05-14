package org.gnucash.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import org.gnucash.android.model.Account;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Money;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;
import org.gnucash.android.util.AmountParser;

import java.math.BigDecimal;
import java.text.ParseException;

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

        SmsMessage[] sms = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        for (SmsMessage s : sms) {
            Account a = findAccount(s);
            if (a == null) {
                continue;
            }

        }
    }

    private Account findAccount(SmsMessage sms) {
        String originator = sms.getOriginatingAddress();
        Log.i(TAG, "originator = " + originator);
        return null;
    }

    private Transaction extractTransaction(SmsMessage sms, Account account) {
        String title = "더미";
        Transaction t = new Transaction(title);

        try {
            String amount_str = "23,000";
            BigDecimal amount = AmountParser.parse(amount_str);
            Money money = new Money(amount, Commodity.DEFAULT_COMMODITY);

            Split s = new Split(money, account.getUID());
            t.addSplit(s);
            t.addSplit(s.createPair(account.getUID()));

        } catch (ParseException e) {

        }
        return t;
    }
}
