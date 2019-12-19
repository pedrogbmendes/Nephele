package weka.reducedScout;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.scout.ReducedScoutLHS;
import lynceus.scout.ReducedScoutVMConfig;
import lynceus.scout.ReducedScoutVMCostGenerator;
import weka.HackGaussianProcess;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.tuning.ModelParams;

public class ReducedWekaScoutLynceus extends Lynceus<ReducedScoutVMConfig, WekaModelSample> {

   /* class constructors */
   public ReducedWekaScoutLynceus(long h, double b, double epsilon, double gamma, lynceus.Lynceus.optimizer opt, long wkldid) {
      super(h, b, epsilon, gamma, opt, wkldid);
   }


   /* abstract methods to be implemented */
   @Override
   protected CostGenerator<ReducedScoutVMConfig> buildCostGenerator(long seed) {
      if (costGenerator == null) {
         costGenerator = new ReducedScoutVMCostGenerator();
      }
      return costGenerator;
   }

   @Override
   protected TestSet<ReducedScoutVMConfig, WekaModelSample> initialTestSet() {
      if (WekaGaussianProcess.useNominalAttributes) {
         System.out.println("Nominal attributes") ;
         return ReducedWekaScoutVMConfigFactory.buildInitTestSet("files/reduced_scout.arff");
      }
      System.out.println("Binary attributes") ;
      return ReducedWekaScoutVMConfigFactory.buildInitTestSet("files/reduced_scout_gp.arff");
   }

   @Override
   protected PredictiveModel<ReducedScoutVMConfig, WekaModelSample> buildPredictiveModel(
         TrainingSet<ReducedScoutVMConfig, WekaModelSample> trainingSet, ModelParams params) {
      return new WekaGaussianProcess<ReducedScoutVMConfig>((WekaSet) trainingSet,params);
   }

	@Override
	protected PredictiveModel<ReducedScoutVMConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<ReducedScoutVMConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<ReducedScoutVMConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}
   
   @Override
   protected PredictiveModel<ReducedScoutVMConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<ReducedScoutVMConfig, WekaModelSample> trainingSet) {
      return new HackGaussianProcess<ReducedScoutVMConfig>((WekaSet) trainingSet);
   }

   @Override
   protected TrainingSet<ReducedScoutVMConfig, WekaModelSample> emptyTrainingSet() {
      final ReducedScoutVMConfigWekaTestSet ts = (ReducedScoutVMConfigWekaTestSet) this.testSet;
      assert ts != null;
      return new ReducedScoutVMConfigWekaTrainingSet(ts.arff());
   }

   @Override
   protected TrainingSet<ReducedScoutVMConfig, WekaModelSample> emptyTrainingSetForApprox() {
      if (true) throw new RuntimeException("NO");
      return new ReducedScoutVMConfigWekaTrainingSet("files/scout_utility.arff");
   }

   @Override
   protected TestSet<ReducedScoutVMConfig, WekaModelSample> fullTestSet() {
      if (WekaGaussianProcess.useNominalAttributes) {
         System.out.println("Nominal attributes") ;
         return ReducedWekaScoutVMConfigFactory.buildInitTestSet("files/reduced_scout.arff");
      }
      System.out.println("Binary attributes") ;
      return ReducedWekaScoutVMConfigFactory.buildInitTestSet("files/reduced_scout_gp.arff");
   }


   @Override
   protected LHS<ReducedScoutVMConfig> instantiateLHS(int initTrainSamples) {
      return new ReducedScoutLHS(initTrainSamples);
   }

}
