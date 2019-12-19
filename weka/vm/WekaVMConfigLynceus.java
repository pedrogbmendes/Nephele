package weka.vm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import lynceus.CostGenerator;
import lynceus.LHS;
import lynceus.Lynceus;
import lynceus.Pair;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.vm.VMConfig;
import lynceus.vm.VMDummyCostGenerator;
import lynceus.vm.VMLHS;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.WekaSet;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.tuning.ModelParams;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public class WekaVMConfigLynceus extends Lynceus<VMConfig, WekaModelSample> {

   /*
   It might be a good idea to have a gamma that is not too large, and an horizon that is not too long.
   In general, we are trusting the model, and we are making some approximations, e.g., GH.
   So we may want to use the model to understand the directions but without taking it too seriously when it comes
   to speculations too much in the future.

   We can think of adaptive schemes in which we do a sort of 10fold / leave-one-out  CV to assess the quality of the
   model, and based on that we can tune gamma
    */
   private final static double gamma = 0.9;
   private final static long horizon = 2;
   private final static double epsilon_u = 0.1;

   private static boolean printed = false;

   public WekaVMConfigLynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
      super(h, b, epsilon, gamma, opt, wkldid);
   }

   protected TestSet<VMConfig, WekaModelSample> fullTestSet() {
      return WekaVMConfigFactory.buildInitTestSet("files/vm.arff");
   }

   private TestSet<VMConfig, WekaModelSample> printAll() {
      TestSet<VMConfig, WekaModelSample> testSet = WekaVMConfigFactory.buildInitTestSet("files/vm.arff");
      if (costGenerator == null) {
         throw new RuntimeException("Cost generator is null");
      }
      System.out.println("PRE  Total test set " + testSet.size());

      Set<Pair<VMConfig, Double>> set = new HashSet<Pair<VMConfig, Double>>();
      for (int i = 0; i < testSet.size(); i++) {
         VMConfig c = testSet.getConfig(i);
         double runningCost = costGenerator.deploymentCost(null, c);
         set.add(new Pair<VMConfig, Double>(c, runningCost));
      }

      for (Pair<VMConfig, Double> p : set) {
         testSet.removeConfig(p.getFst());
         testSet.addTestSampleWithTarget(p.getFst(), p.getSnd());
         //System.out.println("added " + p.fst + ", " + p.snd);
      }

      System.out.println("POST Total test set " + testSet.size());
      testSet.printAll();
      return testSet;
   }


   @Override
   protected TestSet<VMConfig, WekaModelSample> initialTestSet() {
      if (!printed) {
         printAll();
         printed = true;
      }
      return WekaVMConfigFactory.buildInitTestSet("files/vm.arff");
   }


   @Override
   protected PredictiveModel<VMConfig, WekaModelSample> buildPredictiveModel(TrainingSet<VMConfig, WekaModelSample> trainingSet, ModelParams params) {
      //return new OmniscentWekaGaussianProcess((WekaSet) trainingSet, costGenerator);//
      return new WekaGaussianProcess<>((WekaSet) trainingSet,params);
   }

	@Override
	protected PredictiveModel<VMConfig, WekaModelSample> buildPredictiveModel(
			TrainingSet<VMConfig, WekaModelSample> trainingSet, ModelParams params,TestSet<VMConfig, WekaModelSample> testSet, long seed) {
		return new WekaGaussianProcess<>((WekaSet) trainingSet,params, (WekaSet)testSet, seed);
	}
  
   
   @Override
   protected TrainingSet<VMConfig, WekaModelSample> emptyTrainingSet() {
      final VMConfigWekaTestSet ts = (VMConfigWekaTestSet) this.testSet;
      return new VMConfigWekaTrainingSet(ts.arff());
   }

   @Override
   protected CostGenerator<VMConfig> buildCostGenerator(long seed) {
      if (costGenerator == null) {
         costGenerator = new VMDummyCostGenerator("files/vm.arff", 1000);
      }
      return costGenerator;
   }

   @Override
   protected PredictiveModel<VMConfig, WekaModelSample> buildPredictiveModelForApprox(TrainingSet<VMConfig, WekaModelSample> trainingSet) {
      throw new RuntimeException("Not supported ");
   }

   @Override
   protected TrainingSet<VMConfig, WekaModelSample> emptyTrainingSetForApprox() {
      // return new VMConfigWekaTrainingSet("files/vm_ei.arff");
      throw new RuntimeException("Not supported ");
   }

   public void offlineTest(double trainingPerc, long seed) {
      VMConfigWekaTestSet testSet = WekaVMConfigFactory.buildInitTestSet("files/vm.arff");
      VMConfigWekaTrainingSet trainingSet = new VMConfigWekaTrainingSet("files/vm.arff");
      Set<Pair<VMConfig, Double>> set = new HashSet<Pair<VMConfig, Double>>();
      Random rnd = new Random(seed);
      CostGenerator<VMConfig> d = this.buildCostGenerator(seed);
      for (int i = 0; i < testSet.size(); i++) {
         VMConfig c = testSet.getConfig(i);
         double runningCost = d.deploymentCost(null, c);
         set.add(new Pair<VMConfig, Double>(c, runningCost));
      }


      for (Pair<VMConfig, Double> p : set) {
         if (rnd.nextDouble() < trainingPerc) {
            testSet.removeConfig(p.getFst());
            trainingSet.add(p.getFst(), p.getSnd());
         } else {
            testSet.removeConfig(p.getFst());
            testSet.addTestSampleWithTarget(p.getFst(), costGenerator.deploymentCost(null, p.getFst()));
         }
         //System.out.println("added " + p.fst + ", " + p.snd);
      }

      PredictiveModel<VMConfig, WekaModelSample> model = buildPredictiveModel(trainingSet,null);
      model.train();
      double mape = 0;
      for (int i = 0; i < testSet.size(); i++) {
         VMConfig cc = testSet.getConfig(i);
         double cost = d.deploymentCost(null, cc);
         double pred = model.evaluate(cc);
         double err = Math.abs(cost - pred) / cost;
         System.out.println(cc + " " + cost + " vs " + pred);
         mape += err;

      }
      mape /= trainingSet.size();
      /*
      System.out.println("TRAIN");
      trainingSet.printAll();
      System.out.println("TEST");
      testSet.printAll();
      */
      System.out.println("Training " + trainingSet.size() + " Test " + testSet.size() + " MAPE " + mape);


      Instances trainInstances = trainingSet.getInstancesCopy();
      Instances testInstances = testSet.getInstancesCopy();
      System.out.println(testInstances);
      try {
         //Classifier scheme = ((WekaGaussianProcess) this.buildPredictiveModel(trainingSet)).getModel();
         //scheme.buildClassifier(trainInstances);
         Classifier scheme = ((WekaGaussianProcess) model).getModel();
         Evaluation evaluation = new Evaluation(trainInstances);       //NB: traininstances are only needed to get the header. (try set it to null to see it ;)  )
         double[] predd = evaluation.evaluateModel(scheme, testInstances);
         System.out.println(evaluation.toSummaryString());
         System.out.println(Arrays.toString(predd));
      } catch (Exception e) {
         e.printStackTrace();
      }

   }

	@Override
	protected LHS<VMConfig> instantiateLHS(int initTrainSamples) {
		return new VMLHS(initTrainSamples);
	}


}
