package weka.scout;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.scout.ScoutLHS;
import lynceus.scout.ScoutVMConfig;
import lynceus.scout.ScoutVMCostGenerator;
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
public class WekaScoutLynceus extends Lynceus<ScoutVMConfig, WekaModelSample> {
   public WekaScoutLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
      super(h, b, epsilon, gamma, opt, wkldid);
   }

   @Override
   protected CostGenerator<ScoutVMConfig> buildCostGenerator(long seed) {
      if (costGenerator == null) {
         costGenerator = new ScoutVMCostGenerator();
      }
      return costGenerator;
   }

   @Override
   protected TestSet<ScoutVMConfig, WekaModelSample> initialTestSet() {
      return WekaScoutVMConfigFactory.buildInitTestSet("files/scout.arff");
   }

   @Override
   protected PredictiveModel<ScoutVMConfig, WekaModelSample> buildPredictiveModel(TrainingSet<ScoutVMConfig, WekaModelSample> trainingSet, ModelParams params) {
      return new WekaGaussianProcess<ScoutVMConfig>((WekaSet) trainingSet,params);
   }

	@Override
	protected PredictiveModel<ScoutVMConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<ScoutVMConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<ScoutVMConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}
   
   @Override
   protected TrainingSet<ScoutVMConfig, WekaModelSample> emptyTrainingSet() {
      final ScoutVMConfigWekaTestSet ts = (ScoutVMConfigWekaTestSet) this.testSet;
      assert ts != null;
      return new ScoutVMConfigWekaTrainingSet(ts.arff());
   }


   @Override
   protected PredictiveModel<ScoutVMConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<ScoutVMConfig, WekaModelSample> trainingSet) {
      return new HackGaussianProcess<ScoutVMConfig>((WekaSet) trainingSet);
   }

   @Override
   protected TrainingSet<ScoutVMConfig, WekaModelSample> emptyTrainingSetForApprox() {
      return new ScoutVMConfigWekaTrainingSet("files/scout_utility.arff");
   }

   @Override
   protected TestSet<ScoutVMConfig, WekaModelSample> fullTestSet() {
      return WekaScoutVMConfigFactory.buildInitTestSet("files/scout.arff");
   }

	@Override
	protected LHS<ScoutVMConfig> instantiateLHS(int initTrainSamples) {
		return new ScoutLHS(initTrainSamples);
	}

   /**
    * Bootstrap the set of explored configs according to Latin Hypercube Sampling
    * Because our grid is not "uniform", we resort to a kind of hack: we draw at random and we only accept the new
    * config if and only if it does not overlap with any previously sampled config  on any dimension.
    * Of course, we can have problems such that we are stuck with a drawing sequence that makes it impossible to sample
    * the next. In that case, we sample at random
    * This can also happen if we want the initial set to have more samples than the number of dimensions
    *
    * @param allConfigs
    * @param budget
    * @param seed
    * @return
    */
   /*
   protected State<ScoutVMConfig, WekaModelSample> init(TestSet<ScoutVMConfig, WekaModelSample> allConfigs, double budget, long seed) {
      System.out.println("Initializing");
      final State<ScoutVMConfig, WekaModelSample> currState = new State<>();
      final TrainingSet<ScoutVMConfig, WekaModelSample> trainingSet = emptyTrainingSet();
      currState.setBudget(budget);
      currState.setTestSet(allConfigs);
      currState.setTrainingSet(trainingSet);
      int total = initTrainSamples;
      Random r = new Random(seed);
      Set<ScoutVMConfig> possible = new HashSet<>();
      for (int i = 0; i < allConfigs.size(); i++) {
         possible.add(allConfigs.getConfig(i));
      }
      Set<ScoutVMConfig> drawn = new HashSet<>();
      Set<ScoutVMConfig> discarded = new HashSet<>();
      while (total > 0 && possible.size() > 0) {
         ScoutVMConfig nextConfig = allConfigs.getConfig(r.nextInt(possible.size()));
         drawn.add(nextConfig);
         skim(nextConfig, possible);
         long deployCost = setupConfig(nextConfig, currState, this.costGenerator);
         SamplingResult<ScoutVMConfig> samplingResult = sample(nextConfig, this.costGenerator, currState);
         updateState(currState, deployCost, samplingResult);
         total--;
         drawn.add(nextConfig);
      }

      System.out.println("Initialized.");
      //currState.getTrainingSet().printAll();
      return currState;
   }

   private void skim(ScoutVMConfig curr, Set<ScoutVMConfig> possible) {
      for (ScoutVMConfig s : possible) {
         if(overlap(s,curr)){
            possible.remove(s);
         }
      }
   }

*/

}
