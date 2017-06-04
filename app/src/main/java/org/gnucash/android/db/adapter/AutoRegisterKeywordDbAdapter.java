/*
 * Copyright (c) 2017 Jin, Heonkyu <heonkyu.jin@gmail.com>
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
import android.support.v4.util.Pair;
import android.util.Log;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema.AutoRegisterKeywordEntry;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.util.CursorThrowWrapper;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Database adapter for creating/modifying Auto-register keyword entries
 */
public class AutoRegisterKeywordDbAdapter extends DatabaseAdapter<AutoRegister.Keyword> {

    /**
     * Accounts database adapter
     */
    private AccountsDbAdapter mAccountsDbAdapter;

    /**
     * List of enabled keywords for caching
     */
    private List<AutoRegister.Keyword> mEnabledKeywordList;

    /**
     * Opens the database adapter with an existing database
     * @param db        SQLiteDatabase object
     */
    public AutoRegisterKeywordDbAdapter(SQLiteDatabase db, AccountsDbAdapter accountsDbAdapter) {
        super(db, AutoRegisterKeywordEntry.TABLE_NAME, new String[] {
                AutoRegisterKeywordEntry.COLUMN_KEYWORD,
                AutoRegisterKeywordEntry.COLUMN_PRIORITY,
                AutoRegisterKeywordEntry.COLUMN_ACCOUNT_UID
        });
        this.mAccountsDbAdapter = accountsDbAdapter;

        refresh();
    }

    /**
     * Refreshes internal cache
     */
    private void refresh() {
        mEnabledKeywordList = getAllRecords(null, null, AutoRegisterKeywordEntry.COLUMN_PRIORITY);

        for (AutoRegister.Keyword k : mEnabledKeywordList) {
            k.setAccountName(mAccountsDbAdapter.getFullyQualifiedAccountName(k.getAccountUID()));
        }
    }

    /**
     * Return the application instance of the Auto-register keyword database adapter
     * @return keyword database adapter
     */
    public static AutoRegisterKeywordDbAdapter getInstance(){
        return GnuCashApplication.getAutoRegisterKeywordDbAdapter();
    }

    @Override
    public AutoRegister.Keyword buildModelInstance(@NonNull Cursor cursor) {
        CursorThrowWrapper wrapper = new CursorThrowWrapper(cursor);

        String keyword =  wrapper.getString(AutoRegisterKeywordEntry.COLUMN_KEYWORD);
        int priority =  wrapper.getInt(AutoRegisterKeywordEntry.COLUMN_KEYWORD);
        String accountUID = wrapper.getString(AutoRegisterKeywordEntry.COLUMN_ACCOUNT_UID);

        AutoRegister.Keyword model = new AutoRegister.Keyword(keyword, priority, accountUID);
        populateBaseModelAttributes(cursor, model);
        return model;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final AutoRegister.Keyword keyword) {
        stmt.clearBindings();
        stmt.bindString(1, keyword.getKeyword());
        stmt.bindLong(2, keyword.getPriority());
        stmt.bindString(3, keyword.getAccountUID());
        stmt.bindString(4, keyword.getUID());
        return stmt;
    }

    @Override
    public void addRecord(@NonNull AutoRegister.Keyword model, UpdateMethod updateMethod) {
        super.addRecord(model, updateMethod);
        refresh();
    }

    @Override
    public boolean deleteRecord(@NonNull String uid) {
        Iterator<AutoRegister.Keyword> it = mEnabledKeywordList.iterator();
        while (it.hasNext()) {
            AutoRegister.Keyword k = it.next();
            if (k.getUID().equals(uid)) it.remove();
        }
        return super.deleteRecord(uid);
    }

    /**
     *
     *
     * @param memo
     * @return
     */
    public AutoRegister.Keyword findFirstMatchingKeyword(String memo) {
        for (AutoRegister.Keyword k : mEnabledKeywordList) {
            if (memo.contains(k.getKeyword())) {
                return k;
            }
        }
        return null;
    }

    public void updatePriorities(List<Pair<Long, Integer>> updates) {
        beginTransaction();
        for (Pair<Long, Integer> update : updates) {
            updateRecord(AutoRegisterKeywordEntry.TABLE_NAME, update.first,
                    AutoRegisterKeywordEntry.COLUMN_PRIORITY, update.second.toString());
        }
        setTransactionSuccessful();
        endTransaction();
        refresh();
    }
}
