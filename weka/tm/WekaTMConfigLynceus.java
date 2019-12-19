package weka.tm;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.tm.TMConfig;
import lynceus.tm.TMCostGenerator;
import lynceus.tm.TMLHS;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 21.03.18
 */
public class WekaTMConfigLynceus extends Lynceus<TMConfig, WekaModelSample> {


   public WekaTMConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
      super(h, b, epsilon, gamma, opt, wkldid);
   }

   @Override
   protected CostGenerator<TMConfig> buildCostGenerator(long seed) {
      if (costGenerator == null) {
         costGenerator = new TMCostGenerator();
      }
      return costGenerator;
   }

   @Override
   protected TestSet<TMConfig, WekaModelSample> initialTestSet() {
      return WekaTMConfigFactory.buildInitTestSet("files/tm.arff");
   }

   @Override
   protected PredictiveModel<TMConfig, WekaModelSample> buildPredictiveModel(TrainingSet<TMConfig, WekaModelSample> trainingSet, ModelParams params) {
      return new WekaGaussianProcess<TMConfig>((WekaSet) trainingSet,params);
   }
   
	@Override
	protected PredictiveModel<TMConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<TMConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<TMConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}

   @Override
   protected TrainingSet<TMConfig, WekaModelSample> emptyTrainingSet() {
      final TMConfigWekaTestSet ts = (TMConfigWekaTestSet) this.testSet;
      assert ts != null;
      return new TMConfigWekaTrainingSet(ts.arff());
   }


   @Override
   protected PredictiveModel<TMConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<TMConfig, WekaModelSample> trainingSet) {
      return new HackGaussianProcess<TMConfig>((WekaSet) trainingSet);
   }

   @Override
   protected TrainingSet<TMConfig, WekaModelSample> emptyTrainingSetForApprox() {
      return new TMConfigWekaTrainingSet("files/tm_ei.arff");
   }

   @Override
   protected TestSet<TMConfig, WekaModelSample> fullTestSet() {
      return WekaTMConfigFactory.buildInitTestSet("files/tm.arff");
   }

	@Override
	protected LHS<TMConfig> instantiateLHS(int initTrainSamples) {
		return new TMLHS(initTrainSamples);
	}
}
