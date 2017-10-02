package Evaluation.rival3_test;
import net.recommenders.rival.core.DataModelIF;
import net.recommenders.rival.evaluation.metric.EvaluationMetric;
import net.recommenders.rival.evaluation.metric.error.AbstractErrorMetric;

import java.util.List;
import java.util.Map;

/**
 * Forluma based on
 * Ge, M., Delgado-Battenfeld, C., & Jannach, D. (2010). 
 * Beyond accuracy:Evaluating Recommender Systems by Coverage and Serendipity. 
 * Proceedings of the fourth ACM conference on Recommender systems - RecSys 10. 
 * doi:10.1145/1864708.1864761
 * 
 * (Weighted Predict Coverage) the quality of data sets.
 *
 * @author <a href="https://github.com/zack-liruipeng">Ruipeng Li</a>.
 *
 * @param <U> - type associated to users' id
 * @param <I> - type associated to items' id
 */
public class Coverage<U, I> extends AbstractErrorMetric<U, I> implements EvaluationMetric<U> {

    /**
     * Default constructor with predictions and ground truth information.
     *
     * @param predictions predicted scores for users and items
     * @param test ground truth information for users and items
     */
    public Coverage(final DataModelIF<U, I> predictions, final DataModelIF<U, I> test) {
        super(predictions, test);
    }

    /**
     * Constructor where the error strategy can be initialized.
     *
     * @param predictions predicted scores for users and items
     * @param test ground truth information for users and items
     * @param errorStrategy the error strategy
     */
    public Coverage(final DataModelIF<U, I> predictions, final DataModelIF<U, I> test, final ErrorStrategy errorStrategy) {
        super(predictions, test, errorStrategy);
    }

    @Override
    public void compute() {

        if (!Double.isNaN(getValue())) {
            return;
        }
        iniCompute();

        Map<U, List<Double>> data = processDataAsPredictedDifferencesToTest();
        float coverage = (float) 0.0;
        int Total_Items = 0;
        int Test_Items = data.size();     
        int Ratings  = 0;
        int user_number = 0;
        int Total_Ratings = 0;
        for (U testUser : getTest().getUsers()) {
            int user_Items = 0;
                 
            if (data.containsKey(testUser)) {
                for (Double difference : data.get(testUser)) {
                    user_Items++;
                }
                user_number++;
            }    
            Ratings += user_Items;  
        }        
        Total_Ratings += Ratings;
        Total_Items = user_number * Test_Items;
        
        coverage = (float) ((double)Total_Ratings / (double)Total_Items);
        
        if (Total_Items == 0 || Test_Items == 0) {
            setValue(Double.NaN);
        } else {
            setValue(coverage);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Coverage_" + getStrategy();
    }
}