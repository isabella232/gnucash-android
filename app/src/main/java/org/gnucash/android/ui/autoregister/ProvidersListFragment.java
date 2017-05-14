/*
 * Copyright (c) 2016 Ngewi Fet <ngewif@gmail.com>
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
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.ui.account.AccountsActivity;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.util.CursorRecyclerAdapter;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.gnucash.android.db.DatabaseSchema.AutoRegisterProviderEntry;

/**
 *
 */
public class ProvidersListFragment extends Fragment implements Refreshable,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = ProvidersListFragment.class.getSimpleName();

    private AutoRegisterProviderDbAdapter mProviderDbAdapter;
    private AccountsDbAdapter mAccountsDbAdapter;

    private ProviderRecyclerAdapter mProviderRecyclerAdapter;

    @BindView(R.id.auto_register_recycler_view) EmptyRecyclerView mRecyclerView;
    @BindView(R.id.empty_view) TextView mEmptyTextView;

    public static ProvidersListFragment newInstance() {
        ProvidersListFragment fragment = new ProvidersListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_autoregister_providers_list, container, false);

        ButterKnife.bind(this, v);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setEmptyView(mEmptyTextView);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), 2);
            mRecyclerView.setLayoutManager(gridLayoutManager);
        } else {
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(mLayoutManager);
        }

        return v;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

/*
        ActionBar actionbar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionbar.setTitle(R.string.title_auto_register_providers);
        actionbar.setDisplayHomeAsUpEnabled(true);
        setHasOptionsMenu(true);
*/

        mProviderRecyclerAdapter = new ProviderRecyclerAdapter(null);
        mRecyclerView.setAdapter(mProviderRecyclerAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        mProviderDbAdapter = AutoRegisterProviderDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume()");
        super.onResume();
        refresh();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_list_actions, menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProviderRecyclerAdapter != null)
            mProviderRecyclerAdapter.swapCursor(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_create_book:
                AccountsActivity.createDefaultAccounts(GnuCashApplication.getDefaultCurrencyCode(), getActivity());
                return true;

            default:
                return false;
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void refresh() {
        Log.d(LOG_TAG, "refresh()");
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void refresh(String uid) {
        Log.d(LOG_TAG, "refresh(uid)");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "Creating the providers loader");

        return new ProviderCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "Providers loader finished. Swapping in cursor");
        mProviderRecyclerAdapter.swapCursor(data);
        mProviderRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "Resetting the providers loader");
        mProviderRecyclerAdapter.swapCursor(null);
    }

    private static final class ProviderCursorLoader extends DatabaseCursorLoader {
        ProviderCursorLoader(Context context) {
            super(context);
        }

        @Override
        public Cursor loadInBackground() {
            AutoRegisterProviderDbAdapter adapter = AutoRegisterProviderDbAdapter.getInstance();
            Cursor cursor = adapter.fetchAllRecords();

            if (cursor != null)
                registerContentObserver(cursor);
            return cursor;
        }
    }

    private class ProviderRecyclerAdapter extends CursorRecyclerAdapter<ProviderViewHolder> {
        public ProviderRecyclerAdapter(Cursor cursor) {
            super(cursor);
        }

        @Override
        public ProviderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_autoregister_provider, parent, false
            );
            return new ProviderViewHolder(v);
        }

        @Override
        public void onBindViewHolderCursor(final ProviderViewHolder holder, final Cursor cursor) {
            final String uid = cursor.getString(
                    cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_UID)
            );
            String description = cursor.getString(
                    cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_DESCRIPTION)
            );
            String phoneNo = cursor.getString(
                    cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_PHONE_NO)
            );

            String accountUID = cursor.getString(
                    cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_ACCOUNT_UID)
            );
            String accountName = mAccountsDbAdapter.getAccountFullName(accountUID);

            String title = new StringBuilder().append(description).append(" > ")
                    .append(accountName).toString();

            holder.providerName.setText(title);
            holder.description.setText(phoneNo);

            boolean isEnabled = cursor.getLong(
                    cursor.getColumnIndexOrThrow(AutoRegisterProviderEntry.COLUMN_ENABLED)
            ) > 0;
            holder.providerOnoff.setChecked(isEnabled);
            holder.providerOnoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mProviderDbAdapter.setEnabled(uid, b);
                }
            });
        }
    }

    class ProviderViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        @BindView(R.id.primary_text) TextView providerName;
        @BindView(R.id.secondary_text) TextView description;
        @BindView(R.id.account_balance) TextView accountBalance;
        @BindView(R.id.create_transaction) ImageView createTransaction;
        @BindView(R.id.provider_onoff) SwitchCompat providerOnoff;
        @BindView(R.id.options_menu) ImageView optionsMenu;
        @BindView(R.id.account_color_strip) View colorStripView;
        @BindView(R.id.budget_indicator) ProgressBar budgetIndicator;
        String providerId;

        public ProviderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            optionsMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), v);
                    //popup.setOnMenuItemClickListener(AccountsListFragment.AccountRecyclerAdapter.AccountViewHolder.this);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.account_context_menu, popup.getMenu());
                    popup.show();
                }
            });

        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()){
                case R.id.context_menu_edit_accounts:
                    //openCreateOrEditActivity(accoundId);
                    return true;

                case R.id.context_menu_delete:
                    //tryDeleteAccount(accoundId);
                    return true;

                default:
                    return false;
            }
        }

    }
}