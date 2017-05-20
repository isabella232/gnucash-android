package org.gnucash.android.importer;

import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.code.regexp.Pattern;

import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.util.AutoRegisterUtil;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * XML Configuration file is located at res/raw/auto_register_configs.xml.
 *
 * Uses {@link https://github.com/googlei18n/libphonenumber} for formatting phone number.
 * Uses {@link https://github.com/tony19/named-regexp} for named capture group.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 9.
 */

public class AutoRegisterXmlHandler extends DefaultHandler {
    private static final String LOG_TAG = AutoRegisterXmlHandler.class.getSimpleName();

    /**
     * &lt;component&gt; tag name
     */
    private static final String TAG_COMPONENT = "component";
    /**
     * &lt;provider&gt; tag name
     */
    private static final String TAG_PROVIDER = "provider";
    /**
     * &lt;phone&gt; tag name
     */
    private static final String TAG_PHONE = "phone";
    /**
     * &lt;message&gt; tag name
     */
    private static final String TAG_MESSAGE = "message";

    private static final String ATTR_NAME = "name";
    private static final String ATTR_VALUE = "value";
    private static final String ATTR_VERSION = "version";

    private AutoRegisterProviderDbAdapter mProvidersDbAdapter;

    private List<Pair<String, String>> mComponents = new ArrayList<>();
    private List<AutoRegisterProvider> mProviders = new ArrayList<>();

    private String _currentTagName;
    private AutoRegisterProvider _currentProvider;
    private StringBuffer _currentBuffer = new StringBuffer();
    private List<String> _currentPatterns = new ArrayList<>();

    public AutoRegisterXmlHandler(SQLiteDatabase db) {
        if (db == null){
            mProvidersDbAdapter = GnuCashApplication.getAutoRegisterProviderDbAdapter();
        } else {
            mProvidersDbAdapter = new AutoRegisterProviderDbAdapter(db);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        mProvidersDbAdapter.bulkAddRecords(mProviders, DatabaseAdapter.UpdateMethod.insert);
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
                        attrs.getValue(ATTR_VERSION));
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case TAG_PROVIDER:
                _currentProvider.setPattern(concatenatePatterns(_currentPatterns));
                mProviders.add(_currentProvider);
                Log.i(LOG_TAG, "Added provider " + _currentProvider);

                _currentPatterns.clear();
                break;

            case TAG_PHONE:
                _currentProvider.setPhone(AutoRegisterUtil.formatPhoneNumber(_currentBuffer.toString().trim()));
                _currentBuffer.setLength(0);
                break;

            case TAG_MESSAGE:
                _currentPatterns.add(transformPattern(_currentBuffer.toString().trim()));
                _currentBuffer.setLength(0);
                break;
        }
    }

    private Pattern concatenatePatterns(List<String> patterns) {
        StringBuffer buf = new StringBuffer();
        for (String each : patterns) {
            buf.append("(?:").append(each).append(")|");
        }
        buf.setLength(buf.length() - 1);
        return Pattern.compile(buf.toString());
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch (_currentTagName) {
            case TAG_PHONE:
            case TAG_MESSAGE:
                _currentBuffer.append(ch, start, length);
                break;
        }
    }

    /**
     * Transforms message format into valid regular expression with named capture groups.
     *
     * @param patternText
     * @return
     */
    private String transformPattern(String patternText) {
        String pattern = patternText.replaceAll("[()[\\\\]]", "\\\\$0");

        for (Pair<String, String> p : mComponents) {
            String find = new StringBuilder().append('{').append(p.first).append('}').toString();
            String replace = new StringBuilder().append("(?<").append(p.first).append(">")
                    .append(p.second).append(')').toString();

            pattern = pattern.replace(find, replace);
        }

        return pattern;
    }
}