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

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema.AutoRegisterProviderEntry;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.util.TimestampHelper;

/**
 * Database adapter for creating/modifying Auto-register provider entries
 */
public class AutoRegisterProviderDbAdapter extends DatabaseAdapter<AutoRegisterProvider> {
    private final AccountsDbAdapter mAccountsDbAdapter;

    /**
     * Opens the database adapter with an existing database
     * @param db        SQLiteDatabase object
     */
    public AutoRegisterProviderDbAdapter(SQLiteDatabase db, AccountsDbAdapter accountsDbAdapter) {
        super(db, AutoRegisterProviderEntry.TABLE_NAME, new String[] {
                AutoRegisterProviderEntry.COLUMN_NAME,
                AutoRegisterProviderEntry.COLUMN_DESCRIPTION,
                AutoRegisterProviderEntry.COLUMN_PHONE_NO,
                AutoRegisterProviderEntry.COLUMN_VERSION,
                AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID,
                AutoRegisterProviderEntry.COLUMN_ENABLED,
                AutoRegisterProviderEntry.COLUMN_LAST_SYNC
        });

        mAccountsDbAdapter = accountsDbAdapter;
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
        String name = cursor.getString(cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_NAME));
        String description =  cursor.getString(cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_DESCRIPTION));
        String phoneNo = cursor.getString(cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_PHONE_NO));
        String version = cursor.getString(cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_VERSION));
        String accountUID = cursor.getString(cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID));
        int enabled = cursor.getInt(cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_ENABLED));
        String lastSync = cursor.getString(cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_LAST_SYNC));

        AutoRegisterProvider provider = new AutoRegisterProvider(
                name, description, phoneNo, version);
        provider.setAccountUID(accountUID);
        provider.setEnabled(enabled > 0);
        provider.setLastSync(TimestampHelper.getTimestampFromUtcString(lastSync));

        populateBaseModelAttributes(cursor, provider);
        return provider;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final AutoRegisterProvider provider) {
        stmt.clearBindings();
        stmt.bindString(1, provider.getName());
        stmt.bindString(2, provider.getDescription());
        stmt.bindString(3, provider.getPhoneNo());
        stmt.bindString(4, provider.getVersion());
        stmt.bindString(5, provider.getAccountUID());
        stmt.bindLong(6, provider.isEnabled() ? 1L : 0L);
        if (provider.getLastSync() != null)
            stmt.bindString(7, TimestampHelper.getUtcStringFromTimestamp(provider.getLastSync()));
        stmt.bindString(8, provider.getUID());
        return stmt;
    }

    /**
     * Sets the book with unique identifier {@code uid} as active and all others as inactive
     * <p>If the parameter is null, then the currently active book is not changed</p>
     * @param providerUID Unique identifier of the book
     * @return GUID of the currently active book
     */
    public String setEnabled(@NonNull String providerUID, boolean enabled){
        ContentValues contentValues = new ContentValues();
        contentValues.put(AutoRegisterProviderEntry.COLUMN_ENABLED, enabled ? 1 : 0);
        mDb.update(mTableName, contentValues, AutoRegisterProviderEntry.COLUMN_UID + " = ?", new String[]{providerUID});

        return providerUID;
    }
}
