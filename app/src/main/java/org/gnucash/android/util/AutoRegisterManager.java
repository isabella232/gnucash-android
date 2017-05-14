package org.gnucash.android.util;

import android.content.Context;
import android.support.v4.util.Pair;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.AutoRegisterProvider;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.google.code.regexp.Pattern;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import static com.google.i18n.phonenumbers.PhoneNumberUtil.*;

/**
 *
 * XML Configuration file is located at res/raw/auto_register_configs.xml.
 *
 * Uses {@link https://github.com/googlei18n/libphonenumber} for formatting phone number.
 * Uses {@link https://github.com/tony19/named-regexp} for named capture group.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 9.
 */

public class AutoRegisterManager extends DefaultHandler {
    private static final String LOG_TAG = AutoRegisterManager.class.getSimpleName();

    private Locale mDefaultLocale;
    private PhoneNumberUtil mPhoneNumberUtil = getInstance();

    /**
     * &lt;component&gt; tag name
     */
    private static final String TAG_COMPONENT = "component";
    /**
     * &lt;provider&gt; tag name
     */
    private static final String TAG_PROVIDER = "provider";
    /**
     * &lt;pattern&gt; tag name
     */
    private static final String TAG_PATTERN = "pattern";

    private static final String ATTR_NAME = "name";
    private static final String ATTR_DESCRIPTION = "description";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_PHONE = "phoneNo";
    private static final String ATTR_VERSION = "version";

    private List<Pair<String, String>> mComponents = new ArrayList<>();
    private List<AutoRegisterProvider> mProviders = new ArrayList<>();

    private String _currentTagName;
    private AutoRegisterProvider _currentProvider;
    private StringBuffer _currentBuffer = new StringBuffer();

    public AutoRegisterManager() {
        mDefaultLocale = GnuCashApplication.getDefaultLocale();
        loadConfigs();
    }

    private void loadConfigs() {
        SAXParserFactory spf = SAXParserFactory.newInstance();

        try {
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(this);

            Context context = GnuCashApplication.getAppContext();
            InputStream is = context.getResources().openRawResource(R.raw.auto_register_configs);
            BufferedInputStream bis = new BufferedInputStream(is);

            xr.parse(new InputSource(bis));
        } catch (SAXException | ParserConfigurationException | IOException e) {
            Log.e(LOG_TAG, "Error loading currencies into the database");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param phoneNo
     * @return
     */
    public AutoRegisterProvider findProvider(String phoneNo) {
        try {
            Phonenumber.PhoneNumber p = mPhoneNumberUtil.parse(phoneNo, mDefaultLocale.getCountry());
            phoneNo = mPhoneNumberUtil.format(p, PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
        }

        for (AutoRegisterProvider provider : mProviders) {
            MatchType match = mPhoneNumberUtil.isNumberMatch(phoneNo, provider.getPhoneNo());
            if (match == MatchType.EXACT_MATCH ||
                match == MatchType.SHORT_NSN_MATCH ||
                match == MatchType.NSN_MATCH) {
                return provider;
            }
        }

        return null;
    }

    public List<AutoRegisterProvider> getProviders() {
        return mProviders;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
        _currentTagName = qName;

        switch (qName) {
            case TAG_COMPONENT:
                mComponents.add(new Pair<>(attrs.getValue(ATTR_NAME), attrs.getValue(ATTR_VALUE)));
                Log.i(LOG_TAG, "Added component " + attrs.getValue(ATTR_NAME));
                break;

            case TAG_PROVIDER:
                _currentProvider = new AutoRegisterProvider(
                        attrs.getValue(ATTR_NAME),
                        attrs.getValue(ATTR_DESCRIPTION),
                        attrs.getValue(ATTR_PHONE),
                        attrs.getValue(ATTR_VERSION));
                mProviders.add(_currentProvider);

                Log.i(LOG_TAG, "Added provider " + _currentProvider);
                break;

            case TAG_PATTERN:
                _currentBuffer.setLength(0);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (_currentTagName.equals(TAG_PATTERN)) {
            _currentBuffer.append(ch, start, length);
            Pattern pattern = transformToPattern(_currentBuffer.toString().trim());
            _currentProvider.addPattern(pattern);
        }
    }

    /**
     * Transforms message format into valid regular expression with named capture groups.
     *
     * @param patternText
     * @return
     */
    private Pattern transformToPattern(String patternText) {
        String regex = patternText.replaceAll("[()[\\\\]]", "\\\\$0");

        for (Pair<String, String> p : mComponents) {
            String find = new StringBuilder().append('{').append(p.first).append('}').toString();
            String replace = new StringBuilder().append("(?<").append(p.first).append(">")
                    .append(p.second).append(')').toString();

            regex = regex.replace(find, replace);
        }

        return Pattern.compile(regex);
    }
}