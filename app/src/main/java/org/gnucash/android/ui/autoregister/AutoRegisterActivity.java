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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SwitchCompat;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.ui.util.FloatingActionButtonManager;
import org.gnucash.android.util.PreferencesHelper;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Manages actions related to accounts, displaying, exporting and creating new accounts
 * The various actions are implemented as Fragments which are then added to this activity
 */
public class AutoRegisterActivity extends BaseDrawerActivity {
    /**
     * Logging tag
     */
    protected static final String LOG_TAG = AutoRegisterActivity.class.getSimpleName();

    /**
     * Number of pages to show
     */
    private static final int DEFAULT_NUM_PAGES = 2;

    /**
     * Index for the providers tab
     */
    public static final int INDEX_PROVIDERS_FRAGMENT = 0;

    /**
     * Index for the keywords tab
     */
    public static final int INDEX_KEYWORDS_FRAGMENT = 1;

    /**
     * Map containing fragments for the different tabs
     */
    private SparseArray<Fragment> mFragmentPageReferenceMap = new SparseArray<>();

    /**
     * ViewPager which manages the different tabs
     */
    @BindView(R.id.pager) ViewPager mViewPager;
    @BindView(R.id.fab) FloatingActionButton mFloatingActionButton;

    @BindView(R.id.coordinatorLayout) CoordinatorLayout mCoordinatorLayout;

    private AutoRegisterViewPagerAdapter mPagerAdapter;

    private FloatingActionButtonManager mFABManager;

    private String mBookUID;

    /**
     * Adapter for managing the sub-account and transaction fragment pages in the accounts view
     */
    private class AutoRegisterViewPagerAdapter extends FragmentPagerAdapter {

        public AutoRegisterViewPagerAdapter(FragmentManager fm) {
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

                    case INDEX_KEYWORDS_FRAGMENT:
                        currentFragment = KeywordsListFragment.newInstance();
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
            switch (position) {
                case INDEX_KEYWORDS_FRAGMENT:
                    return getString(R.string.title_autoregister_keywords);
            }
            return getString(R.string.title_autoregister_providers);
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
        return R.string.title_autoregister;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_autoregister_providers));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_autoregister_keywords));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mPagerAdapter = new AutoRegisterViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
                mFABManager.revertToInitialState();
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

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int index = mViewPager.getCurrentItem();
                Fragment sourceFragment = mPagerAdapter.getItem(index);
                DialogFragment dialogFragment;
                switch (index) {
                    case INDEX_PROVIDERS_FRAGMENT:
                        dialogFragment = AddProviderDialogFragment.newInstance();
                        dialogFragment.setTargetFragment(sourceFragment, Activity.RESULT_OK);
                        dialogFragment.show(getSupportFragmentManager(), "add_provider_dialog");
                        break;

                    case INDEX_KEYWORDS_FRAGMENT:
                        dialogFragment = AddKeywordDialogFragment.newInstance();
                        dialogFragment.setTargetFragment(sourceFragment, Activity.RESULT_OK);
                        dialogFragment.show(getSupportFragmentManager(), "add_keyword_dialog");
                        break;
                }
            }
        };
        mFABManager = new FloatingActionButtonManager(
                mFloatingActionButton, R.drawable.ic_add_white_48dp, listener
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.autoregister_actions, menu);

        SwitchCompat onOffSwitch = (SwitchCompat) menu.getItem(0).getActionView()
                .findViewById(R.id.actionbar_switch);
        onOffSwitch.setChecked(PreferencesHelper.isAutoRegisterEnabled(mBookUID));
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setEnabled(b);
            }
        });
        return true;
    }

    protected FloatingActionButtonManager getFloatingActionButtomManager() {
        return mFABManager;
    }

    private void setEnabled(boolean enabled) {
        PreferencesHelper.setAutoRegisterEnabled(mBookUID, enabled);

        int messageId = enabled ? R.string.toast_autoregister_enabled : R.string.toast_autoregister_disabled;
        Toast.makeText(this, getString(messageId), Toast.LENGTH_SHORT).show();
    }
}