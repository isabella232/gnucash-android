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

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.DatabaseSchema.AutoRegisterEntry;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.util.CursorThrowWrapper;

/**
 * Database adapter for creating/modifying Auto-register entries
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com>
 */
public class AutoRegisterDbAdapter extends DatabaseAdapter<AutoRegister> {

    /**
     * Opens the database adapter with an existing database
     * @param db        SQLiteDatabase object
     */
    public AutoRegisterDbAdapter(SQLiteDatabase db) {
        super(db, AutoRegisterEntry.TABLE_NAME, new String[] {
                AutoRegisterEntry.COLUMN_INBOX_URI,
                AutoRegisterEntry.COLUMN_FLAG,
                AutoRegisterEntry.COLUMN_TRANSACTION_UID
        });
    }

    /**
     * Return the application instance of the database adapter
     * @return Auto-register database adapter
     */
    public static AutoRegisterDbAdapter getInstance(){
        return GnuCashApplication.getAutoRegisterMessageDbAdapter();
    }

    @Override
    public AutoRegister buildModelInstance(@NonNull Cursor cursor) {
        CursorThrowWrapper wrapper = new CursorThrowWrapper(cursor);

        String inboxUri =  wrapper.getString(AutoRegisterEntry.COLUMN_INBOX_URI);
        AutoRegister.Flag flag = AutoRegister.Flag.valueOf(wrapper.getString(AutoRegisterEntry.COLUMN_FLAG));
        String transactionUID = wrapper.getString(AutoRegisterEntry.COLUMN_TRANSACTION_UID);

        AutoRegister model = new AutoRegister(inboxUri, flag, transactionUID);
        populateBaseModelAttributes(cursor, model);
        return model;
    }

    @Override
    protected @NonNull SQLiteStatement setBindings(@NonNull SQLiteStatement stmt, @NonNull final AutoRegister entry) {
        stmt.clearBindings();
        stmt.bindString(1, entry.getInboxURI());
        stmt.bindString(2, entry.getFlag().name());
        stmt.bindString(3, entry.getTransactionUID());
        stmt.bindString(4, entry.getUID());
        return stmt;
    }
}
