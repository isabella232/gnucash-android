package org.gnucash.android.util;

import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.gnucash.android.app.GnuCashApplication;

import java.util.Locale;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import static com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance;

/**
 *
 * XML Configuration file is located at res/raw/auto_register_configs.xml.
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
}