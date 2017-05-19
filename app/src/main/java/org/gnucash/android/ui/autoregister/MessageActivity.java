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

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewGroup;

import org.gnucash.android.R;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.ui.common.Refreshable;

import butterknife.BindView;

/**
 * Manages actions related to accounts, displaying, exporting and creating new accounts
 * The various actions are implemented as Fragments which are then added to this activity
 *
 */
public class MessageActivity extends BaseDrawerActivity {
    /**
     * Logging tag
     */
    protected static final String LOG_TAG = MessageActivity.class.getSimpleName();

    private static final int REQUEST_READ_SMS_PERMISSION = 1001;
    private static final int REQUEST_RECEIVE_SMS_PERMISSION = 1002;

    /**
     * Number of pages to show
     */
    private static final int DEFAULT_NUM_PAGES = 1;

    /**
     * Index for the providers tab
     */
    public static final int INDEX_UNPROCESSED_FRAGMENT = 0;

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
                    case INDEX_UNPROCESSED_FRAGMENT:
                        currentFragment = MessageListFragment.newInstance();
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
        return R.layout.activity_autoregister_messages;
    }

    @Override
    public int getTitleRes() {
        return R.string.title_auto_register;
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText(R.string.title_auto_register_messages_unprocessed));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

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
    }

    @Override
    protected void onResume() {
        super.onResume();

/*
        int index = mViewPager.getCurrentItem();
        Fragment fragment = (Fragment) mFragmentPageReferenceMap.get(index);
        fragment.setArguments(getIntent().getExtras());
*/
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.autoregister_messages_actions, menu);

        return true;
	}
}