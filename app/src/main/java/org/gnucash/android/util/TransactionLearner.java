package org.gnucash.android.util;

import org.gnucash.android.db.adapter.TransactionsDbAdapter;
import org.gnucash.android.model.Transaction;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

/**
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 25.
 */

public class TransactionLearner {
    private Instances instances;
    private Filter normalizer = new Normalize();
    private NaiveBayes naiveBayes = new NaiveBayes();

    public TransactionLearner() {
        normalizer.setDoNotCheckCapabilities(true);
        naiveBayes.setDoNotCheckCapabilities(true);
    }

}

class TransactionDataLoader {
    private static final ArrayList<Attribute> ATTRIBUTES = new ArrayList<>();
    TransactionsDbAdapter mTransactionsDbAdapter = TransactionsDbAdapter.getInstance();

    static {
        ATTRIBUTES.add(new Attribute("is_weekday"));
        List<String> hourTypes = new ArrayList<>();
        for (TransactionData.hourType ht : TransactionData.hourType.values()) {
            hourTypes.add(ht.name());
        }
        ATTRIBUTES.add(new Attribute("hour_type", hourTypes));
        ATTRIBUTES.add(new Attribute("amount_log"));
    }

    Instances loadTransactionsFromAccount(String accountUID) {
        List<Transaction> transactions = mTransactionsDbAdapter.getAllTransactionsForAccount(accountUID);
        Instances instances = new Instances("transactions", ATTRIBUTES, transactions.size());

        for (Transaction t : transactions) {
            long timeMillis = t.getTimeMillis();
            int dayOfWeek = new LocalDate(timeMillis).getDayOfWeek();

            Instance i = new DenseInstance(3);
            i.setDataset(instances);
/*
            i.setValue(0, dayOfWeek != DateTimeConstants.SATURDAY);
            LocalDate
            instances.add()
*/
        }

        return instances;
    }
}

class TransactionData {
    boolean isWeekday;
    enum hourType {
        HOUR_0TO3, HOUR_3TO6, HOUR_6TO9, HOUR_9TO12,
        HOUR_12TO15, HOUR_15T18, HOUR_18TO21, HOUR_21TO24,
    }
    double amountLog;
}
