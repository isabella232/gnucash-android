package org.gnucash.android.ui.autoregister;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientFactory;
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation;
import com.owncloud.android.lib.resources.users.GetRemoteUserNameOperation;

import org.gnucash.android.R;
import org.gnucash.android.app.GnuCashApplication;
import org.gnucash.android.db.DatabaseSchema;
import org.gnucash.android.db.adapter.AccountsDbAdapter;
import org.gnucash.android.db.adapter.AutoRegisterProviderDbAdapter;
import org.gnucash.android.model.AccountType;
import org.gnucash.android.model.AutoRegisterProvider;
import org.gnucash.android.util.AutoRegisterManager;
import org.gnucash.android.util.QualifiedAccountNameCursorAdapter;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment for adding an ownCloud account.
 */
public class AddProviderDialogFragment extends DialogFragment {
    private AutoRegisterProvider mProvider;

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

    private Context mContext;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment AddProviderDialogFragment.
     */
    public static AddProviderDialogFragment newInstance(AutoRegisterProvider provider) {
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
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                return super.getView(position, convertView, parent);
            }
        };
        mProviderSpinner.setAdapter(providerAdapter);

        // Initialize account spinner
        AccountsDbAdapter accountsDbAdapter = AccountsDbAdapter.getInstance();
        AccountType accountType = AccountType.LIABILITY;

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
                String accountUID = accountsDbAdapter.getUID(mTargetAccountSpinner.getSelectedItemId());

                mProvider.setAccountUID(accountUID);
                providerDbAdapter.addRecord(mProvider);

                dismiss();
            }
        });
    }
}
