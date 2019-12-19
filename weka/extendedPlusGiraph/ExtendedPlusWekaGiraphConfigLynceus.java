package weka.extendedPlusGiraph;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.Pair;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.giraph.ExtendedPlusGiraphConfig;
import lynceus.giraph.ExtendedPlusGiraphConfigCostGenerator;
import lynceus.giraph.ExtendedPlusGiraphLHS;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

public class ExtendedPlusWekaGiraphConfigLynceus extends Lynceus<ExtendedPlusGiraphConfig, WekaModelSample> {

	/* class attributes */
	private static boolean printed = false;
	private static String datasetFile;
	
	
	/* class constructors */
	public ExtendedPlusWekaGiraphConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
		super(h, b, epsilon, gamma, opt, wkldid);
		// TODO Auto-generated constructor stub
	}

	/* abstract superclass methods to implement */
	@Override
	protected CostGenerator<ExtendedPlusGiraphConfig> buildCostGenerator(long seed) {
		if (costGenerator == null) {
			try {
				costGenerator = new ExtendedPlusGiraphConfigCostGenerator(datasetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return costGenerator;
	}


	@Override
	protected TestSet<ExtendedPlusGiraphConfig, WekaModelSample> initialTestSet() {
		if (!printed) {
	         printAll();
	         printed = true;
	      }
		return ExtendedPlusWekaGiraphConfigFactory.buildInitTestSet("files/extended_plus_giraph.arff");
	}


	@Override
	protected PredictiveModel<ExtendedPlusGiraphConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<ExtendedPlusGiraphConfig, WekaModelSample> trainingSet, ModelParams params) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params);
	}

	
	@Override
	protected PredictiveModel<ExtendedPlusGiraphConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<ExtendedPlusGiraphConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<ExtendedPlusGiraphConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}

	@Override
	protected PredictiveModel<ExtendedPlusGiraphConfig, WekaModelSample> buildPredictiveModelForApprox(
			TrainingSet<ExtendedPlusGiraphConfig, WekaModelSample> trainingSet) {
		return new HackGaussianProcess<ExtendedPlusGiraphConfig>((WekaSet) trainingSet);
	}


	@Override
	protected TrainingSet<ExtendedPlusGiraphConfig, WekaModelSample> emptyTrainingSet() {
		final ExtendedPlusGiraphConfigWekaTestSet ts = (ExtendedPlusGiraphConfigWekaTestSet) this.testSet;
		return new ExtendedPlusGiraphConfigWekaTrainingSet(ts.arff());
	}


	@Override
	protected TrainingSet<ExtendedPlusGiraphConfig, WekaModelSample> emptyTrainingSetForApprox() {
		return new ExtendedPlusGiraphConfigWekaTrainingSet("files/extended_plus_giraph_ei.arff");

	}


	@Override
	protected TestSet<ExtendedPlusGiraphConfig, WekaModelSample> fullTestSet() {
		return ExtendedPlusWekaGiraphConfigFactory.buildInitTestSet("files/extended_plus_giraph.arff");
	}


	@Override
	protected LHS<ExtendedPlusGiraphConfig> instantiateLHS(int initTrainSamples) {
		return new ExtendedPlusGiraphLHS(initTrainSamples);
	}
	
	/* other methods */
	public static void setDatasetFile(String file) {
		datasetFile = file;
	}

	private TestSet<ExtendedPlusGiraphConfig, WekaModelSample> printAll() {
        
		TestSet<ExtendedPlusGiraphConfig, WekaModelSample> testSet = ExtendedPlusWekaGiraphConfigFactory.buildInitTestSet("files/extended_giraph.arff");
        if (costGenerator == null) {
           throw new RuntimeException("[ExtendedPlusWekaGiraphConfigLynceus] Cost generator is null");
        }
        System.out.println("[ExtendedPlusWekaGiraphConfigLynceus] PRE  Total test set size = " + testSet.size());

        Set<Pair<ExtendedPlusGiraphConfig, Double>> set = new HashSet<Pair<ExtendedPlusGiraphConfig, Double>>();
        for (int i = 0; i < testSet.size(); i++) {
        	ExtendedPlusGiraphConfig c = testSet.getConfig(i);
        	double runningCost = costGenerator.deploymentCost(null, c);
        	set.add(new Pair<ExtendedPlusGiraphConfig, Double>(c, runningCost));
        }

        for (Pair<ExtendedPlusGiraphConfig, Double> p : set) {
           testSet.removeConfig(p.getFst());
           testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
           //System.out.println("added " + p.fst + ", " + p.snd);
        }

        System.out.println("[ExtendedPlusWekaGiraphConfigLynceus] POST Total test set size = " + testSet.size());
        testSet.printAll();
        return testSet;
   }
	
}
