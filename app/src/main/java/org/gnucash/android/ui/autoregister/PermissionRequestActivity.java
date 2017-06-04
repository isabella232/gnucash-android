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

package org.gnucash.android.ui.autoregister;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.util.PreferencesHelper;

import butterknife.BindView;

/**
 * Manages actions related to accounts, displaying, exporting and creating new accounts
 * The various actions are implemented as Fragments which are then added to this activity
 *
 */
public class PermissionRequestActivity extends BaseDrawerActivity {
    /**
     * Logging tag
     */
    protected static final String LOG_TAG = PermissionRequestActivity.class.getSimpleName();

    protected static final int REQUEST_SMS_PERMISSION = 1001;

    private boolean mReadPermissionGranted;
    private boolean mReceivePermissionGranted;

    @BindView(R.id.btn_request_permission) Button mPermissionRequestButton;

    @Override
    public int getContentView() {
        return R.layout.activity_autoregister_permission;
    }

    @Override
    public int getTitleRes() {
        return R.string.title_autoregister;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionRequestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestSMSPermission();
            }
        });
    }

    private void requestSMSPermission() {
        if (!isPermissionGranted(Manifest.permission.READ_SMS)) return;
        if (!isPermissionGranted(Manifest.permission.RECEIVE_SMS)) return;

        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS },
                REQUEST_SMS_PERMISSION);
    }

    private boolean isPermissionGranted(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Log.d(LOG_TAG, "Rationale = " + permission);
                return false;
            }
        }

        return true;
    }

    private void setEnabled() {
        Log.d(LOG_TAG, "setEnabled()");

        String bookUID = BooksDbAdapter.getInstance().getActiveBookUID();
        PreferencesHelper.setAutoRegisterEnabled(bookUID, true);

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult: requestCode = " + requestCode);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.msg_read_sms_granted), Toast.LENGTH_SHORT).show();
            setEnabled();
        }
    }

}