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
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseCursorLoader;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterKeywordDbAdapter;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.ui.util.CursorRecyclerDraggableAdapter;
import org.gnucash.android.ui.util.FloatingActionButtonManager;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 *
 */
public class KeywordsListFragment extends Fragment implements
        Refreshable, LoaderManager.LoaderCallbacks<Cursor> {
    private static final String LOG_TAG = KeywordsListFragment.class.getSimpleName();

    private enum WorkingMode {
        NORMAL, REORDER
    }
    private WorkingMode mMode = WorkingMode.NORMAL;

    private AutoRegisterKeywordDbAdapter mKeywordDbAdapter;
    private AccountsDbAdapter mAccountsDbAdapter;

    private KeywordRecyclerAdapter mKeywordRecyclerAdapter;

    @BindView(R.id.autoregister_vendor_recycler_view) EmptyRecyclerView mRecyclerView;
    @BindView(R.id.empty_view) TextView mEmptyTextView;

    public static KeywordsListFragment newInstance() {
        KeywordsListFragment fragment = new KeywordsListFragment();

        fragment.mKeywordDbAdapter = AutoRegisterKeywordDbAdapter.getInstance();
        fragment.mAccountsDbAdapter = AccountsDbAdapter.getInstance();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_autoregister_keywords_list, container, false);

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

        mKeywordRecyclerAdapter = new KeywordRecyclerAdapter(null, mRecyclerView);
        mRecyclerView.setAdapter(mKeywordRecyclerAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
        setWorkingMode(WorkingMode.NORMAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mKeywordRecyclerAdapter != null)
            mKeywordRecyclerAdapter.swapCursor(null);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(LOG_TAG, "Creating the providers loader");

        return new DatabaseCursorLoader(getContext()) {
            @Override
            public Cursor loadInBackground() {
                Cursor cursor = mKeywordDbAdapter.fetchAllRecords();

                if (cursor != null)
                    registerContentObserver(cursor);
                return cursor;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(LOG_TAG, "Mappings loader finished. Swapping in cursor");
        mKeywordRecyclerAdapter.swapCursor(data);
        mKeywordRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(LOG_TAG, "Resetting the Mappings loader");
        mKeywordRecyclerAdapter.swapCursor(null);
    }

    @Override
    public void refresh() {
        Log.d(LOG_TAG, "refresh()");
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public void refresh(String uid) {

    }

    private void launchMessageActivity(String vendorName) {
        Log.d(LOG_TAG, "launchMessageActivity(): vendorName = " + vendorName);

        Intent i = new Intent(getContext(), MessageActivity.class);
        i.putExtra(UxArgument.AUTOREGISTER_KEYWORD, vendorName);

        startActivity(i);
    }

    private void setWorkingMode(WorkingMode mode) {
        FloatingActionButtonManager fabManager = ((AutoRegisterActivity) getActivity()).getFloatingActionButtomManager();
        FloatingActionButton fab = fabManager.getFloatingActionButton();

        final Animation zoomOut = AnimationUtils.loadAnimation(getContext(), R.anim.zoom_out);
        final Animation zoomIn = AnimationUtils.loadAnimation(getContext(), R.anim.zoom_in);

        fab.clearAnimation();
        fab.startAnimation(zoomOut);

        switch (mode) {
            case NORMAL:
                fabManager.revertToInitialState();
                break;
            case REORDER:
                fab.setImageResource(R.drawable.ic_done_all_white_48dp);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mKeywordRecyclerAdapter.onEndDrag();
                        setWorkingMode(WorkingMode.NORMAL);
                    }
                });
                break;
        }

        fab.clearAnimation();
        fab.startAnimation(zoomIn);
    }

    private class KeywordRecyclerAdapter extends CursorRecyclerDraggableAdapter<KeywordViewHolder> {
        public KeywordRecyclerAdapter(Cursor cursor, RecyclerView recyclerView) {
            super(cursor, recyclerView);
        }

        @Override
        public KeywordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_autoregister_keyword, parent, false
            );
            return new KeywordViewHolder(v);
        }

        @Override
        public void onBindViewHolderCursor(final KeywordViewHolder holder, final Cursor cursor) {
            Log.d(LOG_TAG, "onBindViewHolderCursor()");
            final AutoRegister.Keyword keyword = mKeywordDbAdapter.buildModelInstance(cursor);

            holder.keyword = keyword;
            holder.iconText.setText("#" + (holder.getLayoutPosition() + 1));

            holder.primaryText.setText(keyword.getKeyword());
            holder.secondaryText.setText(mAccountsDbAdapter.getFullyQualifiedAccountName(keyword.getAccountUID()));

            holder.reorderButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (MotionEventCompat.getActionMasked(motionEvent) == MotionEvent.ACTION_DOWN) {
                        onStartDrag(holder);
                    }
                    return false;
                }
            });
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchMessageActivity(keyword.getKeyword());
                }
            });
        }

        @Override
        public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
            super.onStartDrag(viewHolder);
            setWorkingMode(WorkingMode.REORDER);
        }

        @Override
        public void onEndDrag() {
            Log.d(LOG_TAG, "onEndDrag()");
            List<Pair<Long, Integer>> updates = new ArrayList<>();
            for (UpdatedItem item : mKeywordRecyclerAdapter.getUpdatedItems()) {
                updates.add(new Pair(item.itemId, item.newPosition));
            }
            mKeywordDbAdapter.updatePriorities(updates);
        }
    }

    private void showDeleteConfirmationDialog(final String keywordUID) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_confirm_delete)
                .setMessage(R.string.msg_disable_keyword_confirm)
                .setIcon(android.R.drawable.ic_delete)
                .setPositiveButton(R.string.alert_dialog_ok_delete,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                mKeywordDbAdapter.deleteRecord(keywordUID);
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

    class KeywordViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        AutoRegister.Keyword keyword;
        @BindView(R.id.icon) TextView iconText;
        @BindView(R.id.primary_text) TextView primaryText;
        @BindView(R.id.secondary_text) TextView secondaryText;
        @BindView(R.id.reorder_btn) ImageView reorderButton;
        @BindView(R.id.options_menu) ImageView optionsMenu;

        public KeywordViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            optionsMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(getActivity(), v);
                    popup.setOnMenuItemClickListener(KeywordViewHolder.this);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.autoregister_keyword_context_menu, popup.getMenu());
                    popup.show();
                }
            });

        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()){
                case R.id.context_menu_delete:
                    showDeleteConfirmationDialog(keyword.getUID());
                    return true;

                default:
                    return false;
            }
        }
    }
}