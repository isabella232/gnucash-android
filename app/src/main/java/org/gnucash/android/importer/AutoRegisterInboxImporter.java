package org.gnucash.android.importer;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Telephony;
import android.util.Log;

import org.gnucash.android.db.adapter.AutoRegisterInboxDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.util.AutoRegisterUtil;
import org.gnucash.android.util.CursorThrowWrapper;
import org.gnucash.android.util.TimestampHelper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports SMS inbox messages onto the GnuCash database.
 * Importing is necessary because it is impossible to join SMS ContentProvider with GnuCash database
 * to filter only specific messages.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com>
 */

public class AutoRegisterInboxImporter extends AsyncTask<AutoRegister.Provider, Integer, Long> {
    private static final String LOG_TAG = AutoRegisterInboxImporter.class.getSimpleName();

    private static final int IMPORT_BATCH_SIZE = 100;

    /**
     * Context
     */
    private Context mContext;

    /**
     * SMS inbox cursor
     */
    private Cursor mCursor;

    /**
     * Inbox database adapter
     */
    private AutoRegisterInboxDbAdapter mInboxDbAdapter;

    public AutoRegisterInboxImporter(Context context, AutoRegisterInboxDbAdapter inboxDbAdapter) {
        mContext = context;
        mInboxDbAdapter = inboxDbAdapter;
    }

    @Override
    protected Long doInBackground(AutoRegister.Provider... providers) {
        long totalSize = 0;
        for (AutoRegister.Provider provider : providers) {
            Cursor cursor = getInboxCursor(provider);
            totalSize += importToInbox(provider, new CursorThrowWrapper(cursor));
        }
        return totalSize;
    }

    private Cursor getInboxCursor(AutoRegister.Provider provider) {
        String selection = new StringBuilder()
                .append(Telephony.Sms.Inbox.ADDRESS).append(" IN (?, ?)")
                .toString();

        String[] selectionArgs = new String[] {
                provider.getPhone(),
                AutoRegisterUtil.stripCountryCodeFromPhoneNumber(provider.getPhone())
        };

        return mContext.getContentResolver().query(
                Telephony.Sms.Inbox.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                Telephony.Sms.Inbox.DATE + " ASC"
        );
    }

    private long importToInbox(AutoRegister.Provider provider, CursorThrowWrapper cursor) {
        List<AutoRegister.Inbox> buf = new ArrayList<>(IMPORT_BATCH_SIZE);
        int totalSize = cursor.getCount();

        Log.v(LOG_TAG, "totalSize = " + totalSize);
        Log.v(LOG_TAG, "loop = " + Math.ceil((float) totalSize / IMPORT_BATCH_SIZE));
        for (int i = 0; i < Math.ceil((float) totalSize / IMPORT_BATCH_SIZE); i++) {
            int nextBatchSize = Math.min(IMPORT_BATCH_SIZE, totalSize - i * IMPORT_BATCH_SIZE);

            for (int j = 0; j < nextBatchSize; j++) {
                cursor.moveToNext();

                AutoRegister.Message message = new AutoRegister.Message(
                        provider,
                        AutoRegister.Message.Type.SMS,
                        cursor.getLong(Telephony.Sms.Inbox._ID),
                        new Timestamp(cursor.getLong(Telephony.Sms.Inbox.DATE)),
                        cursor.getString(Telephony.Sms.Inbox.ADDRESS),
                        cursor.getString(Telephony.Sms.Inbox.BODY));

                buf.add(j, new AutoRegister.Inbox(message));
            }

            long result = mInboxDbAdapter.bulkAddRecords(buf, DatabaseAdapter.UpdateMethod.replace);
            Log.v(LOG_TAG, String.format("i = %d, nextBatchSize = %d, result = %d",
                    i, nextBatchSize, result));
            buf.clear();
        }

        return totalSize;
    }
}
