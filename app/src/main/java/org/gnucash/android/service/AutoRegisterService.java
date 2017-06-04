package org.gnucash.android.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import org.gnucash.android.db.adapter.AutoRegisterKeywordDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.model.AutoRegister;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class AutoRegisterService extends IntentService {
    private AutoRegisterProviderDbAdapter mProviderDbAdapter;
    private AutoRegisterKeywordDbAdapter mMappingDbAdapter;

    private static final String ACTION_RECEIVE_SMS = "org.gnucash.android.service.action.NEW_SMS";
    private static final String ACTION_BUILD_VENDOR_SUGGESTION = "org.gnucash.android.service.action.BUILD_VENDOR_SUGGESTION";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "org.gnucash.android.service.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "org.gnucash.android.service.extra.PARAM2";

    public AutoRegisterService() {
        super("AutoRegisterService");

        mProviderDbAdapter = AutoRegisterProviderDbAdapter.getInstance();
        mMappingDbAdapter = AutoRegisterKeywordDbAdapter.getInstance();
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionReceiveSMS(Context context, Intent intent) {
        Intent i = new Intent(context, AutoRegisterService.class);
        i.setAction(ACTION_RECEIVE_SMS);
        i.putExtras(intent);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBuildSuggestion(Context context, String param1, String param2) {
        Intent intent = new Intent(context, AutoRegisterService.class);
        intent.setAction(ACTION_BUILD_VENDOR_SUGGESTION);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_BUILD_VENDOR_SUGGESTION.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionBuildVendorSuggestion(param1, param2);
            } else if (ACTION_RECEIVE_SMS.equals(action)) {
                SmsMessage[] smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
                handleActionReceiveSMS(smsMessages);
            }
        }
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionReceiveSMS(SmsMessage[] smsMessages) {
        for (SmsMessage sms : smsMessages) {
            String address = sms.getOriginatingAddress();
            String body = sms.getMessageBody();

            AutoRegister.Provider provider = mProviderDbAdapter.findActiveProviderByPhone(address);

        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBuildVendorSuggestion(String param1, String param2) {
    }

}
