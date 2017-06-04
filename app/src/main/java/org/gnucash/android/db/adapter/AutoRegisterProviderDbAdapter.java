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

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema.AutoRegisterProviderEntry;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.util.AutoRegisterUtil;
import org.gnucash.android.util.CursorThrowWrapper;
import org.gnucash.android.util.TimestampHelper;

import java.util.HashMap;
import java.util.List;

/**
 * Database adapter for creating/modifying Auto-register provider entries
 */
public class AutoRegisterProviderDbAdapter extends DatabaseAdapter<AutoRegister.Provider> {

    /**
     * Accounts database adapter
     */
    private AccountsDbAdapter mAccountsDbAdapter;

    /**
     * Cached list of active providers
     */
    private List<AutoRegister.Provider> mActiveProviders;

    /**
     * HashMap to hold matched phone number to increase overall speed by caching
     */
    private HashMap<String, AutoRegister.Provider> mProviderPhoneMap = new HashMap<>();

    /**
     * HashMap to hold UID to increase overall speed by caching
     */
    private HashMap<String, AutoRegister.Provider> mProviderUIDMap = new HashMap<>();

    /**
     * Opens the database adapter with an existing database
     * @param db        SQLiteDatabase object
     */
    public AutoRegisterProviderDbAdapter(SQLiteDatabase db, AccountsDbAdapter accountsDbAdapter) {
        super(db, AutoRegisterProviderEntry.TABLE_NAME, new String[] {
                AutoRegisterProviderEntry.COLUMN_NAME,
                AutoRegisterProviderEntry.COLUMN_PHONE,
                AutoRegisterProviderEntry.COLUMN_PATTERNS,
                AutoRegisterProviderEntry.COLUMN_GLOBS,
                AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID,
                AutoRegisterProviderEntry.COLUMN_ICON_NAME,
                AutoRegisterProviderEntry.COLUMN_ACTIVE,
                AutoRegisterProviderEntry.COLUMN_LAST_SYNC
        });
        this.mAccountsDbAdapter = accountsDbAdapter;

        refresh();
    }

    /**
     * Refreshes cache
     */
    public void refresh() {
        mActiveProviders = getProvidersByStatus(true);

        for (AutoRegister.Provider p : mActiveProviders) {
            p.setAccountName(mAccountsDbAdapter.getFullyQualifiedAccountName(p.getAccountUID()));
        }
    }

    /**
     * Return the application instance of the books database adapter
     * @return Books database adapter
     */
    public static AutoRegisterProviderDbAdapter getInstance(){
        return GnuCashApplication.getAutoRegisterProviderDbAdapter();
    }

    @Override
    public AutoRegister.Provider buildModelInstance(@NonNull Cursor cursor) {
        CursorThrowWrapper wrapper = new CursorThrowWrapper(cursor);

        String name =  wrapper.getString(AutoRegisterProviderEntry.COLUMN_NAME);
        String phone = wrapper.getString(AutoRegisterProviderEntry.COLUMN_PHONE);
        String patternText = wrapper.getString(AutoRegisterProviderEntry.COLUMN_PATTERNS);
        String globText = wrapper.getString(AutoRegisterProviderEntry.COLUMN_GLOBS);
        String accountUID = wrapper.getString(AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID);
        String iconName = wrapper.getString(AutoRegisterProviderEntry.COLUMN_ICON_NAME);
        boolean enabled = wrapper.getBoolean(AutoRegisterProviderEntry.COLUMN_ACTIVE);
        String lastSync = wrapper.getString(AutoRegisterProviderEntry.COLUMN_LAST_SYNC);

        AutoRegister.Provider provider = new AutoRegister.Provider(name, iconName);
        provider.setPhone(phone);
        provider.setPatterns(patternText);
        provider.setGlobs(globText);
        provider.setAccountUID(accountUID);
        provider.setActive(enabled);
        if (lastSync != null)
            provider.setLastSync(TimestampHelper.getTimestampFromUtcString(lastSync));

        populateBaseModelAttributes(cursor, provider);
        return provider;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final AutoRegister.Provider provider) {
        stmt.clearBindings();
        stmt.bindString(1, provider.getName());
        stmt.bindString(2, provider.getPhone());
        stmt.bindString(3, provider.getPatternsAsString());
        stmt.bindString(4, provider.getGlobsAsString());
        if (provider.getAccountUID() != null)
            stmt.bindString(5, provider.getAccountUID());
        if (provider.getIconName() != null)
            stmt.bindString(6, provider.getIconName());
        stmt.bindLong(7, provider.isActive() ? 1L : 0L);
        if (provider.getLastSync() != null)
            stmt.bindString(8, TimestampHelper.getUtcStringFromTimestamp(provider.getLastSync()));
        stmt.bindString(9, provider.getUID());
        return stmt;
    }

    /**
     * Returns cached list of enabled providers
     *
     * @return
     */
    public List<AutoRegister.Provider> getActiveProviders() {
        return mActiveProviders;
    }

    /**
     * Find appropriate provider that matches the given phone number.
     *
     * @param phone
     * @return
     */
    public AutoRegister.Provider findActiveProviderByPhone(String phone) {
        if (mProviderPhoneMap.containsKey(phone)) {
            return mProviderPhoneMap.get(phone);
        }

        for (AutoRegister.Provider p : mActiveProviders) {
            if (AutoRegisterUtil.haveSamePhoneNumber(p.getPhone(), phone)) {
                mProviderPhoneMap.put(phone, p);
                return p;
            }
        }
        return null;
    }

    /**
     * Find appropriate provider that matches the given UID.
     *
     * @param UID
     * @return
     */
    public AutoRegister.Provider findActiveProviderByUID(String UID) {
        if (mProviderUIDMap.containsKey(UID)) {
            return mProviderPhoneMap.get(UID);
        }

        for (AutoRegister.Provider p : mActiveProviders) {
            if (p.getUID().equals(UID)) {
                mProviderPhoneMap.put(UID, p);
                return p;
            }
        }
        return null;
    }

    public List<AutoRegister.Provider> getDisabledProviders() {
        return getProvidersByStatus(false);
    }

    private List<AutoRegister.Provider> getProvidersByStatus(boolean enabled) {
        Log.d(LOG_TAG, "getProvidersByStatus(): enabled = " + enabled);

        return getAllRecords(
                AutoRegisterProviderEntry.COLUMN_ACTIVE + " = ?",
                new String[] { enabled ? "1" : "0" },
                null
        );
    }

    /**
     *
     * @param providerUID
     * @param accountUID
     * @return
     */
    public void setActive(@NonNull String providerUID, String accountUID){
        Log.d(LOG_TAG, "setActive(): uid = " + providerUID + ", account = " + accountUID);

        ContentValues contentValues = new ContentValues();
        contentValues.put(AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID, accountUID);
        contentValues.put(AutoRegisterProviderEntry.COLUMN_ACTIVE, 1);

        updateRecord(providerUID, contentValues);
        refresh();
    }

    /**
     *
     * @param providerUID
     * @return
     */
    public void setInactive(@NonNull String providerUID){
        Log.d(LOG_TAG, "setInactive(): uid = " + providerUID);

        ContentValues contentValues = new ContentValues();
        contentValues.put(AutoRegisterProviderEntry.COLUMN_ACTIVE, 0);

        updateRecord(providerUID, contentValues);
        refresh();
    }
}
