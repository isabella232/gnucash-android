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
import android.content.Context;
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
import android.view.MenuItem;
import android.view.ViewGroup;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.importer.AutoRegisterProviderImporter;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.util.PreferencesHelper;

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
     * Tab layout
     */
    @BindView(R.id.tab_layout) TabLayout mTabLayout;

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
                currentFragment = MessageListFragment.newInstance(MessageListFragment.Mode.values()[i]);
                currentFragment.setArguments(getIntent().getExtras());
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
            return getString(MessageListFragment.Mode.values()[position].mTitleRes);
        }

        @Override
        public int getCount() {
            return MessageListFragment.Mode.values().length;
        }
    }

    @Override
    public int getContentView() {
        return R.layout.activity_autoregister_messages;
    }

    @Override
    public int getTitleRes() {
        return R.string.title_autoregister_messages;
    }

    public static void launchWithProvider(Context context, String providerUID) {
        Log.d(LOG_TAG, "launchWithAccount(): uid = " + providerUID);

        Intent i = new Intent(context, MessageActivity.class);
        i.putExtra(UxArgument.AUTOREGISTER_SELECTED_PROVIDER_UID, providerUID);

        context.startActivity(i);
    }

    public static void launchWithKeyword(Context context, String keyword) {
        Log.d(LOG_TAG, "launchWithKeyword(): keyword = " + keyword);

        Intent i = new Intent(context, MessageActivity.class);
        i.putExtra(UxArgument.AUTOREGISTER_KEYWORD, keyword);

        context.startActivity(i);
    }

    @Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String currentBookUID = BooksDbAdapter.getInstance().getActiveBookUID();
        requestSMSPermissions(currentBookUID);
        updateProviderConfiguration(currentBookUID);

        for (MessageListFragment.Mode m : MessageListFragment.Mode.values()) {
            mTabLayout.addTab(mTabLayout.newTab().setText(m.mTitleRes));
        }
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mPagerAdapter = new AutoRegisterViewPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
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
        Fragment fragment = mFragmentPageReferenceMap.get(index);
        fragment.setArguments(getIntent().getExtras());
*/
    }

    /**
     * Requests SMS permission.
     *
     * @param currentBookUID
     */
    private void requestSMSPermissions(String currentBookUID) {
        if (PreferencesHelper.isAutoRegisterEnabled(currentBookUID)) return;

        Intent i = new Intent(this, PermissionRequestActivity.class);
        startActivityForResult(i, PermissionRequestActivity.REQUEST_SMS_PERMISSION);
        finish();
    }

    /**
     * Load provider configuration into database
     *
     * @param currentBookUID
     */
    private void updateProviderConfiguration(String currentBookUID) {
        AutoRegisterProviderImporter providerImporter = new AutoRegisterProviderImporter(
                this, AutoRegisterProviderDbAdapter.getInstance()
        );
        providerImporter.execute(currentBookUID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PermissionRequestActivity.REQUEST_SMS_PERMISSION &&
                resultCode == Activity.RESULT_OK) {
            Log.d(LOG_TAG, "success");
        }
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.autoregister_messages_actions, menu);

        return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_autoregister_config:
                startActivity(new Intent(this, AutoRegisterActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}