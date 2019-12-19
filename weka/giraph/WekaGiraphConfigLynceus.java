package weka.giraph;

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
import lynceus.giraph.GiraphConfig;
import lynceus.giraph.GiraphConfigCostGenerator;
import lynceus.giraph.GiraphLHS;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

public class WekaGiraphConfigLynceus extends Lynceus<GiraphConfig, WekaModelSample>{

	/* class attributes */
	private static boolean printed = false;
	private static String datasetFile;
	
	
	/* class constructor */
	public WekaGiraphConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
		super(h, b, epsilon, gamma, opt, wkldid);
	}

	
	/* superclass abstract methods to be implemented */
	@Override
	protected CostGenerator<GiraphConfig> buildCostGenerator(long seed) {
		if (costGenerator == null) {
			try {
				costGenerator = new GiraphConfigCostGenerator(datasetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return costGenerator;
	}

	@Override
	protected TestSet<GiraphConfig, WekaModelSample> initialTestSet() {
		if (!printed) {
	         printAll();
	         printed = true;
	      }
	      return WekaGiraphConfigFactory.buildInitTestSet("files/giraph.arff");
	}

	@Override
	protected PredictiveModel<GiraphConfig, WekaModelSample> buildPredictiveModel(TrainingSet<GiraphConfig, WekaModelSample> trainingSet, ModelParams params) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params);
	}
	
	@Override
	protected PredictiveModel<GiraphConfig, WekaModelSample> buildPredictiveModel(TrainingSet<GiraphConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<GiraphConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}
   
	

	@Override
	protected PredictiveModel<GiraphConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<GiraphConfig, WekaModelSample> trainingSet) {
		return new HackGaussianProcess<GiraphConfig>((WekaSet) trainingSet);
	}

	@Override
	protected TrainingSet<GiraphConfig, WekaModelSample> emptyTrainingSet() {
		final GiraphConfigWekaTestSet ts = (GiraphConfigWekaTestSet) this.testSet;
	    return new GiraphConfigWekaTrainingSet(ts.arff());
	}

	@Override
	protected TrainingSet<GiraphConfig, WekaModelSample> emptyTrainingSetForApprox() {
		return new GiraphConfigWekaTrainingSet("files/giraph_ei.arff");
	}

	@Override
	protected TestSet<GiraphConfig, WekaModelSample> fullTestSet() {
		return WekaGiraphConfigFactory.buildInitTestSet("files/giraph.arff");
	}

	
	/* other methods */
	private TestSet<GiraphConfig, WekaModelSample> printAll() {
        
		TestSet<GiraphConfig, WekaModelSample> testSet = WekaGiraphConfigFactory.buildInitTestSet("files/giraph.arff");
        if (costGenerator == null) {
           throw new RuntimeException("[WekaGiraphConfigLynceus] Cost generator is null");
        }
        System.out.println("[WekaGiraphConfigLynceus] PRE  Total test set size = " + testSet.size());

        Set<Pair<GiraphConfig, Double>> set = new HashSet<Pair<GiraphConfig, Double>>();
        for (int i = 0; i < testSet.size(); i++) {
        	GiraphConfig c = testSet.getConfig(i);
           double runningCost = costGenerator.deploymentCost(null, c);
           set.add(new Pair<GiraphConfig, Double>(c, runningCost));
        }

        for (Pair<GiraphConfig, Double> p : set) {
           testSet.removeConfig(p.getFst());
           testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
           //System.out.println("added " + p.fst + ", " + p.snd);
        }

        System.out.println("[WekaGiraphConfigLynceus] POST Total test set size = " + testSet.size());
        testSet.printAll();
        return testSet;
   }


	public static void setDatasetFile(String file) {
		datasetFile = file;
	}


	@Override
	protected LHS<GiraphConfig> instantiateLHS(int initTrainSamples) {
		return new GiraphLHS(initTrainSamples);
	}
	
	
	
}
