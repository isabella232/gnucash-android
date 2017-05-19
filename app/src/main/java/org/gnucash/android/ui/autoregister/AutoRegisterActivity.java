/*
 * Copyright (c) 2012 - 2014 Ngewi Fet <ngewif@gmail.com>
 * Copyright (c) 2014 Yongxin Wang <fefe.wyx@gmail.com>
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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.util.PreferencesHelper;
import org.joda.time.tz.Provider;

import butterknife.BindView;

/**
 * Manages actions related to accounts, displaying, exporting and creating new accounts
 * The various actions are implemented as Fragments which are then added to this activity
 *
 */
public class AutoRegisterActivity extends BaseDrawerActivity {
    /**
     * Logging tag
     */
    protected static final String LOG_TAG = AutoRegisterActivity.class.getSimpleName();

    private static final int REQUEST_READ_SMS_PERMISSION = 1001;
    private static final int REQUEST_RECEIVE_SMS_PERMISSION = 1002;

    /**
     * Number of pages to show
     */
    private static final int DEFAULT_NUM_PAGES = 2;

    /**
     * Index for the providers tab
     */
    public static final int INDEX_PROVIDERS_FRAGMENT = 0;

    /**
     * Index for the vendors tab
     */
    public static final int INDEX_MAPPINGS_FRAGMENT = 1;

    /**
     * Map containing fragments for the different tabs
     */
    private SparseArray<Fragment> mFragmentPageReferenceMap = new SparseArray<>();

    /**
     * ViewPager which manages the different tabs
     */
    @BindView(R.id.pager) ViewPager mViewPager;
    @BindView(R.id.fab_create_account) FloatingActionButton mFloatingActionButton;

    private AutoRegisterViewPagerAdapter mPagerAdapter;

    /**
     * Adapter for managing the sub-account and transaction fragment pages in the accounts view
     */
    private class AutoRegisterViewPagerAdapter extends FragmentPagerAdapter {

        public AutoRegisterViewPagerAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment currentFragment = mFragmentPageReferenceMap.get(i);
            if (currentFragment == null) {
                switch (i) {
                    case INDEX_PROVIDERS_FRAGMENT:
                        currentFragment = ProvidersListFragment.newInstance();
                        break;

                    case INDEX_MAPPINGS_FRAGMENT:
                        currentFragment = MappingsListFragment.newInstance();
                        break;
                }
                mFragmentPageReferenceMap.put(i, currentFragment);
            }
            return currentFragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragmentPageReferenceMap.remove(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case INDEX_MAPPINGS_FRAGMENT:
                    return getString(R.string.title_auto_register_mappings);
            }
            return getString(R.string.title_auto_register_providers);
        }

        @Override
        public int getCount() {
            return DEFAULT_NUM_PAGES;
        }
    }

    @Override
    public int getContentView() {
        return R.layout.activity_autoregister;
    }

    @Override
    public int getTitleRes() {
        return R.string.title_auto_register;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_auto_register_providers));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_auto_register_mappings));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        if (PreferencesHelper.hasAutoRegisterRun()) {
            tabLayout.setVisibility(View.VISIBLE);
        } else {
            tabLayout.setVisibility(View.GONE);
        }

        boolean enabled = PreferencesHelper.isAutoRegisterEnabled();
        Log.d(LOG_TAG, "enabled = " + enabled);

        mPagerAdapter = new AutoRegisterViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //nothing to see here, move along
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //nothing to see here, move along
            }
        });

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mViewPager.getCurrentItem();
                if (index == INDEX_PROVIDERS_FRAGMENT) {
                    DialogFragment dialogFragment = AddProviderDialogFragment.newInstance();
                    dialogFragment.setTargetFragment(mPagerAdapter.getItem(index), Activity.RESULT_OK);
                    dialogFragment.show(getSupportFragmentManager(), "add_provider_dialog");
                }
            }
        });
        mFloatingActionButton.setVisibility(enabled ? View.VISIBLE : View.GONE);
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.autoregister_actions, menu);

        SwitchCompat onOffSwitch = (SwitchCompat) menu.getItem(0).getActionView()
                .findViewById(R.id.actionbar_switch);
        onOffSwitch.setChecked(PreferencesHelper.isAutoRegisterEnabled());
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    setEnabled();
                } else {
                    setDisabled();
                }
            }
        });
        return true;
	}
	
	private void setEnabled() {
        Log.d(LOG_TAG, "setEnabled()");

        if (!PreferencesHelper.hasAutoRegisterRun()) {
            PreferencesHelper.setAutoRegisterHasRun(true);
        }

        requestSMSPermission(Manifest.permission.READ_SMS, REQUEST_READ_SMS_PERMISSION);
        requestSMSPermission(Manifest.permission.RECEIVE_SMS, REQUEST_RECEIVE_SMS_PERMISSION);

        PreferencesHelper.setAutoRegisterEnabled(true);
    }

    private void setDisabled() {
        Log.d(LOG_TAG, "setDisabled()");
        PreferencesHelper.setAutoRegisterEnabled(false);
    }

	private void requestSMSPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission)) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }

        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult: requestCode = " + requestCode);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            int messageId = -1;
            switch (requestCode) {
                case REQUEST_READ_SMS_PERMISSION:
                    messageId = R.string.msg_read_sms_granted;
                    break;
                case REQUEST_RECEIVE_SMS_PERMISSION:
                    messageId = R.string.msg_receive_sms_granted;
                    break;
            }

            if (messageId > 0) {
                Toast.makeText(this, getString(messageId), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
	 * Removes the flag indicating that the app is being run for the first time.
	 * This is called every time the app is started because the next time won't be the first time
	 */
/*
	public static void removeFirstRunFlag(){
        Context context = GnuCashApplication.getAppContext();
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(context.getString(R.string.key_first_run), false);
		editor.commit();
	}
*/

}