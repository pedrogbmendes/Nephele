package weka.tensorflow;

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
import lynceus.tensorflow.TensorflowConfigCostGeneratorSize;
import lynceus.tensorflow.TensorflowConfigSize;
import lynceus.tensorflow.TensorflowLHSSize;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

public class WekaTensorflowConfigLynceusSize extends Lynceus<TensorflowConfigSize, WekaModelSample>{

	private static boolean printed = false;
	private static String datasetFile;
	
	/* class constructor */
	public WekaTensorflowConfigLynceusSize(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
		super(h, b, epsilon, gamma, opt, wkldid);
	}

	/* superclass abstract methods to be implemented */
	@Override
	protected CostGenerator<TensorflowConfigSize> buildCostGenerator(long seed) {
		if (costGenerator == null) {
			try {
				costGenerator = new TensorflowConfigCostGeneratorSize(datasetFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return costGenerator;
	}

	@Override
	protected TestSet<TensorflowConfigSize, WekaModelSample> initialTestSet() {
		if (!printed) {
	         printAll();
	         printed = true;
	      }
	      return WekaTensorflowConfigFactorySize.buildInitTestSet("files/tensorflowsize.arff");
	}

	@Override
	protected PredictiveModel<TensorflowConfigSize, WekaModelSample> buildPredictiveModel(TrainingSet<TensorflowConfigSize, WekaModelSample> trainingSet, ModelParams params) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params);
	}

	@Override
	protected PredictiveModel<TensorflowConfigSize, WekaModelSample> buildPredictiveModel(TrainingSet<TensorflowConfigSize, WekaModelSample> trainingSet, ModelParams params,TestSet<TensorflowConfigSize, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}
	
	@Override
	protected PredictiveModel<TensorflowConfigSize, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<TensorflowConfigSize, WekaModelSample> trainingSet) {
		return new HackGaussianProcess<TensorflowConfigSize>((WekaSet) trainingSet);
		//throw new RuntimeException("[WekaConfigLynceus] buildPredictiveModelForApprox not supported yet");
	}

	@Override
	protected TrainingSet<TensorflowConfigSize, WekaModelSample> emptyTrainingSet() {
		final TensorflowConfigWekaTestSetSize ts = (TensorflowConfigWekaTestSetSize) this.testSet;
	    return new TensorflowConfigWekaTrainingSetSize(ts.arff());
	}

	@Override
	protected TrainingSet<TensorflowConfigSize, WekaModelSample> emptyTrainingSetForApprox() {
		//throw new RuntimeException("[WekaConfigLynceus] emptyTrainingSetForApprox not supported yet");
		return new TensorflowConfigWekaTrainingSetSize("files/tensorflow_ei.arff");
	}

	@Override
	protected TestSet<TensorflowConfigSize, WekaModelSample> fullTestSet() {
		return WekaTensorflowConfigFactorySize.buildInitTestSet("files/tensorflowsize.arff");
	}

	/* other methods */
	private TestSet<TensorflowConfigSize, WekaModelSample> printAll() {
        
		TestSet<TensorflowConfigSize, WekaModelSample> testSet = WekaTensorflowConfigFactorySize.buildInitTestSet("files/tensorflowsize.arff");
        if (costGenerator == null) {
           throw new RuntimeException("[WekaTensorflowConfigLynceusSize] Cost generator is null");
        }
        System.out.println("[WekaTensorflowConfigLynceusSize] PRE  Total test set size = " + testSet.size());

        Set<Pair<TensorflowConfigSize, Double>> set = new HashSet<Pair<TensorflowConfigSize, Double>>();
        for (int i = 0; i < testSet.size(); i++) {
           TensorflowConfigSize c = testSet.getConfig(i);
           double runningCost = costGenerator.deploymentCost(null, c);
           set.add(new Pair<TensorflowConfigSize, Double>(c, runningCost));
        }

        for (Pair<TensorflowConfigSize, Double> p : set) {
           testSet.removeConfig(p.getFst());
           testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
           //System.out.println("added " + p.fst + ", " + p.snd);
        }

        System.out.println("[WekaTensorflowConfigLynceusSize] POST Total test set size = " + testSet.size());
        testSet.printAll();
        return testSet;
   }
	
	public static void setDatasetFile(String file) {
		datasetFile = file;
	}

	@Override
	protected LHS<TensorflowConfigSize> instantiateLHS(int initTrainSamples) {
		return new TensorflowLHSSize(initTrainSamples);
	}

	

}
