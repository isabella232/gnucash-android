/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gnucash.android.db.adapter;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema.AutoRegisterInboxEntry;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.model.Money;
import org.gnucash.android.util.CursorThrowWrapper;
import org.gnucash.android.util.TimestampHelper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Database adapter for creating/modifying Auto-register Inbox entries
 */
public class AutoRegisterInboxDbAdapter extends DatabaseAdapter<AutoRegister.Inbox> {

    /**
     * Provider database adapter
     */
    private AutoRegisterProviderDbAdapter mProviderDbAdapter;

    /**
     * Keyword database adapter
     */
    private AutoRegisterKeywordDbAdapter mKeywordDbAdapter;

    /**
     * Return the application instance of the books database adapter
     * @return Books database adapter
     */
    public static AutoRegisterInboxDbAdapter getInstance(){
        return GnuCashApplication.getAutoRegisterInboxDbAdapter();
    }

    /**
     * Opens the database adapter with an existing database
     * @param db        SQLiteDatabase object
     */
    public AutoRegisterInboxDbAdapter(SQLiteDatabase db,
                                      AutoRegisterProviderDbAdapter providerDbAdapter,
                                      AutoRegisterKeywordDbAdapter keywordDbAdapter) {
        super(db, AutoRegisterInboxEntry.TABLE_NAME, new String[] {
                AutoRegisterInboxEntry.COLUMN_MESSAGE_TYPE,
                AutoRegisterInboxEntry.COLUMN_MESSAGE_ID,
                AutoRegisterInboxEntry.COLUMN_MESSAGE_TIMESTAMP,
                AutoRegisterInboxEntry.COLUMN_MESSAGE_ADDRESS,
                AutoRegisterInboxEntry.COLUMN_MESSAGE_BODY,
                AutoRegisterInboxEntry.COLUMN_PROVIDER_UID,
                AutoRegisterInboxEntry.COLUMN_IS_PARSED,
                AutoRegisterInboxEntry.COLUMN_CURRENCY,
                AutoRegisterInboxEntry.COLUMN_VALUE_NUM,
                AutoRegisterInboxEntry.COLUMN_VALUE_DENOM,
                AutoRegisterInboxEntry.COLUMN_MEMO,
                AutoRegisterInboxEntry.COLUMN_IS_COMPLETED,
                AutoRegisterInboxEntry.COLUMN_TRANSACTION_UID,
        });

        mProviderDbAdapter = providerDbAdapter;
        mKeywordDbAdapter = keywordDbAdapter;
    }

    @Override
    public AutoRegister.Inbox buildModelInstance(@NonNull Cursor cursor) {
        CursorThrowWrapper wrapper = new CursorThrowWrapper(cursor);

        String messageType = wrapper.getString(AutoRegisterInboxEntry.COLUMN_MESSAGE_TYPE);
        long messageID = wrapper.getLong(AutoRegisterInboxEntry.COLUMN_MESSAGE_ID);
        Timestamp messageTimestamp = wrapper.getTimestamp(AutoRegisterInboxEntry.COLUMN_MESSAGE_TIMESTAMP);
        String messageAddress= wrapper.getString(AutoRegisterInboxEntry.COLUMN_MESSAGE_ADDRESS);
        String messageBody = wrapper.getString(AutoRegisterInboxEntry.COLUMN_MESSAGE_BODY);
        String providerUID = wrapper.getString(AutoRegisterInboxEntry.COLUMN_PROVIDER_UID);
        boolean parsed = wrapper.getBoolean(AutoRegisterInboxEntry.COLUMN_IS_PARSED);

        AutoRegister.Message message = new AutoRegister.Message(
                mProviderDbAdapter.findActiveProviderByUID(providerUID),
                AutoRegister.Message.Type.valueOf(messageType),
                messageID, messageTimestamp, messageAddress, messageBody);

        String currency = wrapper.getString(AutoRegisterInboxEntry.COLUMN_CURRENCY);
        long valueNum = wrapper.getLong(AutoRegisterInboxEntry.COLUMN_VALUE_NUM);
        long valueDenom = wrapper.getLong(AutoRegisterInboxEntry.COLUMN_VALUE_DENOM);
        String memo = wrapper.getString(AutoRegisterInboxEntry.COLUMN_MEMO);

        AutoRegister.Inbox inbox = parsed ?
                new AutoRegister.Inbox(message, new Money(valueNum, valueDenom, currency), memo) :
                new AutoRegister.Inbox(message, null, null);
        if (memo != null) {
            inbox.setKeyword(mKeywordDbAdapter.findFirstMatchingKeyword(memo));
        }

        boolean completed = wrapper.getBoolean(AutoRegisterInboxEntry.COLUMN_IS_COMPLETED);
        String transactionUID = wrapper.getString(AutoRegisterInboxEntry.COLUMN_TRANSACTION_UID);
        inbox.setCompleted(completed);
        if (transactionUID != null) inbox.setTransactionUID(transactionUID);

        populateBaseModelAttributes(cursor, inbox);
        return inbox;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final AutoRegister.Inbox inbox) {
        stmt.clearBindings();

        AutoRegister.Message message = inbox.getMessage();
        stmt.bindString(1, message.getType().name());
        stmt.bindLong(2, message.getID());
        stmt.bindLong(3, message.getTimestamp().getTime());
        stmt.bindString(4, message.getAddress());
        stmt.bindString(5, message.getBody());
        stmt.bindString(6, message.getProvider().getUID());
        stmt.bindLong(7, message.isParsed() ? 1 : 0);

        if (message.isParsed()) {
            Money value = inbox.getValue();
            stmt.bindString(8, value.getCommodity().getCurrencyCode());
            stmt.bindLong(9, value.getNumerator());
            stmt.bindLong(10, value.getDenominator());
            stmt.bindString(11, inbox.getMemo());
        }

        stmt.bindLong(12, inbox.isCompleted() ? 1 : 0);
        if (inbox.getTransactionUID() != null) {
            stmt.bindString(13, inbox.getTransactionUID());
        }

        stmt.bindString(14, inbox.getUID());
        return stmt;
    }

    public Cursor fetchRecords(AutoRegister.Provider provider, AutoRegister.Keyword keyword) {
        StringBuilder selection = new StringBuilder();
        List<String> selectionBuf = new ArrayList<>();
        List<String> selectionArgBuf = new ArrayList<>();

        // filter by selected provider
        if (provider != null) {
            selection.append(AutoRegisterInboxEntry.COLUMN_PROVIDER_UID).append(" = ?");
/*
            for (String glob : provider.getGlobs()) {
                selectionBuf.add(Telephony.Sms.Inbox.BODY + " GLOB ?");
                selectionArgBuf.add('*' + glob + '*');
            }
            selection.append(" AND (")
                    .append(TextUtils.join(" OR ", selectionBuf))
                    .append(")");
*/
            selectionArgBuf.add(provider.getUID());
        }

        // filter by selected keyword
        if (keyword != null) {
            selection.append(" AND ").append(AutoRegisterInboxEntry.COLUMN_MESSAGE_BODY)
                    .append(" LIKE ?");
            selectionArgBuf.add("%" + keyword.getKeyword() + "%");
        }

        Log.d(LOG_TAG, "selection = " + selection);
        Log.d(LOG_TAG, "args = " + selectionArgBuf);

        return fetchAllRecords(
                selection.toString(),
                selectionArgBuf.toArray(new String[] {}),
                AutoRegisterInboxEntry.COLUMN_MESSAGE_TIMESTAMP + " DESC"
        );
    }
}
