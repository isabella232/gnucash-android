package org.gnucash.android.util;

import android.text.TextUtils;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A helper which facilitates building query selection for {@link android.database.Cursor}.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com>
 */

public class QuerySelectionBuilder {
    /**
     * List that holds selections
     */
    private List<String> mSelections = new ArrayList<>();

    /**
     * List that holds selection arguments;
     */
    private List<String> mSelectionArgs = new ArrayList<>();

    /**
     * Appends a new where clause
     *
     * @param selection selection
     * @param selectionArgs selection arguments
     * @return {@link QuerySelectionBuilder} instance
     */
    public QuerySelectionBuilder append(String selection, String... selectionArgs) {
        int placeholderCount = countPlaceholders(selection);
        int argumentCount = selectionArgs.length;

        if (placeholderCount != argumentCount) {
            throw new RuntimeException(String.format(
                    "# of '?'(%1$d) and # of arguments(%2$d) don't match.",
                    placeholderCount, argumentCount)
            );
        }

        mSelections.add(selection);
        mSelectionArgs.addAll(Arrays.asList(selectionArgs));

        return this;
    }

    public String getSelection() {
        return TextUtils.join(" AND ", mSelections);
    }

    public String[] getSelectionArgs() {
        return mSelectionArgs.toArray(new String[mSelectionArgs.size()]);
    }

    private int countPlaceholders(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '?') count++;
        }
        return count;
    }

    /**
     * Utility function for concatenating strings.
     *
     * @param parts Strings to concatenate.
     * @return Concatenated String
     */
    public static String concat(CharSequence... parts) {
        StringBuilder sb = new StringBuilder();
        for (CharSequence part : parts) {
            sb.append(part);
        }
        return sb.toString();
    }
}