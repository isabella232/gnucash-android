package org.gnucash.android.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 9.
 */

public class AutoRegisterMessage {
    public static final String CARDNO = "cardno";
    public static final String APPROVALNO = "approvalno";
    public static final String HOLDER = "holder";
    public static final String VENDOR = "vendor";
    public static final String AMOUNT = "amount";
    public static final String ACCUM = "accum";
    public static final String CURRENCY = "currency";
    public static final String INSTALMENT = "instalment";
    public static final String DATE = "date";
    public static final String TIME = "time";

    private Map<String, String> _map = new HashMap<>();

    public String get(String key) {
        return _map.get(key);
    }

    public BigDecimal getBigDecimal(String key) {
        return new BigDecimal(get(key).replaceAll(",", ""));
    }

    public void set(String key, String value) {
        _map.put(key, value);
    }
 }
