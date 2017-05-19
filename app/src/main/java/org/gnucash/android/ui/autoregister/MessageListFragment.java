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

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
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
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.CursorRecyclerAdapter;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;
import org.gnucash.android.util.AutoRegisterManager;
import org.gnucash.android.util.AutoRegisterMessage;
import org.gnucash.android.util.CursorThrowWrapper;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.gnucash.android.db.DatabaseSchema.AutoRegisterProviderEntry;

/**
 *
 */
public class MessageListFragment extends Fragment implements Refreshable,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = MessageListFragment.class.getSimpleName();

    private MessageRecyclerAdapter mMessageRecyclerAdapter;

    @BindView(R.id.auto_register_recycler_view) EmptyRecyclerView mRecyclerView;
    @BindView(R.id.empty_view) TextView mEmptyTextView;

    public static MessageListFragment newInstance() {
        MessageListFragment fragment = new MessageListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_autoregister_messages_list, container, false);

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

//        String providerUID = getArguments().getString(UxArgument.AUTOREGISTER_PROVIDER_UID);
        AutoRegisterProvider provider = GnuCashApplication.getAutoRegisterManager().findProvider("1800-1111");

        mMessageRecyclerAdapter = new MessageRecyclerAdapter(null, provider);
        mRecyclerView.setAdapter(mMessageRecyclerAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
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
        if (mMessageRecyclerAdapter != null)
            mMessageRecyclerAdapter.swapCursor(null);
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

        return new CursorLoader(getContext(),
                Telephony.Sms.Inbox.CONTENT_URI,
                null,
                //Telephony.Sms.Inbox.CREATOR + " = '1800-1111'",
                null,
                null,
                Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
                );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "Providers loader finished. Swapping in cursor");
        mMessageRecyclerAdapter.swapCursor(data);
        mMessageRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "Resetting the providers loader");
        mMessageRecyclerAdapter.swapCursor(null);
    }

    private class MessageRecyclerAdapter extends CursorRecyclerAdapter<MessageViewHolder> {
        private AutoRegisterProvider mProvider;

        public MessageRecyclerAdapter(Cursor cursor, AutoRegisterProvider provider) {
            super(cursor);
            mProvider = provider;
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_autoregister_message, parent, false
            );
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolderCursor(final MessageViewHolder holder, final Cursor cursor) {
            CursorThrowWrapper wrapper = new CursorThrowWrapper(cursor);

            String body = wrapper.getString(Telephony.Sms.Inbox.BODY);
            AutoRegisterMessage message = mProvider.parseMessage(body);

            if (message != null) {
                holder.primaryText.setText(message.getVendor());
                holder.secondaryText.setText(message.getInstalment());
                holder.amount.setText(message.getAmount().toString());
                holder.date.setText(message.getDate());
            } else {
                holder.primaryText.setText("ERROR");
                holder.secondaryText.setText(wrapper.getString(Telephony.Mms.Inbox.CREATOR));
            }
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        @BindView(R.id.primary_text) TextView primaryText;
        @BindView(R.id.secondary_text) TextView secondaryText;
        @BindView(R.id.message_amount) TextView amount;
        @BindView(R.id.message_date) TextView date;
        @BindView(R.id.options_menu) ImageView optionsMenu;
        @BindView(R.id.message_color_strip) View colorStripView;
        String providerId;

        public MessageViewHolder(View itemView) {
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