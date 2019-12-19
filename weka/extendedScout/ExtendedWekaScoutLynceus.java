package weka.extendedScout;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.scout.ExtendedScoutLHS;
import lynceus.scout.ExtendedScoutVMConfig;
import lynceus.scout.ExtendedScoutVMCostGenerator;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class ExtendedWekaScoutLynceus extends Lynceus<ExtendedScoutVMConfig, WekaModelSample> {
   public ExtendedWekaScoutLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
      super(h, b, epsilon, gamma, opt, wkldid);
   }

   @Override
   protected CostGenerator<ExtendedScoutVMConfig> buildCostGenerator(long seed) {
      if (costGenerator == null) {
         costGenerator = new ExtendedScoutVMCostGenerator();
      }
      return costGenerator;
   }

   @Override
   protected TestSet<ExtendedScoutVMConfig, WekaModelSample> initialTestSet() {
      return ExtendedWekaScoutVMConfigFactory.buildInitTestSet("files/extended_scout.arff");
   }

   @Override
   protected PredictiveModel<ExtendedScoutVMConfig, WekaModelSample> buildPredictiveModel(TrainingSet<ExtendedScoutVMConfig, WekaModelSample> trainingSet, ModelParams params) {
      return new WekaGaussianProcess<ExtendedScoutVMConfig>((WekaSet) trainingSet,params);
   }

	@Override
	protected PredictiveModel<ExtendedScoutVMConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<ExtendedScoutVMConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<ExtendedScoutVMConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}
   
   @Override
   protected TrainingSet<ExtendedScoutVMConfig, WekaModelSample> emptyTrainingSet() {
      final ExtendedScoutVMConfigWekaTestSet ts = (ExtendedScoutVMConfigWekaTestSet) this.testSet;
      assert ts != null;
      return new ExtendedScoutVMConfigWekaTrainingSet(ts.arff());
   }


   @Override
   protected PredictiveModel<ExtendedScoutVMConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<ExtendedScoutVMConfig, WekaModelSample> trainingSet) {
      return new HackGaussianProcess<ExtendedScoutVMConfig>((WekaSet) trainingSet);
   }

   @Override
   protected TrainingSet<ExtendedScoutVMConfig, WekaModelSample> emptyTrainingSetForApprox() {
      if(true)throw  new RuntimeException("NO");
      return new ExtendedScoutVMConfigWekaTrainingSet("files/scout_utility.arff");
   }

   @Override
   protected TestSet<ExtendedScoutVMConfig, WekaModelSample> fullTestSet() {
      return ExtendedWekaScoutVMConfigFactory.buildInitTestSet("files/extended_scout.arff");
   }

	@Override
	protected LHS<ExtendedScoutVMConfig> instantiateLHS(int initTrainSamples) {
		return new ExtendedScoutLHS(initTrainSamples);
	}

  

}
