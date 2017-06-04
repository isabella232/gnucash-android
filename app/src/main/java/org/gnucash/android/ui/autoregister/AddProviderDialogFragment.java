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

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.importer.AutoRegisterInboxImporter;
import org.gnucash.android.importer.AutoRegisterProviderImporter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment for adding an ownCloud account.
 */
public class AddProviderDialogFragment extends DialogFragment {

    private AutoRegisterProviderDbAdapter mProviderDbAdapter;
    private AccountsDbAdapter mAccountsDbAdapter;

    /**
     * Dialog positive button. Ok to save and validate the data
     */
    @BindView(R.id.btn_save) Button mOkButton;

    /**
     * Cancel button
     */
    @BindView(R.id.btn_cancel) Button mCancelButton;

    @BindView(R.id.source_provider_spinner) Spinner mProviderSpinner;
    @BindView(R.id.target_accounts_spinner) Spinner mTargetAccountSpinner;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment AddProviderDialogFragment.
     */
    public static AddProviderDialogFragment newInstance() {
        AddProviderDialogFragment fragment = new AddProviderDialogFragment();
        return fragment;
    }

    public AddProviderDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);

        mProviderDbAdapter = AutoRegisterProviderDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.title_autoregister_new_provider);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.dialog_autoregister_new_provider, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize provider spinner
        SpinnerAdapter providerAdapter = new ArrayAdapter<AutoRegister.Provider>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                mProviderDbAdapter.getDisabledProviders()) {

            private View setLabel(int position, View v) {
                AutoRegister.Provider p = getItem(position);

                TextView textView = (TextView) v.findViewById(android.R.id.text1);
                textView.setText(p.getName());

                return v;
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                return setLabel(position, v);
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                return setLabel(position, v);
            }
        };

        mProviderSpinner.setAdapter(providerAdapter);

        // Initialize account spinner
        String accountConditions = DatabaseSchema.AccountEntry.COLUMN_TYPE + " = ? "
                + "AND " + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = ?";
        Cursor cursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(
                accountConditions,
                new String[] { AccountType.CREDIT.name(), "0" });
        mTargetAccountSpinner.setAdapter(new QualifiedAccountNameCursorAdapter(getActivity(), cursor));

        setListeners();

        if (cursor.getCount() == 0) {
            mTargetAccountSpinner.setEnabled(false);
        }
    }

    /**
     * Binds click listeners for the dialog buttons
     */
    private void setListeners(){
        mCancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AutoRegister.Provider provider = addNewProvider();

                String toastMessage = String.format(
                        getString(R.string.toast_new_autoregister_provider_added),
                        provider.getName());
                Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();

                ((Refreshable) getTargetFragment()).refresh();
                dismiss();
            }
        });
    }

    /**
     * Add new provider and imports SMS messages from this new provider.
     *
     * @return
     */
    private AutoRegister.Provider addNewProvider() {
        AutoRegister.Provider provider = (AutoRegister.Provider) mProviderSpinner.getSelectedItem();
        String accountUID = mAccountsDbAdapter.getUID(mTargetAccountSpinner.getSelectedItemId());

        mProviderDbAdapter.setActive(provider.getUID(), accountUID);
        AutoRegisterInboxImporter importer = new AutoRegisterInboxImporter(
                getContext(), GnuCashApplication.getAutoRegisterInboxDbAdapter());
        importer.execute(provider);

        return provider;
    }
}
