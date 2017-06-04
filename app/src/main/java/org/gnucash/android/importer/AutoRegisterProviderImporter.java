package org.gnucash.android.importer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.util.AutoRegisterUtil;
import org.gnucash.android.util.PreferencesHelper;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 *
 * XML Configuration file is located at res/raw/auto_register_providersrs.xml.
 *
 * Uses {@url https://github.com/googlei18n/libphonenumber} for formatting phone number.
 * Uses {@url https://github.com/tony19/named-regexp} for named capture group.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 9.
 */

public class AutoRegisterProviderImporter extends AsyncTask<String, Void, Void> {
    private static final String LOG_TAG = AutoRegisterProviderImporter.class.getSimpleName();

    /**
     * Context
     */
    private Context mContext;

    /**
     * Provider database adapter
     */
    private AutoRegisterProviderDbAdapter mProvidersDbAdapter;

    /**
     * Currently loaded version
     */
    private String mCurrentVersion;

    public AutoRegisterProviderImporter(Context context, AutoRegisterProviderDbAdapter providersDbAdapter) {
        mContext = context;
        mProvidersDbAdapter = providersDbAdapter;
    }

    @Override
    protected Void doInBackground(String... bookUID) {
        loadXml(bookUID[0]);
        return null;
    }

    private void loadXml(String bookUID) {
        mCurrentVersion = PreferencesHelper.getAutoRegisterVersion(bookUID);

        try {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.auto_register_providers);
            String versionFromXml = parseXml(inputStream);
            PreferencesHelper.setAutoRegisterVersion(bookUID, versionFromXml);
        } catch (ImportException e) {
            Log.i(LOG_TAG, e.getMessage());
        } catch (SAXException | ParserConfigurationException | IOException e) {
            Log.e(LOG_TAG, "Error loading auto-registers into the database");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @return Version code of XML file
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     */
    private String parseXml(InputStream inputStream) throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        BufferedInputStream bos = new BufferedInputStream(inputStream);

        XmlHandler handler = new XmlHandler();
        xr.setContentHandler(handler);
        xr.parse(new InputSource(bos));

        return handler.mVersionFromXml;
    }

    /**
     * Checks whether the newest configuration is already loaded.
     *
     * @param versionFromXml
     */
    private void assertXmlLoadNeeded(String versionFromXml) {
        if (versionFromXml.compareTo(mCurrentVersion) <= 0) {
            Log.i(LOG_TAG, "This book is already loaded with the newest autoregister configuration");
            throw new ImportException(ImportException.Type.ALREADY_NEWEST_VERSION, mCurrentVersion);
        }
    }

    /**
     *
     */
    private class XmlHandler extends DefaultHandler {
        /**
         *
         */
        private static final String TAG_ROOT = "autoregister";

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
         * &lt;pattern&gt; tag name
         */
        private static final String TAG_PATTERN = "pattern";

        private static final String ATTR_VERSION = "version";
        private static final String ATTR_NAME = "name";
        private static final String ATTR_VALUE = "value";
        private static final String ATTR_ICON = "icon";

        private String mVersionFromXml;

        private List<Pair<String, String>> mComponents = new ArrayList<>();
        private List<AutoRegister.Provider> mProviders = new ArrayList<>();

        private String _currentTagName;
        private AutoRegister.Provider _currentProvider;
        private StringBuffer _currentBuffer = new StringBuffer();
        private List<String> _currentPatterns = new ArrayList<>();
        private List<String> _currentGlobs = new ArrayList<>();

        @Override
        public void endDocument() throws SAXException {
            mProvidersDbAdapter.bulkAddRecords(mProviders, DatabaseAdapter.UpdateMethod.replace);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            _currentTagName = qName;

            switch (qName) {
                case TAG_ROOT:
                    mVersionFromXml = attrs.getValue(ATTR_VERSION);
                    Log.d(LOG_TAG, "mVersionFromXML: " + mVersionFromXml);
                    Log.d(LOG_TAG, "mCurrentVersion: " + mCurrentVersion);
                    assertXmlLoadNeeded(mVersionFromXml);

                    Log.i(LOG_TAG, String.format("Loading autoregister configuration (version %s)", mVersionFromXml));
                    break;

                case TAG_COMPONENT:
                    mComponents.add(new Pair<>(attrs.getValue(ATTR_NAME), attrs.getValue(ATTR_VALUE)));
                    Log.i(LOG_TAG, "Added component " + attrs.getValue(ATTR_NAME));
                    break;

                case TAG_PROVIDER:
                    _currentProvider = new AutoRegister.Provider(
                            attrs.getValue(ATTR_NAME),
                            attrs.getValue(ATTR_ICON));
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            switch (qName) {
                case TAG_PROVIDER:
                    _currentProvider.setPatterns(_currentPatterns.toArray(new String[] {}));
                    _currentProvider.setGlobs(_currentGlobs.toArray(new String[] {}));
                    mProviders.add(_currentProvider);
                    Log.i(LOG_TAG, "Added provider " + _currentProvider);

                    _currentPatterns.clear();
                    _currentGlobs.clear();
                    break;

                case TAG_PHONE:
                    _currentProvider.setPhone(AutoRegisterUtil.formatPhoneNumber(_currentBuffer.toString().trim()));
                    _currentBuffer.setLength(0);
                    break;

                case TAG_PATTERN:
                    _currentPatterns.add(transformPattern(_currentBuffer.toString().trim()));
                    _currentGlobs.add(transformGlob(_currentBuffer.toString().trim()));
                    _currentBuffer.setLength(0);
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            switch (_currentTagName) {
                case TAG_PHONE:
                case TAG_PATTERN:
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
            String pattern = patternText
                    .replaceAll("[()[\\\\]]", "\\\\$0")
                    .replaceAll(" ", "\\\\s+");

            for (Pair<String, String> p : mComponents) {
                String find = new StringBuilder().append('{').append(p.first).append('}').toString();
                String replace = new StringBuilder().append("(?<").append(p.first).append(">")
                        .append(p.second).append(')').toString();

                pattern = pattern.replace(find, replace);
            }

            return pattern;
        }

        /**
         * Transforms message format into valid regular expression with named capture groups.
         *
         * @param patternText
         * @return
         */
        private String transformGlob(String patternText) {
            String pattern = patternText.replaceAll("[*]", "?");
            return pattern.replaceAll("\\{[^}]*\\}", "*");
        }
    }

    /**
     *
     */
    public static class ImportException extends RuntimeException {
        protected enum Type {
            ALREADY_NEWEST_VERSION("Already has newest version %s");

            private String mMessage;

            Type(String message) {
                mMessage = message;
            }
        }
        Type mType;

        public ImportException(Type type, String... args) {
            super(String.format(type.mMessage, args));
        }

        public Type getType() {
            return mType;
        }
    }
}