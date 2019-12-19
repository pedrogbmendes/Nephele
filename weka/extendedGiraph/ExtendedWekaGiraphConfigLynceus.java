package weka.extendedGiraph;

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
import lynceus.giraph.ExtendedGiraphConfig;
import lynceus.giraph.ExtendedGiraphConfigCostGenerator;
import lynceus.giraph.ExtendedGiraphLHS;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

public class ExtendedWekaGiraphConfigLynceus extends Lynceus<ExtendedGiraphConfig, WekaModelSample> {

	/* class attributes */
	private static boolean printed = false;
	private static String datasetFile;
	
	
	/* class constructors */
	public ExtendedWekaGiraphConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
		super(h, b, epsilon, gamma, opt, wkldid);
		// TODO Auto-generated constructor stub
	}
	
	/* abstract superclass methods to implement */
	@Override
	protected CostGenerator<ExtendedGiraphConfig> buildCostGenerator(long seed) {
		if (costGenerator == null) {
			try {
				costGenerator = new ExtendedGiraphConfigCostGenerator(datasetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return costGenerator;
	}

	@Override
	protected TestSet<ExtendedGiraphConfig, WekaModelSample> initialTestSet() {
		if (!printed) {
	         printAll();
	         printed = true;
	      }
		return ExtendedWekaGiraphConfigFactory.buildInitTestSet("files/extended_giraph.arff");
	}

	@Override
	protected PredictiveModel<ExtendedGiraphConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<ExtendedGiraphConfig, WekaModelSample> trainingSet, ModelParams params) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params);
	}

	@Override
	protected PredictiveModel<ExtendedGiraphConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<ExtendedGiraphConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<ExtendedGiraphConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}
	
	@Override
	protected PredictiveModel<ExtendedGiraphConfig, WekaModelSample> buildPredictiveModelForApprox(
			TrainingSet<ExtendedGiraphConfig, WekaModelSample> trainingSet) {
		return new HackGaussianProcess<ExtendedGiraphConfig>((WekaSet) trainingSet);
	}

	@Override
	protected TrainingSet<ExtendedGiraphConfig, WekaModelSample> emptyTrainingSet() {
		final ExtendedGiraphConfigWekaTestSet ts = (ExtendedGiraphConfigWekaTestSet) this.testSet;
		return new ExtendedGiraphConfigWekaTrainingSet(ts.arff());
	}

	@Override
	protected TrainingSet<ExtendedGiraphConfig, WekaModelSample> emptyTrainingSetForApprox() {
		return new ExtendedGiraphConfigWekaTrainingSet("files/extended_giraph_ei.arff");
	}

	@Override
	protected TestSet<ExtendedGiraphConfig, WekaModelSample> fullTestSet() {
		return ExtendedWekaGiraphConfigFactory.buildInitTestSet("files/extended_giraph.arff");
	}

	@Override
	protected LHS<ExtendedGiraphConfig> instantiateLHS(int initTrainSamples) {
		return new ExtendedGiraphLHS(initTrainSamples);
	}
	
	/* other methods */
	public static void setDatasetFile(String file) {
		datasetFile = file;
	}

	private TestSet<ExtendedGiraphConfig, WekaModelSample> printAll() {
        
		TestSet<ExtendedGiraphConfig, WekaModelSample> testSet = ExtendedWekaGiraphConfigFactory.buildInitTestSet("files/extended_giraph.arff");
        if (costGenerator == null) {
           throw new RuntimeException("[ExtendedWekaGiraphConfigLynceus] Cost generator is null");
        }
        System.out.println("[ExtendedWekaGiraphConfigLynceus] PRE  Total test set size = " + testSet.size());

        Set<Pair<ExtendedGiraphConfig, Double>> set = new HashSet<Pair<ExtendedGiraphConfig, Double>>();
        for (int i = 0; i < testSet.size(); i++) {
        	ExtendedGiraphConfig c = testSet.getConfig(i);
        	double runningCost = costGenerator.deploymentCost(null, c);
        	set.add(new Pair<ExtendedGiraphConfig, Double>(c, runningCost));
        }

        for (Pair<ExtendedGiraphConfig, Double> p : set) {
           testSet.removeConfig(p.getFst());
           testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
           //System.out.println("added " + p.fst + ", " + p.snd);
        }

        System.out.println("[ExtendedWekaGiraphConfigLynceus] POST Total test set size = " + testSet.size());
        testSet.printAll();
        return testSet;
   }
	
}
