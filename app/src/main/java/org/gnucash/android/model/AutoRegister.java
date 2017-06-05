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

package org.gnucash.android.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Auto-register transaction (shortly, "Auto-register") represents a transaction made automatically
 * by reading messages from finance service companies like credit card companies or banks.
 *
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com>
 */

public class AutoRegister extends BaseModel {
    public enum Flag { COMPLETED, IGNORED };

    private String mInboxURI;
    private Flag mFlag;
    private String mTransactionUID;

    public AutoRegister(@NonNull String inboxURI, @Nullable Flag flag, @Nullable String transactionUID) {
        mInboxURI = inboxURI;
        mFlag = flag;
        mTransactionUID = transactionUID;
    }

    public String getInboxURI() {
        return mInboxURI;
    }

    public void setInboxURI(String inboxURI) {
        mInboxURI = inboxURI;
    }

    public Flag getFlag() {
        return mFlag;
    }

    public void setFlag(Flag flag) {
        mFlag = flag;
    }

    public String getTransactionUID() {
        return mTransactionUID;
    }

    public void setTransactionUID(String transactionUID) {
        mTransactionUID = transactionUID;
    }

    public static class Message {
        public enum Type { SMS };
        private static final String CARDNO = "cardno";
        private static final String APPROVALNO = "approvalno";
        private static final String HOLDER = "holder";
        private static final String MEMO = "memo";
        private static final String AMOUNT = "amount";
        private static final String ACCUM = "accum";
        private static final String CURRENCY = "currency";
        private static final String INSTALMENT = "instalment";
        private static final String YEAR = "year";
        private static final String MONTH = "month";
        private static final String DAY = "day";
        private static final String HOUR = "hour";
        private static final String MINUTE = "minute";

        /**
         * Message Type
         */
        private Type mType;

        /**
         * Message ID
         */
        private long mID;

        /**
         * Message timestamp
         */
        private Timestamp mTimestamp;

        /**
         * Message's sender address
         */
        private String mAddress;

        /**
         * Message body
         */
        private String mBody;

        /**
         * Matched provider
         */
        private Provider mProvider;

        /**
         * Map holding parsed body parts
         */
        private Map<String, String> mBodyParts;

        public Message(Provider provider, Type type, long ID, Timestamp timestamp, String address, String body) {
            mProvider = provider;
            mType = type;
            mID = ID;
            mTimestamp = timestamp;
            mAddress = address;
            mBody = body;
            mBodyParts = provider.parseBodyParts(this);
        }

        public Type getType() {
            return mType;
        }

        public long getID() {
            return mID;
        }

        public Timestamp getTimestamp() {
            return mTimestamp;
        }

        public String getAddress() {
            return mAddress;
        }

        public String getBody() {
            return mBody;
        }

        public Provider getProvider() {
            return mProvider;
        }

        public boolean isParsed() {
            return mBodyParts != null;
        }

        protected String getBodyPart(String key) {
            return mBodyParts != null ? mBodyParts.get(key) : null;
        }

        @Override
        public String toString() {
            return "Message{" +
                    "mBody='" + mBody + '\'' +
                    ", mProvider=" + mProvider +
                    ", mParsed=" + (mBodyParts != null) +
                    '}';
        }
    }

    /**
     * Represents an inbox message.
     *
     * @author Jin, Heonkyu <heonkyu.jin@gmail.com>
     */
    public static class Inbox extends BaseModel {
        /**
         * 
         */
        private Message mMessage;
        
        /**
         * Value
         */
        private Money mValue;

        /**
         *
         */
        private String mMemo;

        /**
         *
         */
        private boolean completed;

        /**
         * UID of transaction
         */
        private String mTransactionUID;

        /**
         * Matched keyword
         */
        private Keyword mKeyword;

        public Inbox(Message message) {
            mMessage = message;
            if (message.isParsed()) {
                try {
                    // init value
                    String currencyCode = mMessage.getBodyPart(Message.CURRENCY);
                    String amount = mMessage.getBodyPart(Message.AMOUNT).replaceAll(",", "");
                    mValue = new Money(amount,
                            currencyCode == null ? Money.DEFAULT_CURRENCY_CODE : currencyCode);

                    // init memo
                    mMemo = mMessage.getBodyPart(Message.MEMO);
                } catch (Exception e) {
                    Log.d("Inbox", mMessage.mBodyParts.toString());
                }
            }
        }

        public Inbox(Message message, Money value, String memo) {
            mMessage = message;
            mValue = value;
            mMemo = memo;
        }

        public Message getMessage() {
            return mMessage;
        }

        public Money getValue() {
            return mValue;
        }

        public void setValue(Money value) {
            mValue = value;
        }

        public String getMemo() {
            return mMemo;
        }

        public void setMemo(String memo) {
            mMemo = memo;
        }

        public String getHolder() {
            return mMessage.getBodyPart(Message.HOLDER);
        }

        public String getCardNo() {
            return mMessage.getBodyPart(Message.CARDNO);
        }

        public String getInstalment() {
            return mMessage.getBodyPart(Message.INSTALMENT);
        }

        private int convertField(String key) {
            try {
                return Integer.parseInt(mMessage.getBodyPart(key));
            } catch (Exception e) {
                return 0;
            }
        }

        public long getTimeMillis() {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(mMessage.getTimestamp().getTime());

            int year = convertField(Message.YEAR);
            calendar.set(year != 0 ? year : calendar.get(Calendar.YEAR),
                    convertField(Message.MONTH) - 1, convertField(Message.DAY),
                    convertField(Message.HOUR), convertField(Message.MINUTE));

            return calendar.getTimeInMillis();
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public String getTransactionUID() {
            return mTransactionUID;
        }

        public void setTransactionUID(String transactionUID) {
            mTransactionUID = transactionUID;
        }

        public boolean hasKeyword() {
            return mKeyword != null;
        }

        public Keyword getKeyword() {
            return mKeyword;
        }

        public void setKeyword(Keyword keyword) {
            mKeyword = keyword;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("Inbox : {")
                    .append("message = ").append(mMessage).append("\n")
                    .append("keyword = ").append(mKeyword).append("\n")
                    .append("}").toString();
        }
    }

    /**
     * Class representing a provider(can be bank, credit card company, etc) configuration.
     * A provider can have multiple message patterns.
     *
     * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 14.
     */
    public static class Provider extends BaseModel {
        private static final String FIELD_SEPARATOR = "__%%__";

        private String mName;
        private String mPhone;
        private Pattern[] mPatterns;
        private String[] mGlobs;
        private String mAccountUID;
        private String mAccountName;
        private String mIconName;
        private boolean mActive;
        private Timestamp mLastSync;

        /**
         * Create a new provider instance
         *
         */
        public Provider(String name, String iconName) {
            super();
            setUID(generateUID());
            this.mName = name;
            this.mIconName = iconName;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getPhone() {
            return mPhone;
        }

        public void setPhone(String phone) {
            mPhone = phone;
        }

        public Pattern[] getPatterns() {
            return mPatterns;
        }

        public String getPatternsAsString() {
            return TextUtils.join(FIELD_SEPARATOR, mPatterns);
        }

        public void setPatterns(String[] patterns) {
            mPatterns = new Pattern[patterns.length];
            for (int i = 0; i < patterns.length; i++) {
                mPatterns[i] = Pattern.compile(patterns[i]);
            }
        }

        public void setPatterns(String patternText) {
            setPatterns(patternText.split(FIELD_SEPARATOR));
        }

        public String[] getGlobs() {
            return mGlobs;
        }

        public String getGlobsAsString() {
            return TextUtils.join(FIELD_SEPARATOR, mGlobs);
        }

        public void setGlobs(String[] globs) {
            mGlobs = Arrays.copyOf(globs, globs.length);
        }

        public void setGlobs(String patternText) {
            mGlobs = patternText.replaceAll("\\{[^}]+\\}", "*").split(AutoRegister.Provider.FIELD_SEPARATOR);
        }

        public void setAccountUID(String accountUID) {
            mAccountUID = accountUID;
        }

        public void setActive(boolean active) {
            mActive = active;
        }

        public String getAccountUID() {
            return mAccountUID;
        }

        public String getIconName() {
            return mIconName;
        }

        public void setIconName(String iconName) {
            this.mIconName = iconName;
        }

        public boolean isActive() {
            return mActive;
        }

        public Timestamp getLastSync() {
            return mLastSync;
        }

        public void setLastSync(Timestamp lastSync) {
            mLastSync = lastSync;
        }

        public String getAccountName() {
            return mAccountName;
        }

        public void setAccountName(String accountName) {
            mAccountName = accountName;
        }

        /**
         * Parses message and returns parsed body parts
         *
         * @param message
         * @return
         */
        public Map<String, String> parseBodyParts(Message message) {
            for (Pattern pattern : mPatterns) {
                Matcher matcher = pattern.matcher(message.getBody());
                if (matcher.find()) {
                    return matcher.namedGroups();
                }
            }
            return null;
        }
    }

    /**
     * Represents a GnuCash book which is made up of accounts and transactions
     * @author Ngewi Fet <ngewif@gmail.com>
     */
    public static class Keyword extends BaseModel {
        private String mKeyword;
        private int mPriority;
        private String mAccountUID;
        private String mAccountName;

        public Keyword(String keyword, int priority, String accountUID) {
            mKeyword = keyword;
            mPriority = priority;
            mAccountUID = accountUID;
        }

        public String getKeyword() {
            return mKeyword;
        }

        public void setKeyword(String keyword) {
            mKeyword = keyword;
        }

        public int getPriority() {
            return mPriority;
        }

        public void setPriority(int priority) {
            mPriority = priority;
        }

        public String getAccountUID() {
            return mAccountUID;
        }

        public void setAccountUID(String accountUID) {
            mAccountUID = accountUID;
        }

        public String getAccountName() {
            return mAccountName;
        }

        public void setAccountName(String accountName) {
            mAccountName = accountName;
        }
    }
}
