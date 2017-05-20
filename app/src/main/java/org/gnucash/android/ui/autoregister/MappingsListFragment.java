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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.ui.util.widget.EmptyRecyclerView;
import org.gnucash.android.util.AutoRegisterUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 *
 */
public class MappingsListFragment extends Fragment {
    private static final String LOG_TAG = MappingsListFragment.class.getSimpleName();

    private ProviderAdapter mProviderAdapter;

    @BindView(R.id.auto_register_recycler_view) EmptyRecyclerView mRecyclerView;
    @BindView(R.id.empty_view) TextView mEmptyTextView;

    public static MappingsListFragment newInstance() {
        MappingsListFragment fragment = new MappingsListFragment();
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
        AutoRegisterUtil manager = GnuCashApplication.getAutoRegisterManager();
        mProviderAdapter = new ProviderAdapter(manager.getProviders());

        mRecyclerView.setAdapter(mProviderAdapter);
*/
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class ProviderAdapter extends RecyclerView.Adapter<ProviderViewHolder> {
        private List<AutoRegisterProvider> mProviders;

        public ProviderAdapter(List<AutoRegisterProvider> providers) {
            this.mProviders = providers;
        }

        @Override
        public ProviderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.cardview_autoregister_provider, parent, false
            );
            return new ProviderViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ProviderViewHolder holder, int position) {
            AutoRegisterProvider p = mProviders.get(position);

            holder.providerName.setText(p.getName());
/*
            holder.description.setText(p.getPhoneNo());
*/

/*
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onListItemClick(id);
                }
            });
*/
        }

        @Override
        public int getItemCount() {
            return mProviders.size();
        }
    }

    class ProviderViewHolder extends RecyclerView.ViewHolder implements PopupMenu.OnMenuItemClickListener {
        @BindView(R.id.primary_text) TextView providerName;
        @BindView(R.id.secondary_text) TextView description;
        @BindView(R.id.account_balance) TextView accountBalance;
        @BindView(R.id.create_transaction) ImageView createTransaction;
        @BindView(R.id.provider_onoff) SwitchCompat providerOnoff;
        @BindView(R.id.options_menu) ImageView optionsMenu;
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