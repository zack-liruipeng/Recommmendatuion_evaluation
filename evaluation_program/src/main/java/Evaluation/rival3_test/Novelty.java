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
 * (The novelty of) the suprise of data sets.
 *
 * @author <a href="https://github.com/zack-liruipeng">Ruipeng Li</a>.
 *
 * @param <U> - type associated to users' id
 * @param <I> - type associated to items' id
 */

public class Novelty<U,I> extends AbstractErrorMetric<U, I> implements EvaluationMetric<U> {
	 
	public Novelty(DataModelIF<U, I> predictions, DataModelIF<U, I> test) {
		super(predictions, test);
		// TODO Auto-generated constructor stub
	}	

	    /**
	     * Constructor where the error strategy can be initialized.
	     *
	     * @param predictions predicted scores for users and items
	     * @param test ground truth information for users and items
	     * @param errorStrategy the error strategy
	     */
	    public  Novelty(final DataModelIF<U, I> predictions, final DataModelIF<U, I> test, final ErrorStrategy errorStrategy) {
	        super(predictions, test, errorStrategy);
	    }


	    @Override
	    public void compute() {
	    	float novelty =  (float) 0.0;
	    	
	        if (!Double.isNaN(getValue())) {
	            // since the data cannot change, avoid re-doing the calculations
	            return;
	        }	        
	        iniCompute();
        
	        Map<U, List<Double>> data = processDataAsPredictedDifferencesToTest();
	        int testItems = 0;
	        for (U testUser : getTest().getUsers()) {
	            int userItems = 0;
	            float dnovelty = (float) 0.0;
	            float unovelty = (float) 0.0;
	            if (data.containsKey(testUser)) {
	                for (double difference : data.get(testUser)) {
	                	dnovelty +=(float) difference*Math.log(2);
	                    userItems++;
	                }
	            }	            
	            testItems += userItems;
	             
	            if (userItems == 0) {
	            	unovelty = Float.NaN;
	            } else {
	            	unovelty += Math.abs(dnovelty / testItems);
	            } 
	            novelty += unovelty;
	           
	        }	        
	        
	        if (testItems == 0) {
	            setValue(Double.NaN);
	        } else {
	            setValue(novelty);
	        }
	       
	    }

	    /**
	     * {@inheritDoc}
	     */
	    @Override
	    public String toString() {
	        return "Novelty_" + getStrategy();
	    }
}