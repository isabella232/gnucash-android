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
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.model.AutoRegister;
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

    @BindView(R.id.autoregister_provider_recycler_view) EmptyRecyclerView mRecyclerView;
    @BindView(R.id.empty_view) TextView mEmptyTextView;

    public static ProvidersListFragment newInstance() {
        ProvidersListFragment fragment = new ProvidersListFragment();
        fragment.mProviderDbAdapter = AutoRegisterProviderDbAdapter.getInstance();
        fragment.mAccountsDbAdapter = AccountsDbAdapter.getInstance();

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProviderRecyclerAdapter = new ProviderRecyclerAdapter(null);
        mRecyclerView.setAdapter(mProviderRecyclerAdapter);

        getActivity().findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment dialogFragment = AddProviderDialogFragment.newInstance();
                dialogFragment.setTargetFragment(ProvidersListFragment.this, Activity.RESULT_OK);
                dialogFragment.show(getActivity().getSupportFragmentManager(), "add_provider_dialog");
            }
        });
    }

    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume()");
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProviderRecyclerAdapter != null)
            mProviderRecyclerAdapter.swapCursor(null);
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

        return new DatabaseCursorLoader(getContext()) {
            @Override
            public Cursor loadInBackground() {
                Cursor cursor = mProviderDbAdapter.fetchAllRecords(
                        AutoRegisterProviderEntry.COLUMN_ACTIVE + " = ?",
                        new String[]{"1"},
                        null
                );

                if (cursor != null)
                    registerContentObserver(cursor);
                return cursor;
            }
        };
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

    private void showDisableConfirmationDialog(final String providerUID) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_confirm_delete)
                .setMessage(R.string.msg_disable_provider_confirm)
                .setIcon(android.R.drawable.ic_delete)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                mProviderDbAdapter.setInactive(providerUID);
                                refresh();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.alert_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        })
                .create();

        dialog.show();
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
            final AutoRegister.Provider provider = mProviderDbAdapter.buildModelInstance(cursor);

            @DrawableRes int iconId = getResources().getIdentifier(
                    provider.getIconName(), "drawable", getContext().getPackageName());
            holder.icon.setImageDrawable(getResources().getDrawable(iconId));

            holder.primaryText.setText(provider.getName());
            holder.secondaryText.setText(
                    mAccountsDbAdapter.getFullyQualifiedAccountName(provider.getAccountUID())
            );

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDisableConfirmationDialog(provider.getUID());
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MessageActivity.launchWithProvider(getContext(), provider.getUID());
                }
            });
        }
    }

    class ProviderViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        @BindView(R.id.icon) ImageView icon;
        @BindView(R.id.primary_text) TextView primaryText;
        @BindView(R.id.secondary_text) TextView secondaryText;
        @BindView(R.id.account_balance) TextView accountBalance;
        @BindView(R.id.delete_btn) ImageView deleteButton;
        @BindView(R.id.options_menu) ImageView optionsMenu;

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