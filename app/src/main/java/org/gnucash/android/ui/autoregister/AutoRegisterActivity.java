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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.kobakei.ratethisapp.RateThisApp;

import org.gnucash.android.BuildConfig;
import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.BooksDbAdapter;
import org.gnucash.android.export.xml.GncXmlExporter;
import org.gnucash.android.importer.ImportAsyncTask;
import org.gnucash.android.ui.account.AccountsListFragment;
import org.gnucash.android.ui.account.OnAccountClickedListener;
import org.gnucash.android.ui.common.BaseDrawerActivity;
import org.gnucash.android.ui.common.FormActivity;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.TaskDelegate;
import org.gnucash.android.ui.wizard.FirstRunWizardActivity;

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
     * Used to save the index of the last open tab and restore the pager to that index
     */
    public static final String LAST_OPEN_TAB_INDEX = "last_open_tab";

    /**
     * Key for putting argument for tab into bundle arguments
     */
    public static final String EXTRA_TAB_INDEX = "org.gnucash.android.extra.TAB_INDEX";

    /**
     * Map containing fragments for the different tabs
     */
    private SparseArray<Fragment> mFragmentPageReferenceMap = new SparseArray<>();

    /**
     * ViewPager which manages the different tabs
     */
    @BindView(R.id.pager) ViewPager mViewPager;
    @BindView(R.id.fab_create_account) FloatingActionButton mFloatingActionButton;
    @BindView(R.id.coordinatorLayout) CoordinatorLayout mCoordinatorLayout;

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

        //show the simple accounts list
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

        setCurrentTab();

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
	}

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Sets the current tab in the ViewPager
     */
    public void setCurrentTab(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int lastTabIndex = preferences.getInt(LAST_OPEN_TAB_INDEX, INDEX_PROVIDERS_FRAGMENT);
        int index = getIntent().getIntExtra(EXTRA_TAB_INDEX, lastTabIndex);
        mViewPager.setCurrentItem(index);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.edit().putInt(LAST_OPEN_TAB_INDEX, mViewPager.getCurrentItem()).apply();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.global_actions, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
            case android.R.id.home:
                return super.onOptionsItemSelected(item);

		default:
			return false;
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