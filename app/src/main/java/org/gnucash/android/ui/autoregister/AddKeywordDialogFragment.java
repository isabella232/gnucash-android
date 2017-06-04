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
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.gnucash.android.R;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterKeywordDbAdapter;
import org.gnucash.android.db.adapter.DatabaseAdapter;
import org.gnucash.android.model.Account;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.AutoRegister;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.ui.common.UxArgument;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment for adding an ownCloud account.
 */
public class AddKeywordDialogFragment extends DialogFragment {

    private AutoRegisterKeywordDbAdapter mMappingDbAdapter;
    private AccountsDbAdapter mAccountsDbAdapter;

    /**
     * Dialog positive button. Ok to save and validate the data
     */
    @BindView(R.id.btn_save) Button mOkButton;

    /**
     * Cancel button
     */
    @BindView(R.id.btn_cancel) Button mCancelButton;

    @BindView(R.id.keyword) TextView mKeywordText;
    @BindView(R.id.target_accounts_spinner) Spinner mTargetAccountSpinner;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment AddProviderDialogFragment.
     */
    public static AddKeywordDialogFragment newInstance() {
        AddKeywordDialogFragment fragment = new AddKeywordDialogFragment();
        return fragment;
    }

    public AddKeywordDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomDialog);

        mMappingDbAdapter = AutoRegisterKeywordDbAdapter.getInstance();
        mAccountsDbAdapter = AccountsDbAdapter.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.title_autoregister_new_keyword);

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.dialog_autoregister_new_keyword, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getArguments() != null)
            mKeywordText.setText(getArguments().getString(UxArgument.AUTOREGISTER_KEYWORD));

        String accountConditions = DatabaseSchema.AccountEntry.COLUMN_TYPE + " = ? "
                + "AND " + DatabaseSchema.AccountEntry.COLUMN_PLACEHOLDER + " = ?";
        Cursor cursor = mAccountsDbAdapter.fetchAccountsOrderedByFullName(
                accountConditions,
                new String[] { AccountType.EXPENSE.name(), "0" });

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
                String keyword = mKeywordText.getText().toString();
                int priority = mMappingDbAdapter.fetchAllRecords().getCount() + 1;
                Account account = mAccountsDbAdapter.getRecord(mTargetAccountSpinner.getSelectedItemId());

                mMappingDbAdapter.addRecord(new AutoRegister.Keyword(keyword, priority, account.getUID()),
                        DatabaseAdapter.UpdateMethod.replace);

                String toastMessage = String.format(
                        getString(R.string.toast_new_autoregister_keyword_added),
                        keyword, account.getFullName());
                Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();

                MessageActivity.launchWithKeyword(getContext(), keyword);
                dismiss();
            }
        });
    }
}
