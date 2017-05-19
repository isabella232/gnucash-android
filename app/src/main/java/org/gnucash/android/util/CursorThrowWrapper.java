package org.gnucash.android.util;

import android.database.Cursor;
import android.database.CursorWrapper;

/**
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 19.
 */

public class CursorThrowWrapper extends CursorWrapper {
    public CursorThrowWrapper(Cursor cursor) {
        super(cursor);
    }

    public byte[] getBlob(String columnName) {
        return getBlob(getColumnIndexOrThrow(columnName));
    }

    public boolean getBoolean(String columnName) {
        return getInt(columnName) > 0;
    }

    public double getDouble(String columnName) {
        return getDouble(getColumnIndexOrThrow(columnName));
    }

    public float getFloat(String columnName) {
        return getFloat(getColumnIndexOrThrow(columnName));
    }

    public int getInt(String columnName) {
        return getInt(getColumnIndexOrThrow(columnName));
    }

    public long getLong(String columnName) {
        return getLong(getColumnIndexOrThrow(columnName));
    }

    public long getShort(String columnName) {
        return getShort(getColumnIndexOrThrow(columnName));
    }

    public String getString(String columnName) {
        return getString(getColumnIndexOrThrow(columnName));
    }
}
