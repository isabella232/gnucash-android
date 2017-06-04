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
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.adapter.AutoRegisterInboxDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterKeywordDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.model.Money;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.transaction.TransactionsActivity;
import org.gnucash.android.ui.util.CursorRecyclerAdapter;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;
import org.gnucash.android.util.AutoRegisterUtil;
import org.gnucash.android.util.CursorThrowWrapper;
import org.gnucash.android.util.TimestampHelper;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Fragment for displaying auto-register message list.
 *
 */
public class MessageListFragment extends Fragment implements Refreshable,
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = MessageListFragment.class.getSimpleName();

    /**
     * Enumeration for message list modes
     */
    protected enum Mode {
        INBOX(R.string.title_autoregister_messages_inbox),
        COMPLETED(R.string.title_autoregister_messages_completed),
        RECOGNIZED(R.string.title_autoregister_messages_recognized),
        UNRECOGNIZED(R.string.title_autoregister_messages_unrecognized),
        ALL(R.string.title_autoregister_messages_all);

        @StringRes
        int mTitleRes;

        Mode(@StringRes int titleRes) {
            mTitleRes = titleRes;
        }
    }

    private Mode mMode;

    /**
     * Inbox database adapter
     */
    private AutoRegisterInboxDbAdapter mInboxDbAdapter;

    /**
     * Provider database adapter
     */
    private AutoRegisterProviderDbAdapter mProviderDbAdapter;

    /**
     * Keyword database adapter
     */
    private AutoRegisterKeywordDbAdapter mKeywordDbAdapter;

    private AutoRegister.Provider mCurrentProvider;
    private AutoRegister.Keyword mCurrentKeyword;

    private MessageRecyclerAdapter mMessageRecyclerAdapter;

    @BindView(R.id.autoregister_recycler_view)
    EmptyRecyclerView mRecyclerView;
    @BindView(R.id.empty_view)
    TextView mEmptyTextView;

    public static MessageListFragment newInstance(Mode mode) {
        MessageListFragment fragment = new MessageListFragment();
        fragment.mMode = mode;
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInboxDbAdapter = AutoRegisterInboxDbAdapter.getInstance();
        mProviderDbAdapter = AutoRegisterProviderDbAdapter.getInstance();
        mKeywordDbAdapter = AutoRegisterKeywordDbAdapter.getInstance();

        Bundle arguments = getArguments();
        if (arguments != null) {
            String currentProviderUID = arguments.getString(UxArgument.AUTOREGISTER_SELECTED_PROVIDER_UID);
            if (currentProviderUID != null) {
                mCurrentProvider = mProviderDbAdapter.getRecord(currentProviderUID);
            }

            String currentKeyword = arguments.getString(UxArgument.AUTOREGISTER_KEYWORD);
            if (currentKeyword != null) {
                mCurrentKeyword = mKeywordDbAdapter.findFirstMatchingKeyword(currentKeyword);
            }
        }
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

        mMessageRecyclerAdapter = new MessageRecyclerAdapter(null);
        mRecyclerView.setAdapter(mMessageRecyclerAdapter);

        //@TODO
/*
        mMessageRecyclerAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                ;
            }
        });
        mMessageRecyclerAdapter.getFilter().filter()
*/
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

        return new DatabaseCursorLoader(getContext()) {
            @Override
            public Cursor loadInBackground() {
                Cursor cursor = mInboxDbAdapter.fetchRecords(
                        mCurrentProvider, mCurrentKeyword);

                if (cursor != null)
                    registerContentObserver(cursor);
                return cursor;
            }
        };
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

    /**
     * Shows add new keyword dialog.
     *
     * @param memo
     */
    private void showAddKeywordDialog(String memo) {
        DialogFragment dialogFragment = AddKeywordDialogFragment.newInstance();

        Bundle bundle = new Bundle();
        bundle.putString(UxArgument.AUTOREGISTER_KEYWORD, memo);
        dialogFragment.setArguments(bundle);

        dialogFragment.setTargetFragment(this, Activity.RESULT_OK);
        dialogFragment.show(getActivity().getSupportFragmentManager(), "add_keyword_dialog");
    }

    private class MessageRecyclerAdapter extends CursorRecyclerAdapter<MessageViewHolder> {
        public MessageRecyclerAdapter(Cursor cursor) {
            super(cursor);
        }

        @Override
        public int getItemViewType(int position) {
            return super.getItemViewType(position);
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_autoregister_message, parent, false
            );
            return new MessageViewHolder(v);
        }

        @Override
        public void onBindViewHolderCursor(final MessageViewHolder holder, Cursor cursor) {
            AutoRegister.Inbox inbox = mInboxDbAdapter.buildModelInstance(cursor);
            AutoRegister.Message message = inbox.getMessage();

            holder.mInbox = inbox;

            @DrawableRes int iconId = getResources().getIdentifier(
                    message.getProvider().getIconName(), "drawable", getContext().getPackageName());
            holder.icon.setImageDrawable(getResources().getDrawable(iconId));

            if (message.isParsed()) {
                holder.smallLabel.setText(inbox.getCardNo());
                holder.primaryText.setText(inbox.getMemo());
                holder.secondaryText.setText(inbox.hasKeyword() ?
                        inbox.getKeyword().getAccountName() :
                        "");

                if (inbox.getValue() != null) {
                    TransactionsActivity.displayBalance(holder.amount, inbox.getValue());
                }

                holder.createTransactionButton.setVisibility(
                        inbox.hasKeyword() ? View.VISIBLE : View.INVISIBLE
                );
                holder.date.setText(TransactionsActivity.getPrettyDateFormat(getContext(),
                        inbox.getTimeMillis()
                ));
            } else {
                holder.smallLabel.setText("");
                holder.primaryText.setText(R.string.label_unrecognizable_message);
                holder.secondaryText.setText(message.getBody());

                TransactionsActivity.displayBalance(holder.amount, Money.getZeroInstance());

                holder.createTransactionButton.setVisibility(View.INVISIBLE);
                holder.date.setText(TransactionsActivity.getPrettyDateFormat(getContext(),
                        message.getTimestamp().getTime()
                ));
            }

        }

        @Override
        public CharSequence convertToString(Cursor cursor) {
            CursorThrowWrapper wrapper = new CursorThrowWrapper(cursor);
            return wrapper.getString(Telephony.Sms.Inbox.BODY);
        }
    }

    class MessageViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        AutoRegister.Inbox mInbox;

        @BindView(R.id.icon)
        ImageView icon;
        @BindView(R.id.smallLabel)
        TextView smallLabel;
        @BindView(R.id.primary_text)
        TextView primaryText;
        @BindView(R.id.secondary_text)
        TextView secondaryText;
        @BindView(R.id.message_amount)
        TextView amount;
        @BindView(R.id.message_date)
        TextView date;
        @BindView(R.id.create_transaction)
        ImageView createTransactionButton;
        @BindView(R.id.options_menu)
        ImageView optionsMenu;

        public MessageViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(LOG_TAG, mInbox.toString());
                }
            });
            createTransactionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AutoRegisterUtil.createTransaction(mInbox);
                    Toast.makeText(getContext(),
                            String.format(getString(R.string.toast_autoregister_transaction_created),
                                    mInbox.getMessage().getProvider().getAccountName(),
                                    mInbox.getKeyword().getAccountName(),
                                    amount.getText()),
                            Toast.LENGTH_SHORT).show();
                    refresh();
                }
            });
            optionsMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), v);
                    popup.setOnMenuItemClickListener(MessageViewHolder.this);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.autoregister_message_context_menu, popup.getMenu());
                    popup.show();
                }
            });
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.context_menu_add_keyword:
                    showAddKeywordDialog(mInbox.getMemo());
                    return true;

                case R.id.context_menu_add_transaction:
                    //AutoRegisterUtil.createTransaction(mProvider, mInbox);
                    return true;

                default:
                    return false;
            }
        }

    }
}