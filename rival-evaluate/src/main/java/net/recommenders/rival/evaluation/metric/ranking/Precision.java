package net.recommenders.rival.evaluation.metric.ranking;

import net.recommenders.rival.core.DataModel;
import net.recommenders.rival.evaluation.metric.EvaluationMetric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Precision of a ranked list of items.
 *
 * @author <a href="http://github.com/abellogin">Alejandro</a>.
 */
public class Precision<U, I> extends AbstractRankingMetric<U, I> implements EvaluationMetric<U> {

    /**
     * Precision values per user at each cutoff level
     */
    private Map<Integer, Map<U, Double>> userPrecAtCutoff;

    /**
     * Default constructor with predictions and groundtruth information
     *
     * @param predictions predicted scores for users and items
     * @param test groundtruth information for users and items
     */
    public Precision(DataModel<U, I> predictions, DataModel<U, I> test) {
        this(predictions, test, 1.0);
    }

    /**
     * Constructor where the relevance threshold can be initialized
     *
     * @param predictions predicted ratings
     * @param test groundtruth ratings
     * @param relThreshold relevance threshold
     */
    public Precision(DataModel<U, I> predictions, DataModel<U, I> test, double relThreshold) {
        this(predictions, test, relThreshold, new int[]{});
    }

    /**
     * Constructor where the cutoff levels can be initialized
     *
     * @param predictions predicted ratings
     * @param test groundtruth ratings
     * @param relThreshold relevance threshold
     * @param ats cutoffs
     */
    public Precision(DataModel<U, I> predictions, DataModel<U, I> test, double relThreshold, int[] ats) {
        super(predictions, test, relThreshold, ats);
    }

    /**
     * Computes the global precision by first summing the precision for each
     * user and then averaging by the number of users.
     */
    @Override
    public void compute() {
        if (!Double.isNaN(value)) {
            // since the data cannot change, avoid re-doing the calculations
            return;
        }
        value = 0.0;
        Map<U, List<Double>> data = processDataAsRankedTestRelevance();
        userPrecAtCutoff = new HashMap<Integer, Map<U, Double>>();
        metricPerUser = new HashMap<U, Double>();

        int nUsers = 0;
        for (U user : data.keySet()) {
            List<Double> sortedList = data.get(user);
            double uprec = 0.0;
            int rank = 0;
            for (double rel : sortedList) {
                rank++;
                uprec += computeBinaryPrecision(rel);
                // compute at a particular cutoff
                for (int at : ats) {
                    if (rank == at) {
                        Map<U, Double> m = userPrecAtCutoff.get(at);
                        if (m == null) {
                            m = new HashMap<U, Double>();
                            userPrecAtCutoff.put(at, m);
                        }
                        m.put(user, uprec / rank);
                    }
                }
            }
            // DO NOT assign the precision of the whole list to those cutoffs larger than the list's size
            // instead, we fill with not relevant items until such cutoff
            for (int at : ats) {
                if (rank <= at) {
                    Map<U, Double> m = userPrecAtCutoff.get(at);
                    if (m == null) {
                        m = new HashMap<U, Double>();
                        userPrecAtCutoff.put(at, m);
                    }
                    m.put(user, uprec / at);
                }
            }
            // normalize by list size
            uprec /= rank;
            if (!Double.isNaN(uprec)) {
                value += uprec;
                metricPerUser.put(user, uprec);
                nUsers++;
            }
        }
        value = value / nUsers;
    }

    /**
     * Method to return the precision value at a particular cutoff level.
     *
     * @param at cutoff level
     * @return the precision corresponding to the requested cutoff level
     */
    @Override
    public double getValueAt(int at) {
        if (userPrecAtCutoff.containsKey(at)) {
            int n = 0;
            double prec = 0.0;
            for (U u : userPrecAtCutoff.get(at).keySet()) {
                double uprec = getValueAt(u, at);
                if (!Double.isNaN(uprec)) {
                    prec += uprec;
                    n++;
                }
            }
            prec = (n == 0) ? 0.0 : prec / n;
            return prec;
        }
        return Double.NaN;
    }

    /**
     * Method to return the precision value at a particular cutoff level for a
     * given user.
     *
     * @param user the user
     * @param at cutoff level
     * @return the precision corresponding to the requested user at the cutoff
     * level
     */
    @Override
    public double getValueAt(U user, int at) {
        if (userPrecAtCutoff.containsKey(at) && userPrecAtCutoff.get(at).containsKey(user)) {
            return userPrecAtCutoff.get(at).get(user);
        }
        return Double.NaN;
    }
}
