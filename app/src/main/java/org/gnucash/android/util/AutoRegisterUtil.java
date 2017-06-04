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

package org.gnucash.android.util;

import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.model.Commodity;
import org.gnucash.android.model.Split;
import org.gnucash.android.model.Transaction;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import static com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance;

/**
 *
 * XML Configuration file is located at res/raw/auto_register_providers.xmlml.
 *
 * Uses {@link https://github.com/googlei18n/libphonenumber} for formatting phone number.
 * Uses {@link https://github.com/tony19/named-regexp} for named capture group.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 9.
 */

public class AutoRegisterUtil {
    private static final String LOG_TAG = AutoRegisterUtil.class.getSimpleName();

    private static PhoneNumberUtil mPhoneNumberUtil = getInstance();

    public static String formatPhoneNumber(String phone) {
        try {
            Phonenumber.PhoneNumber p = mPhoneNumberUtil.parse(phone, GnuCashApplication.getDefaultLocale().getCountry());
            return mPhoneNumberUtil.format(p, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            Log.e(LOG_TAG, "Phone number format error: " + phone);
            return "";
        }
    }

    public static String stripCountryCodeFromPhoneNumber(String phone) {
        try {
            Phonenumber.PhoneNumber p = mPhoneNumberUtil.parse(phone, GnuCashApplication.getDefaultLocale().getCountry());
            return String.valueOf(p.getNationalNumber());
        } catch (NumberParseException e) {
            Log.e(LOG_TAG, "Phone number format error: " + phone);
            return "";
        }
    }

    public static boolean haveSamePhoneNumber(String phone1, String phone2) {
        PhoneNumberUtil.MatchType match = mPhoneNumberUtil.isNumberMatch(phone1, phone2);
        switch (match) {
            case SHORT_NSN_MATCH:
            case NSN_MATCH:
            case EXACT_MATCH:
                return true;
            default:
                return false;
        }
    }

    public static void createTransaction(AutoRegister.Inbox inbox) {
        Transaction transaction = new Transaction(inbox.getMemo());
        transaction.setTime(inbox.getTimeMillis());
        transaction.setCommodity(inbox.getValue().getCommodity());

        Split split = new Split(inbox.getValue(), inbox.getMessage().getProvider().getAccountUID());
        transaction.addSplit(split);
        transaction.addSplit(split.createPair(inbox.getKeyword().getAccountUID()));

        TransactionsDbAdapter.getInstance().addRecord(transaction, DatabaseAdapter.UpdateMethod.insert);
    }
}