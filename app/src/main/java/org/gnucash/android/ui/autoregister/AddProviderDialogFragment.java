package org.gnucash.android.ui.autoregister;

import android.app.Activity;
import android.content.Context;
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

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.ui.common.Refreshable;
import org.gnucash.android.util.AutoRegisterManager;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment for adding an ownCloud account.
 */
public class AddProviderDialogFragment extends DialogFragment {
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
        setStyle(DialogFragment.STYLE_NORMAL, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.dialog_autoregister_new_provider, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize provider spinner
        AutoRegisterManager manager = GnuCashApplication.getAutoRegisterManager();
        SpinnerAdapter providerAdapter = new ArrayAdapter<AutoRegisterProvider>(getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                manager.getProviders()) {

            private View setLabel(int position, View v) {
                AutoRegisterProvider p = getItem(position);
                String label = new StringBuilder(p.getDescription()).
                        append(" (").append(p.getPhoneNo()).append(')').toString();

                TextView textView = (TextView) v.findViewById(android.R.id.text1);
                textView.setText(label);

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
        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        AccountType accountType = AccountType.CREDIT;

        String accountConditions = "("
                + DatabaseSchema.AccountEntry.COLUMN_TYPE + " = ? )";
        Cursor cursor = accountsDbAdapter.fetchAccountsOrderedByFullName(accountConditions,
                new String[]{accountType.name()});

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
        final AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        final AutoRegisterProviderDbAdapter providerDbAdapter = AutoRegisterProviderDbAdapter.getInstance();

        mCancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mOkButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AutoRegisterProvider provider = (AutoRegisterProvider) mProviderSpinner.getSelectedItem();
                String accountUID = accountsDbAdapter.getUID(mTargetAccountSpinner.getSelectedItemId());

                provider.setAccountUID(accountUID);
                providerDbAdapter.addRecord(provider);

                ((Refreshable) getTargetFragment()).refresh();
                dismiss();
            }
        });
    }
}
