package Evaluation.rival3_test;

	import net.recommenders.rival.core.DataModelIF;
	import net.recommenders.rival.core.DataModelUtils;
	import net.recommenders.rival.core.Parser;
	import net.recommenders.rival.core.SimpleParser;
	import net.recommenders.rival.evaluation.metric.ranking.MAP;
	import net.recommenders.rival.evaluation.metric.ranking.NDCG;
	import net.recommenders.rival.evaluation.metric.ranking.Precision;
	import net.recommenders.rival.evaluation.metric.ranking.Recall;
	import net.recommenders.rival.evaluation.strategy.EvaluationStrategy;
	import net.recommenders.rival.examples.DataDownloader;
	import net.recommenders.rival.recommend.frameworks.RecommenderIO;
	import net.recommenders.rival.recommend.frameworks.mahout.GenericRecommenderBuilder;
	import net.recommenders.rival.recommend.frameworks.exceptions.RecommenderException;
	import net.recommenders.rival.split.parser.MovielensParser;
	import net.recommenders.rival.split.splitter.CrossValidationSplitter;
	import org.apache.mahout.cf.taste.common.TasteException;
	import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
	import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
	import org.apache.mahout.cf.taste.recommender.RecommendedItem;
	import org.apache.mahout.cf.taste.recommender.Recommender;

	import java.io.File;
	import java.io.FileNotFoundException;
	import java.io.IOException;
	import java.io.UnsupportedEncodingException;
	import java.lang.reflect.InvocationTargetException;
	import java.util.List;
	import net.recommenders.rival.core.DataModelFactory;
	import net.recommenders.rival.evaluation.metric.error.MAE;
	import net.recommenders.rival.evaluation.metric.error.RMSE;

	/*
	 * This code has been changed from 2015 recommenders.net, the framework base is designed by Alan
	 * and Changed by Ruipeng Li
	 * The licenses is in http://www.apache.org/licenses/LICENSE-2.0
	 */

	public final class MovieTwteetings_SVD {

	    /**
	     * The number of folds.
	     */
	    public static final int N_FOLDS = 5;

	    /**
	     * The cutoff for evaluation metrics, in our experiment, we use 10, 20, 50
	     */
	    public static final int AT = 10;

	    /**
	     * The number of factors, in our experiment, we use 10, 50
	     */
	    public static final int FACTORS = 10;
	    
	    /**
	     * The number of iterations, in our experiment, we use 10, 50
	     */
	    public static final int ITERATIONS = 20;
	    
	    public static final double REL_TH = 3.0;

	    public static final long SEED = 2048L;
	    
	    public static long exe_time=0;
        
	    private MovieTwteetings_SVD(){
	    	
	    }

	    /**
	     * Main method. Parameter is not used.
	     *
	     * @param args the arguments (not used)
	     */
	    public static void main(final String[] args) {
	        String url = "https://github.com/zack-liruipeng/Recommmendatuion_evaluation/blob/master/movietweetings_AUG_7.zip";
	        String folder = "data/MovieTweetings";
	        String modelPath = "data/MovieTweetings/model/";
	        String recPath = "data/MovieTweetings/recommendations/";
	        String dataFile = "data/MovieTweetings/movietweetings_AUG_7/ratings.dat";
	        int nFolds = N_FOLDS;
	        timer time = new timer();
	        prepareSplits(url, nFolds, dataFile, folder, modelPath);
	        time.start();
	        recommend(nFolds, modelPath, recPath);
	        // the strategy files are (currently) being ignored
	        prepareStrategy(nFolds, modelPath, recPath, modelPath);
	        evaluate(nFolds, modelPath, recPath);
	        time.end_time();
	        exe_time = time.get_exe_time() / 1000000000;
	        System.out.println("The total time for algorithm execution is:"  + exe_time + "_seconds");
	    }

	    /**
	     * Downloads a dataset and stores the splits generated from it.
	     *
	     * @param url url where dataset can be downloaded from
	     * @param nFolds number of folds
	     * @param inFile file to be used once the dataset has been downloaded
	     * @param folder folder where dataset will be stored
	     * @param outPath path where the splits will be stored
	     */
	    public static void prepareSplits(final String url, final int nFolds, final String inFile, final String folder, final String outPath) {
	        DataDownloader dd = new DataDownloader(url, folder);
	        dd.downloadAndUnzip();

	        boolean perUser = true;
	        long seed = SEED;
	        Parser<Long, Long> parser = new MovielensParser();

	        DataModelIF<Long, Long> data = null;
	        try {
	            data = parser.parseData(new File(inFile));
	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        DataModelIF<Long, Long>[] splits = new CrossValidationSplitter<Long, Long>(nFolds, perUser, seed).split(data);
	        File dir = new File(outPath);
	        if (!dir.exists()) {
	            if (!dir.mkdir()) {
	                System.err.println("Directory " + dir + " could not be created");
	                return;
	            }
	        }
	        for (int i = 0; i < splits.length / 2; i++) {
	            DataModelIF<Long, Long> training = splits[2 * i];
	            DataModelIF<Long, Long> test = splits[2 * i + 1];
	            String trainingFile = outPath + "train_" + i + ".csv";
	            String testFile = outPath + "test_" + i + ".csv";
	            System.out.println("train: " + trainingFile);
	            System.out.println("test: " + testFile);
	            boolean overwrite = true;
	            try {
	                DataModelUtils.saveDataModel(training, trainingFile, overwrite, "\t");
	                DataModelUtils.saveDataModel(test, testFile, overwrite, "\t");
	            } catch (FileNotFoundException | UnsupportedEncodingException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    /**
	     * Recommends using an UB algorithm.
	     *
	     * @param nFolds number of folds
	     * @param inPath path where training and test models have been stored
	     * @param outPath path where recommendation files will be stored
	     */
	    public static void recommend(final int nFolds, final String inPath, final String outPath) {
	        for (int i = 0; i < nFolds; i++) {
	            org.apache.mahout.cf.taste.model.DataModel trainModel;
	            org.apache.mahout.cf.taste.model.DataModel testModel;
	            try {
	                trainModel = new FileDataModel(new File(inPath + "train_" + i + ".csv"));
	                testModel = new FileDataModel(new File(inPath + "test_" + i + ".csv"));
	            } catch (IOException e) {
	                e.printStackTrace();
	                return;
	            }

	    	    //FunkSVDFactorizer,SVDPlusPlusFactorizer,RatingSGDFactorizer,
	            GenericRecommenderBuilder grb = new GenericRecommenderBuilder();
	            String recommenderClass = "org.apache.mahout.cf.taste.impl.recommender.svd.SVDRecommender";
	            String factirizerClass = "org.apache.mahout.cf.taste.impl.recommender.svd.RatingSGDFactorizer";
	            int factors=FACTORS;
	            int interations=ITERATIONS;

	            Recommender recommender = null;
	            try {
	                recommender = grb.buildRecommender(trainModel, recommenderClass, factirizerClass, interations,factors);
	            } catch (RecommenderException e) {
	                e.printStackTrace();
	            }


	            String fileName = "recs_" + i + ".csv";

	            LongPrimitiveIterator users;
	            try {
	                users = testModel.getUserIDs();
	                boolean createFile = true;
	                while (users.hasNext()) {
	                    long u = users.nextLong();
	                    assert recommender != null;
	                    List<RecommendedItem> items = recommender.recommend(u, trainModel.getNumItems());
	                    RecommenderIO.writeData(u, items, outPath, fileName, !createFile, null);
	                    createFile = false;
	                }
	            } catch (TasteException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    /**
	     * Prepares the strategies to be evaluated with the recommenders already
	     * generated.
	     *
	     * @param nFolds number of folds
	     * @param splitPath path where splits have been stored
	     * @param recPath path where recommendation files have been stored
	     * @param outPath path where the filtered recommendations will be stored
	     */
	    @SuppressWarnings("unchecked")
	    public static void prepareStrategy(final int nFolds, final String splitPath, final String recPath, final String outPath) {
	        for (int i = 0; i < nFolds; i++) {
	            File trainingFile = new File(splitPath + "train_" + i + ".csv");
	            File testFile = new File(splitPath + "test_" + i + ".csv");
	            File recFile = new File(recPath + "recs_" + i + ".csv");
	            DataModelIF<Long, Long> trainingModel;
	            DataModelIF<Long, Long> testModel;
	            DataModelIF<Long, Long> recModel;
	            try {
	                trainingModel = new SimpleParser().parseData(trainingFile);
	                testModel = new SimpleParser().parseData(testFile);
	                recModel = new SimpleParser().parseData(recFile);
	            } catch (IOException e) {
	                e.printStackTrace();
	                return;
	            }

	            Double threshold = REL_TH;
	            String strategyClassName = "net.recommenders.rival.evaluation.strategy.UserTest";
	            EvaluationStrategy<Long, Long> strategy = null;
	            try {
	                strategy = (EvaluationStrategy<Long, Long>) (Class.forName(strategyClassName)).getConstructor(DataModelIF.class, DataModelIF.class, double.class).
	                        newInstance(trainingModel, testModel, threshold);
	            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException | InvocationTargetException e) {
	                e.printStackTrace();
	            }

	            DataModelIF<Long, Long> modelToEval = DataModelFactory.getDefaultModel();
	            for (Long user : recModel.getUsers()) {
	                assert strategy != null;
	                for (Long item : strategy.getCandidateItemsToRank(user)) {
	                    if (recModel.getUserItemPreferences().get(user).containsKey(item)) {
	                        modelToEval.addPreference(user, item, recModel.getUserItemPreferences().get(user).get(item));
	                    }
	                }
	            }
	            try {
	                DataModelUtils.saveDataModel(modelToEval, outPath + "strategymodel_" + i + ".csv", true, "\t");
	            } catch (FileNotFoundException | UnsupportedEncodingException e) {
	                e.printStackTrace();
	            }
	        }
	    }

	    /**
	     * Evaluates the recommendations generated in previous steps.
	     *
	     * @param nFolds number of folds
	     * @param splitPath path where splits have been stored
	     * @param recPath path where recommendation files have been stored
	     */
	    public static void evaluate(final int nFolds, final String splitPath, final String recPath) {
	    	double maeRes = 0.0;
	    	double mseRes;
	        double rmseRes = 0.0; 
	        
	        double precisionRes = 0.0;
	        double recallRes = 0.0;
	        
	        double mapRes= 0.0;
	        double ndcgRes = 0.0;
	        
	        double coverageRes =0.0;
	        double noveltyRes =0.0;
	        int count=0;
	        
	        for (int i = 0; i < nFolds; i++) {
	            File testFile = new File(splitPath + "test_" + i + ".csv");
	            File recFile = new File(recPath + "recs_" + i + ".csv");
	            DataModelIF<Long, Long> testModel = null;
	            DataModelIF<Long, Long> recModel = null;
	            try {
	                testModel = new SimpleParser().parseData(testFile);
	                recModel = new SimpleParser().parseData(recFile);
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	            
	            MAE<Long, Long> mae = new MAE<Long, Long>(recModel, testModel);
	            mae.compute();
	            maeRes += mae.getValue();
	            
	            NDCG<Long, Long> ndcg = new NDCG<>(recModel, testModel, new int[]{AT});
	            ndcg.compute();
	            ndcgRes += ndcg.getValueAt(AT);

	            RMSE<Long, Long> rmse = new RMSE<>(recModel, testModel);
	            rmse.compute();
	            rmseRes += rmse.getValue();
	            
	            MAP<Long, Long> map =new MAP<>(recModel, testModel);
	            map.compute();
	            mapRes += map.getValue();
	            
	            Precision<Long, Long> precision = new Precision<>(recModel, testModel, REL_TH, new int[]{AT});
	            precision.compute();
	            precisionRes += precision.getValueAt(AT);
	            
	            Recall<Long, Long> recall = new Recall<>(recModel, testModel, REL_TH, new int[]{AT});
	            recall.compute();
	            recallRes += recall.getValueAt(AT);
	            
	            Coverage<Long, Long> coverage = new Coverage<>(recModel, testModel);
	            coverage.compute();
	            coverageRes += coverage.getValue();       	            
	            
	            Novelty<Long, Long> novelty = new Novelty<>(recModel, testModel);
	            novelty.compute();
	            if (!Double.isNaN(novelty.getValue())){
	            	noveltyRes += novelty.getValue();
	            	count++;
	            };
	        }
	        
	        mseRes= Math.pow((rmseRes/nFolds), 2);
	        
	        System.out.println("MAE:" + maeRes/nFolds);
	        System.out.println("MSE: " + mseRes);
	        System.out.println("RMSE: " + rmseRes / nFolds);
	        System.out.println("P@" + AT + ": " + precisionRes / nFolds);
	        System.out.println("R@" + AT + ": " + recallRes / nFolds);
	        System.out.println("NDCG@" + AT + ": " + ndcgRes / nFolds);
	        System.out.println("MAP:" + mapRes / nFolds);
	        System.out.println("Coverage:" + coverageRes / nFolds);
	        System.out.println("Novelty:" + noveltyRes / count);
	    }
	}
