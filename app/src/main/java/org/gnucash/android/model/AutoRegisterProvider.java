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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class representing a provider(can be bank, credit card company, etc) configuration.
 * A provider can have multiple message patterns.
 *
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 14.
 */
public class AutoRegisterProvider extends BaseModel {
    private String mName;
    private String mDescription;
    private String mPhoneNo;
    private String mVersion;
    private String mAccountUID;
    private boolean mEnabled;
    private Timestamp mLastSync;

    private List<Pattern> mPatterns = new ArrayList<>();

    /**
     * Create a new book instance
     *
     */
    public AutoRegisterProvider(String name, String description, String phoneNo, String version){
        super();
        setUID(generateUID());
        this.mName = name;
        this.mDescription = description;
        this.mPhoneNo = phoneNo;
        this.mVersion = version;
    }

    /**
     * Adds new message parse pattern.
     *
     * @param pattern
     */
    public void addPattern(Pattern pattern) {
        mPatterns.add(pattern);
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getPhoneNo() {
        return mPhoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        mPhoneNo = phoneNo;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getAccountUID() {
        return mAccountUID;
    }

    public void setAccountUID(String accountUID) {
        mAccountUID = accountUID;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
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
        boolean found = false;
        Matcher matcher = null;
        for (Pattern p : mPatterns) {
            matcher = p.matcher(text);
            if (matcher.find()) {
                found = true;
                break;
            }
        }

        if (!found) return null;

        AutoRegisterMessage message = new AutoRegisterMessage();
        for (Map.Entry<String, String> entry : matcher.namedGroups().entrySet()) {
            message.set(entry.getKey(), entry.getValue());
        }

        return message;
    }

    @Override
    public String toString() {
        return "AutoRegisterProvider{" +
                "mName='" + mName + '\'' +
                ", mPhoneNo='" + mPhoneNo + '\'' +
                ", mDescription='" + mDescription + '\'' +
                '}';
    }
}
