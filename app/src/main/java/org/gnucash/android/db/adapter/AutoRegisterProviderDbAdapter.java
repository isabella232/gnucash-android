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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.code.regexp.Pattern;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema.AutoRegisterProviderEntry;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.util.CursorThrowWrapper;
import org.gnucash.android.util.TimestampHelper;

import java.util.List;

/**
 * Database adapter for creating/modifying Auto-register provider entries
 */
public class AutoRegisterProviderDbAdapter extends DatabaseAdapter<AutoRegisterProvider> {

    /**
     * Opens the database adapter with an existing database
     * @param db        SQLiteDatabase object
     */
    public AutoRegisterProviderDbAdapter(SQLiteDatabase db) {
        super(db, AutoRegisterProviderEntry.TABLE_NAME, new String[] {
                AutoRegisterProviderEntry.COLUMN_NAME,
                AutoRegisterProviderEntry.COLUMN_VERSION,
                AutoRegisterProviderEntry.COLUMN_PHONE,
                AutoRegisterProviderEntry.COLUMN_PATTERN,
                AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID,
                AutoRegisterProviderEntry.COLUMN_ENABLED,
                AutoRegisterProviderEntry.COLUMN_LAST_SYNC
        });
    }

    /**
     * Return the application instance of the books database adapter
     * @return Books database adapter
     */
    public static AutoRegisterProviderDbAdapter getInstance(){
        return GnuCashApplication.getAutoRegisterProviderDbAdapter();
    }

    @Override
    public AutoRegisterProvider buildModelInstance(@NonNull Cursor cursor) {
        CursorThrowWrapper wrapper = new CursorThrowWrapper(cursor);

        String name =  wrapper.getString(AutoRegisterProviderEntry.COLUMN_NAME);
        String version = wrapper.getString(AutoRegisterProviderEntry.COLUMN_VERSION);
        String phone = wrapper.getString(AutoRegisterProviderEntry.COLUMN_PHONE);
        Pattern pattern = Pattern.compile(wrapper.getString(AutoRegisterProviderEntry.COLUMN_PATTERN));
        String accountUID = wrapper.getString(AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID);
        boolean enabled = wrapper.getBoolean(AutoRegisterProviderEntry.COLUMN_ENABLED);
        String lastSync = wrapper.getString(AutoRegisterProviderEntry.COLUMN_LAST_SYNC);

        AutoRegisterProvider provider = new AutoRegisterProvider(name, version);
        provider.setPhone(phone);
        provider.setPattern(pattern);
        provider.setAccountUID(accountUID);
        provider.setEnabled(enabled);
        if (lastSync != null)
            provider.setLastSync(TimestampHelper.getTimestampFromUtcString(lastSync));

        populateBaseModelAttributes(cursor, provider);
        return provider;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final AutoRegisterProvider provider) {
        stmt.clearBindings();
        stmt.bindString(1, provider.getName());
        stmt.bindString(2, provider.getVersion());
        stmt.bindString(3, provider.getPhone());
        stmt.bindString(4, provider.getPattern().toString());
        if (provider.getAccountUID() != null)
            stmt.bindString(5, provider.getAccountUID());
        stmt.bindLong(6, provider.isEnabled() ? 1L : 0L);
        if (provider.getLastSync() != null)
            stmt.bindString(7, TimestampHelper.getUtcStringFromTimestamp(provider.getLastSync()));
        stmt.bindString(8, provider.getUID());
        return stmt;
    }

    public List<AutoRegisterProvider> getDisabledProviders() {
        Log.d(LOG_TAG, "getDisalbedProviders()");

        return getAllRecords(
                AutoRegisterProviderEntry.COLUMN_ENABLED + " = ?",
                new String[] { "0" },
                null
        );
    }

    /**
     *
     * @param providerUID
     * @param accountUID
     * @return
     */
    public void setEnabled(@NonNull String providerUID, String accountUID){
        Log.d(LOG_TAG, "setEnabled(): uid = " + providerUID + ", account = " + accountUID);

        ContentValues contentValues = new ContentValues();
        contentValues.put(AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID, accountUID);
        contentValues.put(AutoRegisterProviderEntry.COLUMN_ENABLED, 1);

        updateRecord(providerUID, contentValues);
    }
}
