package weka.giraphPlus;

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
import lynceus.giraph.GiraphPlusConfig;
import lynceus.giraph.GiraphPlusConfigCostGenerator;
import lynceus.giraph.GiraphPlusLHS;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

public class WekaGiraphPlusConfigLynceus extends Lynceus<GiraphPlusConfig, WekaModelSample>{

	/* class attributes */
	private static boolean printed = false;
	private static String datasetFile;
	
	/* class constructor */
	public WekaGiraphPlusConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
		super(h, b, epsilon, gamma, opt, wkldid);
	}

	/* superclass abstract methods to be implemented */
	@Override
	protected CostGenerator<GiraphPlusConfig> buildCostGenerator(long seed) {
		if (costGenerator == null) {
			try {
				costGenerator = new GiraphPlusConfigCostGenerator(datasetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return costGenerator;
	}

	@Override
	protected TestSet<GiraphPlusConfig, WekaModelSample> initialTestSet() {
		if (!printed) {
	         printAll();
	         printed = true;
	      }
		return WekaGiraphPlusConfigFactory.buildInitTestSet("files/giraph_plus.arff");
	}

	@Override
	protected PredictiveModel<GiraphPlusConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<GiraphPlusConfig, WekaModelSample> trainingSet, ModelParams params) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params);
	}
	
	@Override
	protected PredictiveModel<GiraphPlusConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<GiraphPlusConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<GiraphPlusConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}

	@Override
	protected PredictiveModel<GiraphPlusConfig, WekaModelSample> buildPredictiveModelForApprox(
			TrainingSet<GiraphPlusConfig, WekaModelSample> trainingSet) {
		return new HackGaussianProcess<GiraphPlusConfig>((WekaSet) trainingSet);
	}

	@Override
	protected TrainingSet<GiraphPlusConfig, WekaModelSample> emptyTrainingSet() {
		final GiraphPlusConfigWekaTestSet ts = (GiraphPlusConfigWekaTestSet) this.testSet;
	    return new GiraphPlusConfigWekaTrainingSet(ts.arff());
	}

	@Override
	protected TrainingSet<GiraphPlusConfig, WekaModelSample> emptyTrainingSetForApprox() {
		return new GiraphPlusConfigWekaTrainingSet("files/giraph_plus_ei.arff");
	}

	@Override
	protected TestSet<GiraphPlusConfig, WekaModelSample> fullTestSet() {
		return WekaGiraphPlusConfigFactory.buildInitTestSet("files/giraph_plus.arff");
	}

	@Override
	protected LHS<GiraphPlusConfig> instantiateLHS(int initTrainSamples) {
		return new GiraphPlusLHS(initTrainSamples);
	}
	
	/* other methods */
	private TestSet<GiraphPlusConfig, WekaModelSample> printAll() {
        
		TestSet<GiraphPlusConfig, WekaModelSample> testSet = WekaGiraphPlusConfigFactory.buildInitTestSet("files/giraph_plus.arff");
        if (costGenerator == null) {
           throw new RuntimeException("[WekaGiraphPlusConfigLynceus] Cost generator is null");
        }
        System.out.println("[WekaGiraphPlusConfigLynceus] PRE  Total test set size = " + testSet.size());

        Set<Pair<GiraphPlusConfig, Double>> set = new HashSet<Pair<GiraphPlusConfig, Double>>();
        for (int i = 0; i < testSet.size(); i++) {
        	GiraphPlusConfig c = testSet.getConfig(i);
           double runningCost = costGenerator.deploymentCost(null, c);
           set.add(new Pair<GiraphPlusConfig, Double>(c, runningCost));
        }

        for (Pair<GiraphPlusConfig, Double> p : set) {
           testSet.removeConfig(p.getFst());
           testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
           //System.out.println("added " + p.fst + ", " + p.snd);
        }

        System.out.println("[WekaGiraphPlusConfigLynceus] POST Total test set size = " + testSet.size());
        testSet.printAll();
        return testSet;
   }


	public static void setDatasetFile(String file) {
		datasetFile = file;
	}
	
}
