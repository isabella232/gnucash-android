/*
 * Copyright (c) 2015 Ngewi Fet <ngewif@gmail.com>
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

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;

import org.gnucash.android.util.AutoRegisterMessage;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Class representing a provider(can be bank, credit card company, etc) configuration.
 * A provider can have multiple message patterns.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 14.
 */
public class AutoRegisterProvider extends BaseModel {
    private String mName;
    private String mVersion;
    private String mPhone;
    private Pattern mPattern;
    private String mAccountUID;
    private boolean mEnabled;
    private Timestamp mLastSync;

    /**
     * Create a new book instance
     *
     */
    public AutoRegisterProvider(String name, String version) {
        super();
        setUID(generateUID());
        this.mName = name;
        this.mVersion = version;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getPhone() {
        return mPhone;
    }

    public void setPhone(String phone) {
        mPhone = phone;
    }

    public Pattern getPattern() {
        return mPattern;
    }

    public void setPattern(Pattern pattern) {
        mPattern = pattern;
    }

    public void setAccountUID(String accountUID) {
        mAccountUID = accountUID;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public String getAccountUID() {
        return mAccountUID;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public Timestamp getLastSync() {
        return mLastSync;
    }

    public void setLastSync(Timestamp lastSync) {
        mLastSync = lastSync;
    }

    /**
     * Parses message text to create a new {@link AutoRegisterMessage} instance.
     *
     * @param text
     * @return
     */
    public AutoRegisterMessage parseMessage(String text) {
        Matcher matcher = mPattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        AutoRegisterMessage message = new AutoRegisterMessage();
        for (Map.Entry<String, String> entry : matcher.namedGroups().entrySet()) {
            message.set(entry.getKey(), entry.getValue());
        }

        return message;
    }
}
