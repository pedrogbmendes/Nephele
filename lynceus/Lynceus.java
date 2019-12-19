package lynceus;

import gh.GaussHermiteParams;
import gh.NormalGaussHermiteQuadrature;
import lynceus.Main.timeout;
import lynceus.results.OptResult;
import lynceus.tensorflow.TensorflowConfigCostGenerator;
import lynceus.tensorflow.TensorflowConfigCostGeneratorSize;
import lynceus.tensorflow.TensorflowConfigSize;

import org.apache.commons.math3.distribution.NormalDistribution;
import regression.LinTrendLine;
import regression.LogTrendLine;
import regression.PolyTrendLine;
import regression.TrendLine;
import weka.AbstractConfigWekaTestSet;
import weka.WekaGaussianProcess;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.gauss.CustomGP;
import weka.tuning.ModelParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */

public abstract class Lynceus<C extends Configuration, M extends ModelSample> {

   public static boolean log_transform = false;

   static {
      if (log_transform) {
         throw new RuntimeException("No log transform for now");
      }
      System.out.println("log_transform is " + log_transform);

   }

   protected CostGenerator<C> costGenerator;
   public static int initTrainSamples = 10;
   public static int gaussHermitePoints = 3;
   public static double discreteBudget;
   public static double maxTimePerc;
   private final double budget_confidence = 0.99;
   private final double z_99 = 2.326348;   // x = mu + z s  for the percentile of a normal distribution
   private static double T_max = 500;      // Maximum acceptable runtime for the target job
   private static double AccConstrain = 0.85;
   protected TestSet<C, M> testSet;
   private long horizon;               // In the lookahead
   private double budget;               // Monetary budget to run the exploration
   private double cumulativeCost;         // Cumulative monetary cost of the exploration phase
   private long cumulativeExplorations;     // Number of explorations done during the exploration phase
   private double epsilon_utility;         // In percentage w.r.t. last one
   private double maxVar = 0.0;			//max variance of the model

   private double gamma; // Discount factor for future rewards: 0 = only consider immediate reward; 1 = long-term reward has the same value as short term reward
   private double delta; // Parameter for the UCB BO. It is set to be equal to gamma (which is not used with UCB)

   private double maxCost = Double.NEGATIVE_INFINITY;  // Max cost to run the job until completion seen so far

   private double timeExp = 0.0; //in seconds
   private final static boolean _trace_eval_ = false;

   private static int THREADS;

   private int consecutiveAcceptableUtilities = 0;
   private final static int minAcceptableUtilities = 2;

   private final optimizer opt_type;

   private final long wkldid;

   private long searchSpaceCardinality = -1;

   private LHS<C> lhs;

   private final static boolean lyn_print = false;
   private final static boolean penalty_cost = false;
   private final static boolean initSamples_budget = true;


   private static PrintWriter debugWriter = null;
   private static boolean debug_logs;

   private int stdv_counter;
   private int stdv_total_counter;


   private static timeout timeoutType;

   private static PrintWriter estimationErrorsWriter = null;
   private static boolean estimationErrors_logs;
   private long timeoutCounter;   // number of times an exploration is timed out. This should be equal to the length of the estimationErrors array
   ArrayList<Double> estimationErrors = new ArrayList<Double>();   // estimation errors of the linear model (for the timeout policy) that
   // predicts the costs of running a job until completion

   private int nexToOpt;	// number of explorations performed until the config that is returned was found
   private double currDFO;	// keep track of the current and next DFOs to see if a better
   private double nextDFO;	// configuration has been found
   private long seed = 0;

   private static boolean runLogs;
   private static PrintWriter runLogsWriter = null;

   /* these variables are for the runLogs, for each exploration */
   private double avgMuCost = 0;
   private double mu50Cost = -1;
   private double mu90Cost = -1;
   private double avgSigmaCost = 0;
   private double sigma50Cost = -1;
   private double sigma90Cost = -1;
   private double avgEI = 0;
   private double ei50 = -1;
   private double ei90 = -1;

   private double avgMuAcc = 0;
   private double mu50Acc = -1;
   private double mu90Acc = -1;
   private double avgSigmaAcc = 0;
   private double sigma50Acc = -1;
   private double sigma90Acc = -1;



   //   static{
//	   if(debug_logs){
//			String file = "files/debug/" + Main.optimizer + "_timeout" + withTimeOut + "_initSamples" + initTrainSamples + "_budget" + Main.budget + "_lookahead" + Main.horizon + "_gamma" + Main.gamma + "_gh" + gaussHermitePoints + "_maxTime" + Main.max_time_perc + ".txt";
//			try {
//				fh = new FileHandler(file);
//				LOGGER.addHandler(fh);
//		        SimpleFormatter formatter = new SimpleFormatter();
//		        fh.setFormatter(formatter);
//			} catch (SecurityException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//	    }
//   }

   public static void setGaussHermitePoints(int gaussHermitePoints) {
      Lynceus.gaussHermitePoints = gaussHermitePoints;
   }

   /* T_max in min */
   public static void setT_max(double t) {
      T_max = t;
      if (t == 0) {
         throw new RuntimeException("Max time cannot be zero");
      }
   }


   // hill climbing parameters
   public static boolean HC = false;

   public enum optimizer {
      LYNCEUS, CHERRYPICK, LYNCEUS_OPT, CHERRYPICK_PURCIARO, CHERRYPICK_UCB, LYNCEUS_APPROX, LYNCEUS_PURCIARO, RAND, LYNCEUS_GP, CHERRYPICK_GP
   }


   public Lynceus(long h, double b, double epsilon, double gamma, optimizer opt, long wkldid) {
      this.horizon = h;
      this.budget = b;
      this.cumulativeCost = 0;
      this.cumulativeExplorations = 0;
      this.costGenerator = buildCostGenerator(h);
      if(initSamples_budget == false) {
    	  this.lhs = instantiateLHS(initTrainSamples);
      }else {
    	  this.lhs = instantiateLHS(250);
      }
      
      this.epsilon_utility = epsilon;
      this.gamma = gamma;
      this.delta = gamma;
      this.opt_type = opt;
      this.wkldid = wkldid;
      this.timeoutCounter = 0;

      this.stdv_counter = 0;
      this.stdv_total_counter = 0;

      this.estimationErrors.clear();

      this.nexToOpt = -1;
      this.currDFO = Double.POSITIVE_INFINITY;	// keep track of the current and next DFOs to see if a better
      this.nextDFO = Double.POSITIVE_INFINITY;
   }

   public static void setDebugLogs() {
      debug_logs = true;
      String timeout = Main.timeoutToStr(timeoutType);

      String file = "files/debug/" + Main.optimizer + "_timeout" + timeout + "_bootstrapMethod_" + Main.bootstrap + "_initSamples" + initTrainSamples + "_budget" + Main.budget + "_lookahead" + Main.horizon + "_gamma" + Main.gamma + "_gh" + gaussHermitePoints + "_maxTime" + Main.max_time_perc + "_" + Main.file + ".txt";
      File f = new File(file);
      if (f.exists()) {
         f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
      }
      try {
         debugWriter = new PrintWriter(file, "UTF-8");
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   public static void setEstimationErrorsLogs() {
      estimationErrors_logs = true;
      String timeout = Main.timeoutToStr(timeoutType);

      String file = "files/estimationErrors/" + Main.optimizer + "_timeout" + timeout + "_bootstrapMethod_" + Main.bootstrap + "_initSamples" + initTrainSamples + "_budget" + Main.budget + "_lookahead" + Main.horizon + "_gamma" + Main.gamma + "_gh" + gaussHermitePoints + "_maxTime" + Main.max_time_perc + "_" + Main.file + ".txt";
      File f = new File(file);
      if (f.exists()) {
         f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
      }
      try {
         estimationErrorsWriter = new PrintWriter(file, "UTF-8");
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      estimationErrorsWriter.println("wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;numSeeds;seed;lookahead;gamma;gh;maxTimePerc;estimationError");
      estimationErrorsWriter.flush();
   }

   public static void setRunLogs(long seed) {
	   runLogs = true;
      String timeout = Main.timeoutToStr(timeoutType);

      String file = "files/runLogs/" + "runID_" + seed + "_numSeeds_" + Main.numSeeds + "_optimizer_" + Main.optimizer + "_timeout" + timeout + "_bootstrapMethod_" + Main.bootstrap + "_initSamples" + initTrainSamples + "_budget" + Main.budget + "_lookahead" + Main.horizon + "_gamma" + Main.gamma + "_gh" + gaussHermitePoints + "_maxTime" + Main.max_time_perc + "_" + Main.file + ".txt";
      File f = new File(file);
      if (f.exists()) {
         f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
      }
      try {
         runLogsWriter = new PrintWriter(file, "UTF-8");
      } catch (FileNotFoundException | UnsupportedEncodingException e) {
         //

         e.printStackTrace();
      }
      runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO;runningTime(mins);Acc;Cost;EIc;avgMuCost;avgSigmaCost;avgEI;mu50Cost;sigma50Cost;ei50;mu90Cost;sigma90Cost;ei90;avgMuAcc;avgSigmaAcc;mu50Acc;sigma50Acc;mu90Acc;sigma90Acc;Time(mins)");
      runLogsWriter.flush();
   }


   public static void setTIMEOUT(timeout type) {
      timeoutType = type;
   }


   public static void setTHREADS(int tHREADS) {
      THREADS = tHREADS;
   }

   public static void setInitTrainingSamples(int initSamples) {
      initTrainSamples = initSamples;
   }

   public static void setDiscreteBudget(double budget) {
      discreteBudget = budget;
   }

   public static void setMaxTimePerc(double timePerc) {
      maxTimePerc = timePerc;
   }

   @Override
   public String toString() {
      return "{" +
            "initTrainSamples=" + initTrainSamples +
            ", gaussHermitePoints=" + gaussHermitePoints +
            ", T_max=" + T_max +
            ", horizon=" + horizon +
            ", budget=" + budget +
            ", epsilon_utility=" + epsilon_utility +
            ", gamma=" + gamma +
            ", consecutiveAcceptableUtilities=" + consecutiveAcceptableUtilities +
            '}';
   }

   protected abstract CostGenerator<C> buildCostGenerator(long seed);

   protected abstract TestSet<C, M> initialTestSet();

   protected abstract PredictiveModel<C, M> buildPredictiveModel(TrainingSet<C, M> trainingSet, ModelParams params);

   protected abstract PredictiveModel<C, M> buildPredictiveModel(TrainingSet<C, M> trainingSet, ModelParams params, TestSet<C, M> testSet, long seed);

   protected abstract PredictiveModel<C, M> buildPredictiveModelForApprox(TrainingSet<C, M> trainingSet);

   protected abstract TrainingSet<C, M> emptyTrainingSet();

   protected abstract TrainingSet<C, M> emptyTrainingSetForApprox();

   protected abstract TestSet<C, M> fullTestSet();

   protected abstract LHS<C> instantiateLHS(int initTrainSamples);

   private C bestConfigGroundTruth(TestSet<C, M> fullTestSet, double t_max) {
	   Pair<C, Double> opt = null;

	   if(this.costGenerator instanceof TensorflowConfigCostGenerator) {

		   TensorflowConfigCostGeneratorSize costGeneratorall = ((TensorflowConfigCostGenerator) costGenerator).dataset_all;

			for(TensorflowConfigSize cc : costGeneratorall.getCostConfig().keySet()) {

		         double y = costGeneratorall.deploymentCost(null,  (TensorflowConfigSize) cc);
		         double time = y / costGeneratorall.costPerConfigPerMinute((TensorflowConfigSize) cc);   // time in min
		         double acc = costGeneratorall.getAccForConfig((TensorflowConfigSize) cc);

	        	 //tensorflow with dataset sizes
		        if (time > t_max)
		        	 continue;

		        if ((opt == null || opt.getSnd() > y) && acc >= AccConstrain) {
		        		C conf = (C) cc;
		        		opt = new Pair<>(conf, y);
		        }
	         }

	   }else {

	      for (int i = 0; i < fullTestSet.size(); i++) {
	         C cc = fullTestSet.getConfig(i);
	         double y = costGenerator.deploymentCost(null, cc);
	         double time = y / costGenerator.costPerConfigPerMinute(cc);   // time in min
	         double acc = costGenerator.getAccForConfig(cc);
	         if(acc == -1) {
	        	 if (time > t_max)
		        	 continue;
		         if (opt == null || opt.getSnd() > y) {
		        	 opt = new Pair<>(cc, y);
		         }
	         }else {
	        	 //tensorflow with dataset sizes
		        if (time > t_max)
		        	 continue;

		        if ((opt == null || opt.getSnd() > y) && acc >= AccConstrain) {
		        		opt = new Pair<>(cc, y);
		        }
	         }

	     }
	   }
      if (opt == null) {
         throw new RuntimeException("No optimal config found. This should not happen, since we" +
                                          "are using t_max always at least as high as the minimum");
      }

      //C opt_ = (C) new TensorflowConfigSize(1,8,0.0001,16,0,0,1.5/6.0);
      //return opt_;
      return opt.getFst();
   }


   private double distanceFromOpt(TestSet<C, M> fullTestSet, C config, double tmax) {
	   Pair<C, Double> opt;

	  if(this.costGenerator instanceof TensorflowConfigCostGenerator) {
		  C optConfig = bestConfigGroundTruth(fullTestSet, tmax);
		  double costOpt = ((TensorflowConfigCostGenerator) costGenerator).dataset_all.deploymentCost(null, (TensorflowConfigSize) optConfig);
		  opt = new Pair<>(optConfig, costOpt);

	  }else{
		  opt = bestInTest(fullTestSet, tmax);
	  }


	  double chosenCost = 0;

      if(penalty_cost) {
          double acc = costGenerator.getAccForConfig(config);
          double runningTime = costGenerator.deploymentCost(null, config) / costGenerator.costPerConfigPerMinute(config);

    	  if(acc!=-1 && (acc < AccConstrain || runningTime > tmax)) {
    		  chosenCost = costGenerator.deploymentCost(null, config) + this.maxCost + 3*this.maxVar ;
    	  }else {
    		  chosenCost = costGenerator.deploymentCost(null, config);
    	  }
      }else {
    	  /*if(acc!=-1 && (acc < AccConstrain || runningTime > tmax)) {
    		 chosenCost = costGenerator.deploymentCost(null, config) + this.maxCost + 3*this.maxVar ;
    	  }else {
    		  chosenCost = costGenerator.deploymentCost(null, config);
    	  }*/
    	  chosenCost = costGenerator.deploymentCost(null, config);
      }


      final double optCost = opt.getSnd();
      //final double optCost = 0.00165344749221095;
      //System.out.println(config + " cost=" + chosenCost + " optCost=" + optCost + " DFO=" + (chosenCost - optCost) / optCost);

      if (optCost == 0) {
         throw new RuntimeException("WKLD " + wkldid + ": cost for opt " + opt.getFst() + " is " + optCost);
      }

      return (chosenCost - optCost) / optCost;
      //TODO: I do not return the info whether the chosen complies with the target tmax
   }


   private void evalOnTest(State<C, M> evalState, int iter) {
      PredictiveModel<C, M> m = buildPredictiveModel(evalState.getTrainingSet(), evalState.getParams());
      m.train();
      double mape = 0;
      //System.out.println("Training set for eval on test");
      //evalState.getTrainingSet().printAll();
      //System.out.println("Eval on test");
      for (int ti = 0; ti < evalState.getTestSet().size(); ti++) {
         C cc = evalState.getTestSet().getConfig(ti);
         double groundTruthCost = costGenerator.deploymentCost(null, cc);
         double predCost = m.evaluate(cc);
         double err = Math.abs(groundTruthCost - predCost) / groundTruthCost;
         System.out.println(cc + " real " + groundTruthCost + " pred " + predCost + " err " + err);
         mape += err;
      }
      mape /= evalState.getTestSet().size();
      System.out.println("[" + (iter) + "] MAPE on ground truth " + mape);
   }


   public OptResult doYourJob(long seed) {
      System.out.println("================ " + this.opt_type + "============ " + seed + " " + this);
      System.out.flush();
      if (debug_logs) {
         debugWriter.println("================ " + this.opt_type + "============ " + seed + " " + this);
         debugWriter.flush();
      }

      if (opt_type.toString().contains("_GP")) {
         WekaGaussianProcess.ensemble = false;
         System.out.println("Using GP");
      } else {
         WekaGaussianProcess.ensemble = true;
         System.out.println("USING RANDOM FOREST");
      }

      switch (opt_type) {
         case LYNCEUS_OPT:
            return lynceus(seed);
         case LYNCEUS:
            return lynceus(seed);
         case LYNCEUS_PURCIARO:
            return lynceus((seed));
         case CHERRYPICK_PURCIARO:
            return cherryPick(seed);
         case CHERRYPICK:
            return cherryPick(seed);
         case CHERRYPICK_UCB:
            return cherryPick(seed);
         case LYNCEUS_APPROX:
            return lynceus(seed);
         case LYNCEUS_GP:
            return lynceus(seed);
         case CHERRYPICK_GP:
            return cherryPick(seed);
         case RAND:
            return rand(seed);
         default:
            throw new RuntimeException(opt_type + " not valid");
      }
   }


   private OptResult rand(long seed) {

	   long startTime = System.currentTimeMillis();
	   this.seed = seed;

      /* keep track of the errors of the linear model for estimating cost */
      double error = 0.0;
      double finalError = 0.0;   // average of the errors of all explorations of the same seed

      this.testSet = initialTestSet();
      this.searchSpaceCardinality = this.testSet.size();

      final TestSet<C, M> allTestConfigsNoTarget = this.fullTestSet();

//      int j = 0;
//      while (j < allTestConfigsNoTarget.size()) {
//    	  C c = allTestConfigsNoTarget.getConfig(j);
//    	  System.out.println("DFO = " + distanceFromOpt(allTestConfigsNoTarget, c, T_max));
//    	  j ++;
//      }

      State<C, M> currState;

      String timeout = Main.timeoutToStr(timeoutType);


      if (Main.bootstrap.equalsIgnoreCase("lhs")) {
         currState = initLHS(testSet, budget, seed, allTestConfigsNoTarget, startTime);
      } else {
         currState = init(testSet, budget, seed, allTestConfigsNoTarget, startTime);
//     	  List<C> samples = new ArrayList();
//     	  for (int i = 0; i < currState.getTrainingSet().size(); i++) {
//     		  samples.add(currState.getTrainingSet().getConfig(i).getFst());
//     	  }
//     	  lhs.checkSamples(samples, "random", (int)seed);
      }

      if (lyn_print) {
         System.out.println("Seed: " + seed + " ; State after init" + currState);
      }
      if (debug_logs) {
         debugWriter.println("Seed: " + seed);
         debugWriter.println("State after init" + currState);
         debugWriter.flush();
      }

      /* this predictive model SHOULD NOT be here
       * I put it just to assure that the init-dfo is the same
       * for all optimizers and that the bestInTrain method
       * outputs the most expensive config with a cost that is
       * cost = cost + 3*variance when there are no feasible configs */

      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(), currState.getTestSet(), seed);
      model.train();

      C initBest = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      if (penalty_cost)
    	  initBest = bestInTrain(currState.getTrainingSet(), T_max).getFst();

      double initDist = distanceFromOpt(allTestConfigsNoTarget, initBest, T_max);

      long setupCost;
      SamplingResult<C> samplingResult;
      int iter = 0;

      double prevU = -1;

      Random random = new Random(seed);

      /* while there are unexplored configs and we have budget ==> explore */
      while (currState.getTestSet().size() > 0 && currState.getBudget() > 0) {

         if (_trace_eval_) {
            evalOnTest(currState, iter);
         }

         C nextConfig = null;

         /* select random config from the set of unexplored configs */

//           final ArrayList<C> testSet = new ArrayList<C>();
//           for (int j = 0; j < currState.getTestSet().size(); j ++){
//        	   testSet.add(currState.getTestSet().getConfig(j));
//           }
//           //System.out.println("Pre-RAND: 1st config -- " + testSet.get(0) + " ; SIZE = " + testSet.size());
//           Collections.shuffle(testSet, random);	/* shuffle set of unexplored configs */
//           //System.out.println("Post- RAND: 1st config -- " + testSet.get(0) + " ; SIZE = " + testSet.size());
//           nextConfig = testSet.get(0)	;	/* remove first config of the shuffled set */
//           //System.out.println(opt);

         if(penalty_cost) {
        	 if(model.maxVariance(testSet) > this.maxVar)
        		 this.maxVar = model.maxVariance(testSet);
         }

         /* alternative method */
         nextConfig = currState.getTestSet().getConfig(random.nextInt(currState.getTestSet().size()));

         /* explore the chosen config and update your state */
         setupCost = setupConfig(nextConfig, currState, this.costGenerator);
         samplingResult = sample(nextConfig, costGenerator, currState);
         updateState(currState, setupCost, samplingResult);

         if(currState.getBudget() <= 0) break;

         double costConfig = costGenerator.deploymentCost(null, nextConfig);
         double runningTime = costConfig / costGenerator.costPerConfigPerMinute(nextConfig); // in minutes
         this.timeExp += (runningTime*60.0);	//in seconds
         double accConfig = costGenerator.getAccForConfig(nextConfig);

         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);

         if (runLogs) {
           double actTime =  ((System.currentTimeMillis()-startTime)/1000.0 + this.timeExp)/60.0; //in MINS
	         //runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO");
	         runLogsWriter.println(seed + ";" + Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";0;0.0;0;" + maxTimePerc + ";" + cumulativeExplorations + ";" + nextConfig.toString() + ";" + nextDFO + ";" + currDFO +  ";" + runningTime + ";" + accConfig + ";" + costConfig + ";0;" + this.avgMuCost + ";" + this.avgSigmaCost + ";" + this.avgEI + ";" + this.mu50Cost + ";" + this.sigma50Cost + ";" + this.ei50 + ";" + this.mu90Cost + ";" + this.sigma90Cost + ";" + this.ei90 + ";" + this.avgMuAcc + ";" + this.avgSigmaAcc + ";" + this.mu50Acc + ";" + this.sigma50Acc + ";" + this.mu90Acc + ";" + this.sigma90Acc + ";" +  actTime);
	         runLogsWriter.flush();
         }

         // better config was found. Let us save the NEX so far
         if (this.nextDFO < this.currDFO) {
        	 if(accConfig >= AccConstrain && runningTime <= T_max) {
        		 //System.out.println(accConfig + " " + runningTime);
        		 this.currDFO = this.nextDFO;
        	 }
        	 this.nexToOpt = (int) cumulativeExplorations;
         }

         if (lyn_print) {
            System.out.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
         }
         if (debug_logs) {
            debugWriter.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
            debugWriter.println("#########################################################################");
            debugWriter.flush();
         }
         iter++;
      }

      C ret = bestInTrain(currState.getTrainingSet(), T_max).getFst();
      double dist = distanceFromOpt(allTestConfigsNoTarget, ret, T_max);
      double costPerUnitOfTime = costGenerator.costPerConfigPerMinute(ret);
      double time = costGenerator.deploymentCost(null, ret) / costPerUnitOfTime;
      C groundBest = bestConfigGroundTruth(allTestConfigsNoTarget, T_max);

      double costPerTimePerBest=0;
      double timeBest = 0;
      if(costGenerator instanceof TensorflowConfigCostGenerator) {
	      costPerTimePerBest = ((TensorflowConfigCostGenerator) costGenerator).dataset_all.costPerConfigPerMinute((TensorflowConfigSize) groundBest);
	      timeBest = ((TensorflowConfigCostGenerator) costGenerator).dataset_all.deploymentCost(null, (TensorflowConfigSize) groundBest) / costPerTimePerBest;
      }else {
	      costPerTimePerBest = costGenerator.costPerConfigPerMinute(groundBest);
	      timeBest = costGenerator.deploymentCost(null, groundBest) / costPerTimePerBest;
      }

      boolean withinTmax = time <= T_max;
      if (lyn_print) {
         System.out.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         System.out.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
      }
      if (debug_logs) {
         debugWriter.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         debugWriter.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
         debugWriter.flush();
      }

      /* compute average error */
      for (int i = 0; i < estimationErrors.size(); i++) {
         finalError += estimationErrors.get(i);
         if (estimationErrors_logs) {
            //estimationErrorsWriter.println("wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;numSeeds;seed;lookahead;gamma;gh;maxTimePerc;estimationError");
            estimationErrorsWriter.println(Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";" + Main.numSeeds + ";" + seed + ";0;0.0;0;" + maxTimePerc + ";" + estimationErrors.get(i));
            estimationErrorsWriter.flush();
         }
      }
      finalError /= estimationErrors.size();

      double intervalTime = (System.currentTimeMillis()-startTime)/1000.0 + this.timeExp;

      System.out.println("stdv_counter = " + stdv_counter + " ; stdv_total_counter = " + stdv_total_counter + " ; %fails = " + (float) stdv_counter / (float) stdv_total_counter);

      return new OptResult(this.opt_type, this.cumulativeCost, (long) time, 0, cumulativeExplorations, initDist, dist, ret, seed, initTrainSamples, horizon, this.gaussHermitePoints, this.epsilon_utility, this.budget, withinTmax, finalError, timeoutCounter, nexToOpt, intervalTime);

   }

   private OptResult cherryPick(long seed) {

	   long startTime = System.currentTimeMillis();
	   this.seed = seed;

      /* keep track of the errors of the linear model for estimating cost */
      double error = 0.0;
      double finalError = 0.0;   // average of the errors of all explorations of the same seed

      this.testSet = initialTestSet();
      this.searchSpaceCardinality = this.testSet.size();

      final TestSet<C, M> allTestConfigsNoTarget = this.fullTestSet();

      State<C, M> currState;

      String timeout = Main.timeoutToStr(timeoutType);

      if (Main.bootstrap.equalsIgnoreCase("lhs")) {
         currState = initLHS(testSet, budget, seed, allTestConfigsNoTarget, startTime);
      } else {
         currState = init(testSet, budget, seed, allTestConfigsNoTarget, startTime);
      }

      if (lyn_print) {
         System.out.println("Seed: " + seed + " ; State after init" + currState);
      }
      if (debug_logs) {
         debugWriter.println("Seed: " + seed);
         debugWriter.println("State after init" + currState);
         debugWriter.flush();
      }

      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(), currState.getTestSet(), seed);
      model.train();

      PredictiveModel<C, M> modelAccuracy = buildPredictiveModel(currState.getTrainingSetAccuracy(), currState.getParamsAcc(), currState.getTestSet(), seed);
      modelAccuracy.train();

      C initBest = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      if(penalty_cost)
    	  initBest = bestInTrain(currState.getTrainingSet(), T_max).getFst();

      double initDist = distanceFromOpt(allTestConfigsNoTarget, initBest, T_max);

      long setupCost;
      SamplingResult<C> samplingResult;
      int iter = 0;

      double prevU = -1;
      Random random = new Random(seed);

      /* while there are unexplored configs and we have budget ==> explore */
      while (currState.getTestSet().size() > 0 && currState.getBudget() > 0) {

    	  //System.out.println("budget= " + currState.getBudget());

         if (_trace_eval_) {
            evalOnTest(currState, iter);
         }

         //System.out.println("CP: budget " + currState.getBudget() + " train");
         //currState.getTrainingSet().printAll();
         final Pair<C, Double> bestSoFar;
         if (penalty_cost) {
        	 bestSoFar = bestInTrain(currState.getTrainingSet(), T_max);
         }else {
        	 bestSoFar = bestInTrain(currState.getTrainingSet(), T_max, model);
         }


         C opt = null;
         double best = 0;
         /* Build and train model on current training set*/
         //PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet());
         model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(), currState.getTestSet(), seed);
         model.train();

         modelAccuracy = buildPredictiveModel(currState.getTrainingSetAccuracy(), currState.getParamsAcc(), currState.getTestSet(), seed);
         modelAccuracy.train();

         /*
           The original Cherrypick is budget unaware in the sense that it can stop
           after having spent a given budget, but does not consider the available budget when
           choosing the next config. I.e., it can sample a config and because of that go oer the budget
         */
         final Queue<C> feasibleSet = new ConcurrentLinkedQueue<>();
         populateWithFeasibleByCost(feasibleSet, currState, model, modelAccuracy, z_99, true);
	     //if(feasibleSet.size() == 0) {
	    //	 populateWithFeasibleByCost(feasibleSet, currState, model, modelAccuracy, z_99, false);
	       	 //feasibleSet.add(currState.getTestSet().getConfig(random.nextInt(currState.getTestSet().size())));
	     //}
         if (feasibleSet.size() == 0) {
        	 feasibleSet.add(currState.getTestSet().getConfig(random.nextInt(currState.getTestSet().size())));
           //System.out.println("CP: No more feasible solutions. Breaking");
           //break; //Go to return the best among the explored ones
         }

         /* order results */
         Queue<C> orderedFeasibleSet = sort(feasibleSet);


         /*
	       * Compute avgMu, avgSimga, avgEI, mu50, sigma50, ei50, mu90, sigma90 and ei90
	       * for the current exploration to add to the run logs
	       */
          int size = orderedFeasibleSet.size();
	   	  double[] muCost = new double[orderedFeasibleSet.size()];
	   	  double[] sigmaCost = new double[orderedFeasibleSet.size()];
	   	  double[] ei = new double[orderedFeasibleSet.size()];
	   	  double currMuCost = 0, currSigmaCost = 0;
	   	  double[] muAcc = new double[orderedFeasibleSet.size()];
	   	  double[] sigmaAcc = new double[orderedFeasibleSet.size()];
	   	  double currMuAcc = 0, currSigmaAcc = 0;

	      this.avgMuCost = 0;
	      this.avgSigmaCost = 0;
	      this.avgMuAcc = 0;
	      this.avgSigmaAcc = 0;
	      this.avgEI = 0;
	      int i = 0;


         /* Find the unexplored configuration with the best constrained expected improvement */
         while (orderedFeasibleSet.size() > 0) {
            C c = orderedFeasibleSet.remove();

            //for (int i = 0; i < currState.getTestSet().size(); i++) {
            //C c = currState.getTestSet().getConfig(i);
            double eic = constrainedExpectedImprovement(c, model, modelAccuracy, bestSoFar, false).fst;
            /*
            NOW EIC already takes into account the specific acquisition function
            if (opt_type.equals(optimizer.CHERRYPICK_PURCIARO)) {
               double cost = model.evaluate(c);
               eic = eic / cost;
            }
            */

            currMuCost = model.evaluate(c);
            currSigmaCost = model.stdv(c);
            currMuAcc = modelAccuracy.evaluate(c);
            currSigmaAcc = modelAccuracy.stdv(c);

    	   	muCost[i] = currMuCost;
    	   	sigmaCost[i] = currSigmaCost;
    	   	muAcc[i] = currMuAcc;
    	   	sigmaAcc[i] = currSigmaAcc;
    	   	ei[i] = eic;

    	   	this.avgMuCost += currMuCost;
    	   	this.avgSigmaCost += currSigmaCost;
    	   	this.avgMuAcc += currMuAcc;
    	   	this.avgSigmaAcc += currSigmaAcc;
    	   	this.avgEI += eic;
    	   	i++;



            if (lyn_print) {
               System.out.println("[utility] " + c + " ; eic = " + eic + " ; avgCost = " + model.evaluate(c) + " ; stdCost = " + model.stdv(c));
            }
            if (debug_logs) {
               debugWriter.println("[utility] " + c + " ; eic = " + eic + " ; avgCost = " + model.evaluate(c) + " ; stdCost = " + model.stdv(c));
               debugWriter.flush();
            }

            if (opt == null || eic > best) {
               opt = c;
               best = eic;
            }
         }


	      this.avgMuCost /= size;
	      this.avgSigmaCost /= size;
	      this.avgMuAcc /= size;
	      this.avgSigmaAcc /= size;
	      this.avgEI /= size;

	      Arrays.sort(muCost);
	      Arrays.sort(sigmaCost);
	      Arrays.sort(muAcc);
	      Arrays.sort(sigmaAcc);
	      Arrays.sort(ei);

	      int index_50 = (int) Math.floor((50.0 / 100) * size);
	      if (index_50 == size)
	         index_50 = size - 2; //not the max
	      if (size <= 2)
	    	  index_50 = (int) Math.floor(size/2);

	      this.mu50Cost = muCost[index_50];
	      this.sigma50Cost = sigmaCost[index_50];
	      this.mu50Acc = muAcc[index_50];
	      this.sigma50Acc = sigmaAcc[index_50];
	      this.ei50 = ei[index_50];

	      int index_90 = (int) Math.floor((90.0 / 100) * size);
	      if (index_90 == size)
	         index_90 = size - 2; //not the max
	      if (size <= 2)
	    	  index_90 = (int) Math.floor(size/2);

	      this.mu90Cost = muCost[index_90];
	      this.sigma90Cost = sigmaCost[index_90];
	      this.mu90Acc = muAcc[index_90];
	      this.sigma90Acc = sigmaAcc[index_90];
	      this.ei90 = ei[index_90];


         /* Check for early stop condition.*/
         double cU = best;

         double impr = cU / bestSoFar.getSnd();

         if (prevU >= 0) {
            if (impr < epsilon_utility) {
               this.consecutiveAcceptableUtilities++;
               if (impr < epsilon_utility && consecutiveAcceptableUtilities > minAcceptableUtilities) {
                  if (lyn_print) {
                     System.out.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". Stopping");
                  }
                  if (debug_logs) {
                     debugWriter.println("Predicted U is " + cU + " best so far is  " + bestSoFar.getSnd() + "  improvement is only " + impr + ". Stopping");
                     debugWriter.flush();
                  }
                  break;
               }
            } else {
               consecutiveAcceptableUtilities = 0;
            }
         }
         if (lyn_print) {
            System.out.println("Consecutive decreases " + consecutiveAcceptableUtilities + " Predicted U is " + cU + " prev was " + prevU + " curr best is " + bestSoFar.getSnd() + " improvement is " + impr + ". NOT Stopping");
         }
         if (debug_logs) {
            debugWriter.println("#########################################################################");
            debugWriter.println("Consecutive decreases " + consecutiveAcceptableUtilities + " Predicted U is " + cU + " prev was " + prevU + " curr best is " + bestSoFar.getSnd() + " improvement is " + impr + ". NOT Stopping");
            debugWriter.flush();
         }
         prevU = cU;

         if (penalty_cost) {
        	 if(model.maxVariance(testSet) > this.maxVar)
        		 this.maxVar = model.maxVariance(testSet);
         }

         /*Since you did not stop, explore the chosen config and update your state*/
         setupCost = setupConfig(opt, currState, this.costGenerator);
         samplingResult = sample(opt, costGenerator, currState);
         updateState(currState, setupCost, samplingResult);

         if(currState.getBudget() <= 0) break;

         double costConfig = costGenerator.deploymentCost(null, opt);
         double runningTime = costConfig / costGenerator.costPerConfigPerMinute(opt); //minutes
         this.timeExp += (runningTime*60.0);	//seconds
         double accConfig = costGenerator.getAccForConfig(opt);

         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, opt, T_max);

         if (runLogs) {
           double actTime =  ((System.currentTimeMillis()-startTime)/1000.0 + this.timeExp)/60.0; //in MINS
	         //runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO");
	         runLogsWriter.println(seed + ";" + Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";0;0.0;0;" + maxTimePerc + ";" + cumulativeExplorations + ";" + opt.toString() + ";" + nextDFO + ";" + currDFO + ";" + runningTime + ";" + accConfig + ";" + costConfig + ";" + cU + ";" + this.avgMuCost + ";" + this.avgSigmaCost + ";" + this.avgEI + ";" + this.mu50Cost + ";" + this.sigma50Cost + ";" + this.ei50 + ";" + this.mu90Cost + ";" + this.sigma90Cost + ";" + this.ei90 + ";" + this.avgMuAcc + ";" + this.avgSigmaAcc + ";" + this.mu50Acc + ";" + this.sigma50Acc + ";" + this.mu90Acc + ";" + this.sigma90Acc + ";" +  actTime);
	         runLogsWriter.flush();
         }

         // better config was found. Let us save the NEX so far
         if (this.nextDFO < this.currDFO) {
        	 if(accConfig >= AccConstrain && runningTime <= T_max)
        		 this.currDFO = this.nextDFO;
        	 this.nexToOpt = (int) cumulativeExplorations;
         }

         if (lyn_print) {
            System.out.println("Updated state " + currState + " after sampling " + opt + " with running cost ($) " + samplingResult.getExecutionRealCost());
         }
         if (debug_logs) {
            debugWriter.println("Updated state " + currState + " after sampling " + opt + " with running cost ($) " + samplingResult.getExecutionRealCost());
            debugWriter.println("#########################################################################");
            debugWriter.flush();
         }
         iter++;
      }

      C ret = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      if(penalty_cost)
    	  ret = bestInTrain(currState.getTrainingSet(), T_max).getFst();

      System.out.println("Exploration ended");
      double dist = distanceFromOpt(allTestConfigsNoTarget, ret, T_max);
      double costPerUnitOfTime = costGenerator.costPerConfigPerMinute(ret);
      double time = costGenerator.deploymentCost(null, ret) / costPerUnitOfTime;

      System.out.println("Best exploed " + ret + " with cost " + (time * costPerUnitOfTime) + " with time " + time + " with constraint " + T_max);
      C groundBest = bestConfigGroundTruth(allTestConfigsNoTarget, T_max);

      double costPerTimePerBest=0;
      double timeBest = 0;
      if(costGenerator instanceof TensorflowConfigCostGenerator) {
	      costPerTimePerBest = ((TensorflowConfigCostGenerator) costGenerator).dataset_all.costPerConfigPerMinute((TensorflowConfigSize) groundBest);
	      timeBest = ((TensorflowConfigCostGenerator) costGenerator).dataset_all.deploymentCost(null, (TensorflowConfigSize) groundBest) / costPerTimePerBest;
      }else {
	      costPerTimePerBest = costGenerator.costPerConfigPerMinute(groundBest);
	      timeBest = costGenerator.deploymentCost(null, groundBest) / costPerTimePerBest;
      }

      boolean withinTmax = time <= T_max;
      System.out.println("Ground best " + groundBest + " with cost " + (timeBest * costPerTimePerBest) + " with time " + timeBest + " with constraint " + T_max);
      if (lyn_print) {
         System.out.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         System.out.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
      }
      if (debug_logs) {
         debugWriter.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         debugWriter.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
         debugWriter.flush();
      }

      /* compute average error */
      for (int i = 0; i < estimationErrors.size(); i++) {
         finalError += estimationErrors.get(i);
         if (estimationErrors_logs) {
            //estimationErrorsWriter.println("wkld;optimizer;budget;bootstrapMthode;initSamples;timeout;numSeeds;seed;lookahead;gamma;gh;maxTimePerc;estimationError");
            estimationErrorsWriter.println(Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";" + Main.numSeeds + ";" + seed + ";0;0.0;0;" + maxTimePerc + ";" + estimationErrors.get(i));
            estimationErrorsWriter.flush();
         }
      }
      finalError /= estimationErrors.size();

      double intervalTime = (System.currentTimeMillis()-startTime)/1000.0 + this.timeExp;

      System.out.println("stdv_counter = " + stdv_counter + " ; stdv_total_counter = " + stdv_total_counter + " ; %fails = " + (float) stdv_counter / (float) stdv_total_counter);
      return new OptResult(this.opt_type, this.cumulativeCost, (long) time, 0, cumulativeExplorations, initDist, dist, ret, seed, initTrainSamples, horizon, this.gaussHermitePoints, this.epsilon_utility, this.budget, withinTmax, finalError, timeoutCounter, nexToOpt, intervalTime);

   }


   private Queue<C> sort(Queue<C> feasibleSet) {
      Configuration[] set = new Configuration[feasibleSet.size()];

      Queue<C> results = new ConcurrentLinkedQueue<C>();

      int i = 0;
      for (C o : feasibleSet) {
         set[i] = o;
         i++;
      }

      /* do quickSort */
      quickSort(0, set.length - 1, (C[]) set);

      i = 0;
      while (i < set.length) {
         results.add((C) set[i]);
         i++;
      }

      return results;
   }

   private void quickSort(int lowerIndex, int higherIndex, C array[]) {
      int i = lowerIndex;
      int j = higherIndex;
      // calculate pivot number, I am taking pivot as middle index number
      int pivot = array[lowerIndex + (higherIndex - lowerIndex) / 2].hashCode();

      if (i < j) {
         // Divide into two arrays
         while (i <= j) {
            /**
             * In each iteration, we will identify a number from left side which
             * is greater then the pivot value, and also we will identify a number
             * from right side which is less then the pivot value. Once the search
             * is done, then we exchange both numbers.
             */
            while (array[i].hashCode() < pivot) {
               i++;
            }
            while (array[j].hashCode() > pivot) {
               j--;
            }
            if (i <= j) {
               C temp = array[i];
               array[i] = array[j];
               array[j] = temp;
               //move index to next position on both sides
               i++;
               j--;
            }
         }
         // call quickSort() method recursively
         if (lowerIndex < j)
            quickSort(lowerIndex, j, array);
         if (i < higherIndex)
            quickSort(i, higherIndex, array);
      } else
         return;
   }

   private Queue<ConfigUtility<C>> sortCU(Queue<ConfigUtility<C>> queue) {
      ConfigUtility<C> set[] = new ConfigUtility[queue.size()];

      Queue<ConfigUtility<C>> results = new ConcurrentLinkedQueue<ConfigUtility<C>>();

      int i = 0;
      for (ConfigUtility<C> o : queue) {
         set[i] = o;
         i++;
      }

      /* do quickSort */
      quickSortCU(0, set.length - 1, set);

      i = 0;
      while (i < set.length) {
         results.add(set[i]);
         i++;
      }

      return results;
   }

   private void quickSortCU(int lowerIndex, int higherIndex, ConfigUtility<C> array[]) {
      int i = lowerIndex;
      int j = higherIndex;
      // calculate pivot number, I am taking pivot as middle index number
      int pivot = array[lowerIndex + (higherIndex - lowerIndex) / 2].getConfiguration().hashCode();

      if (i < j) {
         // Divide into two arrays
         while (i <= j) {
            /**
             * In each iteration, we will identify a number from left side which
             * is greater then the pivot value, and also we will identify a number
             * from right side which is less then the pivot value. Once the search
             * is done, then we exchange both numbers.
             */
            while (array[i].getConfiguration().hashCode() < pivot) {
               i++;
            }
            while (array[j].getConfiguration().hashCode() > pivot) {
               j--;
            }
            if (i <= j) {
               ConfigUtility<C> temp = array[i];
               array[i] = array[j];
               array[j] = temp;
               //move index to next position on both sides
               i++;
               j--;
            }
         }
         // call quickSort() method recursively
         if (lowerIndex < j)
            quickSortCU(lowerIndex, j, array);
         if (i < higherIndex)
            quickSortCU(i, higherIndex, array);
      } else
         return;
   }


   /*
   //TODO: our policy is such that if horizon == 0, we are greedy
   However, in the real exploration, we are never greedy.
   If budget == num expl that's easy: when budget = 1, be greedy
   For us it's different, bc we don't know exactly when it's our last iteration


   How can we know that?
   Idea 1.  We only give the budget for the *exploration* phase. The cost for the last
   recommendation is out of the budget.
   Then, you query the model and get the highest predictive mean.
   If the predicted value is better than the best known, you setupConfig it.
   Clearly, if it turns out to be worse, then you rollback to the best known.
   The downside is that you naturally go over the budget.
   And cherrypick does not have such a step

   Idea 2.
   Run the simulated step and get the corresponding config.
   Simulate the exploration of the config, decreasing the budget
   Simulate a greedy step. See how much money you are left with.
   Are you


   Idea 3. We leave as it is, but there's a discrepancy between the utility used
   In the real world    ===> I don't want this

   Idea 4. Remove the greedy step
   I think the greedy step in general makes sense. EI mixes exploration and exploitation
   but at any iteration K, you don't know whether you have "explored or exploited" in the previous
   iterations. It can be that you have explored a bunch of points with high variance but that
   turned out to be not good.
   Then, a final greedy step is useful.
   Note that, for example, cherrypick fixes a minimum number of iterations before allowing the
   thing to stop. This is not only to have a minimum number of samples (you could do that by sampling at random
   at the beginning), but also to increases the chance that you have performed an exploitation
   step in which you have sampled a potentially good configuration.


   IN GENERAL Note that since our budget can go to 0 before we reach h = 0, it's possible that
   our simulation does not run the greedy step ever.
   That's one more reason to just say: we give budget to train the model.
   Get the recommendation



   BTW: The point is that for some reason I was only doing the greedy step in the simulation and not
   in the real life exploration



   //// EARLY STOP
   I guess we can play with that, but in general the early stop condition
   is a heuristic. E.g., CP fixes 10% and min N = 6
    */

   public static final boolean retrain_in_depth = false;

   private OptResult lynceus(long seed) {
      System.out.println("Retraining in depth = " + retrain_in_depth);
      /* keep track of the errors of the linear model for estimating cost */
      seed = 45;
      this.seed = seed;

      long startTime = System.currentTimeMillis();

      double error = 0.0;
      double finalError = 0.0;   // average of the errors of all explorations of the same seed

      final TestSet<C, M> allTestConfigsNoTarget = this.fullTestSet();

      this.testSet = initialTestSet();
      //System.out.println("Pritingin init test");
      //this.testSet.printAll();
      this.searchSpaceCardinality = this.testSet.size();

      State<C, M> currState;

      String timeout = Main.timeoutToStr(timeoutType);

      if (Main.bootstrap.equalsIgnoreCase("lhs")) {
         System.out.println("Bootstrapping LHS");
         currState = initLHS(testSet, budget, seed, allTestConfigsNoTarget, startTime);
      } else {
         System.out.println("Bootstrapping RND");
         currState = init(testSet, budget, seed, allTestConfigsNoTarget, startTime);
      }
      System.out.println("Bootstrapping finished");

      if (lyn_print) {
         System.out.println("Seed: " + seed + " ; State after init" + currState);
      }
      if (debug_logs) {
         debugWriter.println("Seed: " + seed);
         debugWriter.println("State after init" + currState);
         debugWriter.flush();
      }


      /* measure time */
      ArrayList<Double> timer = new ArrayList<Double>();
      long start;
      long elapsedTime;


      ConfigUtility<C> configUtility;
      long setupCost;
      SamplingResult<C> samplingResult;
      int iteration = 0;

      double previousUtility = -1;

      //cost model
      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(),currState.getTestSet(), seed);
      model.train();

      PredictiveModel<C, M> modelAccuracy = buildPredictiveModel(currState.getTrainingSetAccuracy(), currState.getParamsAcc(),currState.getTestSet(), seed);
      modelAccuracy.train();

      C initBest = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      if(penalty_cost)
    	  initBest = bestInTrain(currState.getTrainingSet(), T_max).getFst();

      double initDist = distanceFromOpt(allTestConfigsNoTarget, initBest, T_max);


      //C initBest = bestInTrain(currState.getTrainingSet(), T_max).getFst();

      /* while there are unexplored configs and we have budget ==> explore */
      int ite = 0;
      while (currState.getTestSet().size() > 0 && currState.getBudget() > 0) {
         currState.resetParams(); //This is done at the beginning of each iteration
         currState.resetParamsAcc(); //This is done at the beginning of each iteration

         model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(),currState.getTestSet(), seed);
         model.train();

         modelAccuracy = buildPredictiveModel(currState.getTrainingSetAccuracy(), currState.getParamsAcc(),currState.getTestSet(), seed);
         modelAccuracy.train();

         WekaGaussianProcess wekaGaussianProcess = (WekaGaussianProcess) model;
         WekaGaussianProcess wekaGaussianProcessAcc = (WekaGaussianProcess) modelAccuracy;

         if (wekaGaussianProcess.peekClassifier() instanceof CustomGP && !retrain_in_depth) {
            CustomGP gp = (CustomGP) wekaGaussianProcess.peekClassifier();
            currState.setParams(gp.getModelParams());
         }

         if (wekaGaussianProcessAcc.peekClassifier() instanceof CustomGP && !retrain_in_depth) {
             CustomGP gp = (CustomGP) wekaGaussianProcessAcc.peekClassifier();
             currState.setParamsAcc(gp.getModelParams());
          }

         if (_trace_eval_) {
            evalOnTest(currState, iteration);
         }
         //System.out.println(">>>>>>>> " + System.currentTimeMillis() + " Exploration " + (++ite));
         //Reset the params so that they get updated.

         if (this.opt_type != optimizer.LYNCEUS_APPROX) {

            if (true) {  //Print data for offline analysis
               //System.out.println("Training set b/f nextConfig ");
               try {
            	   //System.out.println("fdskjdfgiagadsfu");
                  NominalToBinary f = new NominalToBinary();
                  f.setInputFormat(currState.getTrainingSet().instances());
                  Instances train = Filter.useFilter(currState.getTrainingSet().instances(), f);
                  train.setRelationName("job_cost");
                  train.setClassIndex(train.numAttributes() - 1);
                  //System.out.println(train);

                  Instances test = ((AbstractConfigWekaTestSet) currState.getTestSet()).getInstancesCopy();
                  for (Instance i : test) {
                     Configuration cc = ((AbstractConfigWekaTestSet) currState.getTestSet()).buildFromInstance(i);
                     double y = costGenerator.deploymentCost(null, (C) cc);
                     i.setClassValue(y);
                  }

                  test = Filter.useFilter(test, f);

                  //System.out.println(test);

                  //System.exit(-1);
               } catch (Exception e) {
                  throw new RuntimeException(e);
               }
            }


            start = System.currentTimeMillis();
            configUtility = nextConfig(currState, this.horizon, seed);
            elapsedTime = System.currentTimeMillis() - start;
            timer.add(elapsedTime / 1000.0);
         } else {
            if (true) throw new UnsupportedOperationException("Lynceus approx is not supported as of now");
            configUtility = approxNextConfig(currState, this.horizon);
         }
         if (configUtility == null || configUtility.getConfiguration() == null) {
            break;
         }
         final double currentUtility = configUtility.getUtility();

         //final Pair<C, Double> bestInTrain = bestInTrain(currState.getTrainingSet(), T_max);

         /* Why do we retrain? We've not updated the training set
         currState.resetParams();
         model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams());
         model.train();
         wekaGaussianProcess = (WekaGaussianProcess) model;
         if (!retrain_in_depth && wekaGaussianProcess.peekClassifier() instanceof CustomGP) {
            CustomGP gp = (CustomGP) wekaGaussianProcess.peekClassifier();
            currState.setParams(gp.getModelParams());
         }
         */


         final Pair<C, Double> bestInTrain;
         if (penalty_cost) {
        	 bestInTrain = bestInTrain(currState.getTrainingSet(), T_max);
         }else {
        	 bestInTrain = bestInTrain(currState.getTrainingSet(), T_max, model);
         }


         final double improvement = (configUtility.getUtility()) / bestInTrain.getSnd();

         if (previousUtility >= 0) {
            if (improvement < epsilon_utility) {
               this.consecutiveAcceptableUtilities++;
               if (improvement < epsilon_utility && this.consecutiveAcceptableUtilities > minAcceptableUtilities) {
                  if (lyn_print) {
                     System.out.println("Predicted U is " + currentUtility + " best in train is " + bestInTrain.getSnd() + "  improvement is only " + improvement + ". Stopping");
                  }
                  break;
               }
            } else {
               consecutiveAcceptableUtilities = 0;
            }
         }
         if (lyn_print) {
            System.out.println("Consecutive decreases " + this.consecutiveAcceptableUtilities + " Predicted U is " + currentUtility + " prev was " + previousUtility + " curr best is " + bestInTrain.getSnd() + " improvement is " + improvement + ". NOT Stopping");
         }
         if (debug_logs) {
            debugWriter.println("#########################################################################");
            debugWriter.println("Consecutive decreases " + this.consecutiveAcceptableUtilities + " Predicted U is " + currentUtility + " prev was " + previousUtility + " curr best is " + bestInTrain.getSnd() + " improvement is " + improvement + ". NOT Stopping");
            debugWriter.flush();
         }
         previousUtility = currentUtility;


	   	  double[] muCost = new double[currState.getTestSet().size()];
	   	  double[] sigmaCost = new double[currState.getTestSet().size()];
	   	  double[] muAcc = new double[currState.getTestSet().size()];
	   	  double[] sigmaAcc = new double[currState.getTestSet().size()];
	   	  double currMuCost = 0, currSigmaCost = 0;
	   	  double currMuAcc = 0, currSigmaAcc = 0;

	      model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(),currState.getTestSet(), seed);
	      model.train();

	      modelAccuracy = buildPredictiveModel(currState.getTrainingSetAccuracy(), currState.getParamsAcc(), currState.getTestSet(), seed);
	      modelAccuracy.train();

	      this.avgMuCost = 0;
	      this.avgSigmaCost = 0;
	      this.avgMuAcc = 0;
	      this.avgSigmaAcc = 0;

	      for (int i = 0; i < currState.getTestSet().size(); i++) {
	    	  currMuCost = model.evaluate(currState.getTestSet().getConfig(i));
	    	  currSigmaCost = model.stdv(currState.getTestSet().getConfig(i));

	    	  currMuAcc = modelAccuracy.evaluate(currState.getTestSet().getConfig(i));
	    	  currSigmaAcc = modelAccuracy.stdv(currState.getTestSet().getConfig(i));

	    	  muCost[i] = currMuCost;
	    	  sigmaCost[i] = currSigmaCost;
	    	  muAcc[i] = currMuAcc;
	    	  sigmaAcc[i] = currSigmaAcc;
	    	  this.avgMuCost += currMuCost;
	    	  this.avgSigmaCost += currSigmaCost;
	    	  this.avgMuAcc += currMuAcc;
	    	  this.avgSigmaAcc += currSigmaAcc;
	      }

	      this.avgMuCost /= currState.getTestSet().size();
	      this.avgSigmaCost /= currState.getTestSet().size();
	      this.avgMuAcc /= currState.getTestSet().size();
	      this.avgSigmaAcc /= currState.getTestSet().size();


	      Arrays.sort(muCost);
	      Arrays.sort(sigmaCost);
	      Arrays.sort(muAcc);
	      Arrays.sort(sigmaAcc);

	      int index_50 = (int) Math.floor((50.0 / 100) * currState.getTestSet().size());
	      if (index_50 == currState.getTestSet().size())
	         index_50 = currState.getTestSet().size() - 2; //not the max
	      if (currState.getTestSet().size() <= 2)
	    	  index_50 = (int) Math.floor(currState.getTestSet().size() / 2);

	      this.mu50Cost = muCost[index_50];
	      this.sigma50Cost = sigmaCost[index_50];
	      this.mu50Acc = muAcc[index_50];
	      this.sigma50Acc = sigmaAcc[index_50];


	      int index_90 = (int) Math.floor((90.0 / 100) * currState.getTestSet().size());
	      if (index_90 == currState.getTestSet().size())
	         index_90 = currState.getTestSet().size() - 2; //not the max
	      if (currState.getTestSet().size() <= 2)
	    	  index_90 = (int) Math.floor(currState.getTestSet().size() / 2);

	      this.mu90Cost = muCost[index_90];
	      this.sigma90Cost = sigmaCost[index_90];
	      this.mu90Acc = muAcc[index_90];
	      this.sigma90Acc = sigmaAcc[index_90];




         if(penalty_cost) {
        	 if(model.maxVariance(testSet) > this.maxVar)
        		 this.maxVar = model.maxVariance(testSet);
         }

         final C nextConfig = configUtility.getConfiguration();
         setupCost = setupConfig(nextConfig, currState, this.costGenerator);
         samplingResult = sample(nextConfig, costGenerator, currState);
         updateState(currState, setupCost, samplingResult);

         if(currState.getBudget() <= 0) break;

         double costConfig = costGenerator.deploymentCost(null, nextConfig);
         double runningTime = costConfig / costGenerator.costPerConfigPerMinute(nextConfig); // in minutes
         this.timeExp += (runningTime*60.0);	//in seconds
         double accConfig = costGenerator.getAccForConfig(nextConfig);

         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);

         if (runLogs) {
           double actTime =  ((System.currentTimeMillis()-startTime)/1000.0 + this.timeExp)/60.0; //in MINS
        	 //runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO");
        	 runLogsWriter.println(seed + ";" + Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout +  ";0;0.0;0;" + maxTimePerc + ";" + cumulativeExplorations + ";" + nextConfig.toString() + ";" + nextDFO + ";" + currDFO + ";" + runningTime +  ";" + accConfig + ";" + costConfig + ";" +  currentUtility + ";" + this.avgMuCost + ";" + this.avgSigmaCost + ";" + this.avgEI + ";" + this.mu50Cost + ";" + this.sigma50Cost + ";" + this.ei50 + ";" + this.mu90Cost + ";" + this.sigma90Cost + ";" + this.ei90 + ";" + this.avgMuAcc + ";" + this.avgSigmaAcc + ";" + this.mu50Acc + ";" + this.sigma50Acc + ";" + this.mu90Acc + ";" + this.sigma90Acc + ";" + actTime);
        	 runLogsWriter.flush();
         }


         // better config was found. Let us save the NEX so far
         if (this.nextDFO < this.currDFO) {
        	 if(accConfig >= AccConstrain && runningTime <= T_max)
        		 this.currDFO = this.nextDFO;
        	 this.nexToOpt = (int) cumulativeExplorations;
         }

         if (lyn_print) {
            System.out.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
         }
         if (debug_logs) {
            debugWriter.println("Updated state " + currState + " after sampling " + nextConfig + " with running cost ($) " + samplingResult.getExecutionRealCost());
            debugWriter.println("#########################################################################");
            debugWriter.flush();
         }
         iteration++;

      }

      C ret = bestInTrain(currState.getTrainingSet(), T_max, model).getFst();
      if(penalty_cost)
    	  ret = bestInTrain(currState.getTrainingSet(), T_max).getFst();

      double dist = distanceFromOpt(allTestConfigsNoTarget, ret, T_max);
      double costPerUnitOfTime = costGenerator.costPerConfigPerMinute(ret);
      double time = costGenerator.deploymentCost(null, ret) / costPerUnitOfTime; //in seconds
      C groundBest = bestConfigGroundTruth(allTestConfigsNoTarget, T_max);

      double costPerTimePerBest=0;
      double timeBest = 0;
      if(costGenerator instanceof TensorflowConfigCostGenerator) {
	      costPerTimePerBest = ((TensorflowConfigCostGenerator) costGenerator).dataset_all.costPerConfigPerMinute((TensorflowConfigSize) groundBest);
	      timeBest = ((TensorflowConfigCostGenerator) costGenerator).dataset_all.deploymentCost(null, (TensorflowConfigSize) groundBest) / costPerTimePerBest;
      }else {
	      costPerTimePerBest = costGenerator.costPerConfigPerMinute(groundBest);
	      timeBest = costGenerator.deploymentCost(null, groundBest) / costPerTimePerBest;
      }


      boolean withinTmax = time <= T_max;
      System.out.println("Best explored " + ret + " with cost " + (time * costPerUnitOfTime) + " with time " + time + " with constraint " + T_max);
      System.out.println("Ground best " + groundBest + " with cost " + (timeBest * costPerTimePerBest) + " with time " + timeBest + " with constraint " + T_max);


      if (lyn_print) {
         System.out.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         System.out.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
      }
      if (debug_logs) {
         debugWriter.println("Complying with time constraint: " + withinTmax + " ; time = " + time + " ; Tmax = " + T_max);
         debugWriter.println("WKLD " + wkldid + " " + this.opt_type + " Cumulative <cost, #expl> = <" + cumulativeCost + ", " + cumulativeExplorations + "> Returning best " + ret + " with time " + time + " and cost " + (time * costPerUnitOfTime) + " ground best" + groundBest + " with time " + timeBest + " and cost " + (timeBest * costPerTimePerBest) + " Dist_from_opt " + dist + " budget " + budget + " epsilon " + epsilon_utility);
         debugWriter.flush();
      }
      /* compute avg time to get next config */
      int time_samples = timer.size();
      int counter = 0;
      double time_sum = 0.0;
      while (counter < time_samples) {
         time_sum += timer.get(counter);
         counter++;
      }
      double avg_time = time_sum / time_samples;
      if (lyn_print) {
         System.out.println("Average time to get next config = " + avg_time + " secs");
      }
      if (debug_logs) {
         debugWriter.println("Average time to get next config = " + avg_time + " secs");
         debugWriter.flush();
      }

      /* compute average error */
      for (int i = 0; i < estimationErrors.size(); i++) {
         finalError += estimationErrors.get(i);
         if (estimationErrors_logs) {
            //estimationErrorsWriter.println("wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;numSeeds;seed;lookahead;gamma;gh;maxTimePerc;estimationError");
            estimationErrorsWriter.println(Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";" + Main.numSeeds + ";" + seed + ";" + horizon + ";" + gamma + ";" + gaussHermitePoints + ";" + maxTimePerc + ";" + estimationErrors.get(i));
            estimationErrorsWriter.flush();
         }
      }
      finalError /= estimationErrors.size();
      //in seconds
      double intervalTime = (System.currentTimeMillis()-startTime)/1000.0 + this.timeExp;

      System.out.println("stdv_counter = " + stdv_counter + " ; stdv_total_counter = " + stdv_total_counter + " ; %fails = " + (float) stdv_counter / (float) stdv_total_counter + " ; final_budget = " + currState.getBudget());
      return new OptResult(this.opt_type, this.cumulativeCost, (long) time, 0, cumulativeExplorations, initDist, dist, ret, seed, initTrainSamples, horizon, this.gaussHermitePoints, this.epsilon_utility, this.budget, withinTmax, finalError, timeoutCounter, nexToOpt, intervalTime);

   }



   /* update training and test sets, as well as, overall cost of exploring and number of explorations */
   private void updateState(State<C, M> currState, double setUpCost, SamplingResult<C> samplingResult) {

      double executionCost = samplingResult.getExecutionCost();
      double transformedExecutionCost = executionCost;
      final double accConf = costGenerator.getAccForConfig(samplingResult.getConfig());

      if(penalty_cost) {

          final double runningTime = costGenerator.deploymentCost(null, samplingResult.getConfig()) / costGenerator.costPerConfigPerMinute(samplingResult.getConfig()); // in minutes
          if(accConf != -1 ){
              if (accConf < AccConstrain  || runningTime > T_max)
          	     transformedExecutionCost += (maxCost + 3*maxVar);
          }
      }

      if (log_transform) {
         transformedExecutionCost = Math.log(transformedExecutionCost);
      }
      if (timeoutType == timeout.FALSE) {

    	 currState.setBudget((currState.getBudget() - setUpCost - executionCost));
    	 if (currState.getBudget() <= 0) {
    		 currState.setBudget(0.0);
    		 System.out.println("No more budget.");

    	 }else {
	         currState.addTrainingSample(samplingResult.getConfig(), transformedExecutionCost);
	         currState.addTrainingSampleAccuracy(samplingResult.getConfig(), accConf);
	         currState.removeTestSample(samplingResult.getConfig());
	         currState.setCurrentConfiguration(samplingResult.getConfig());

	         this.cumulativeCost += setUpCost + executionCost;
	         this.cumulativeExplorations++;
    	 }
      } else {

         //if (true) throw new RuntimeException("NO timeout allowed");
         /* with the timeout, the cost that is paid and the cost that is added to the model are different */
         //TODO: double check the cost we are removing from budget and putting in the training set

         currState.setBudget((currState.getBudget() - setUpCost - samplingResult.getExecutionRealCost()));      // update budget ==> subtract setup cost and runTime cost
    	 if (currState.getBudget() <= 0) {
    		 currState.setBudget(0.0);
    		 System.out.println("No more budget.");
    	 }else {
    		 currState.addTrainingSample(samplingResult.getConfig(), transformedExecutionCost);
    		 //also retrain model

    		 currState.addTrainingSampleAccuracy(samplingResult.getConfig(), accConf);
    		 currState.removeTestSample(samplingResult.getConfig());
    		 currState.setCurrentConfiguration(samplingResult.getConfig());

    		 this.cumulativeCost += setUpCost + samplingResult.getExecutionRealCost();
    		 this.cumulativeExplorations++;
    	 }
      }
   }

   /**
    * try new configuration
    *
    * @return: Sampling result = {predicted cost of the sample, predicted time to train, config, real cost of the
    * sample}
    **/
   protected final SamplingResult<C> sample(C config, CostGenerator<C> costGenerator, State<C, M> state) {
	  int i;

      if (timeoutType == timeout.FALSE) {

          //System.out.println("Next config to sample " + config);
          double runtimeCost = costGenerator.deploymentCost(state, config);
          final double costPerTimeUnit = costGenerator.costPerConfigPerMinute(config);   // $$/minute
          final double timeTaken = runtimeCost / costPerTimeUnit;                  // minutes

          //final double accForConfig = costGenerator.getAccForConfig(config);

          estimationErrors.add(-1.0);


    	  if (penalty_cost) {
    		  if (runtimeCost > maxCost) {
         		 maxCost = runtimeCost;

         		 int counter = 0;
         		 for (i = 0; i < state.getTrainingSet().size()-counter; i++) {
         			 C conf = state.getTrainingSet().getConfig(i).getFst();
         			 double acc = costGenerator.getAccForConfig(conf);
         			 double costConf = costGenerator.deploymentCost(state, conf);
         			 double runningTime =  costConf / costGenerator.costPerConfigPerMinute(conf); // in minutes

         			 //System.out.println(i +"" +conf.toString() +"" + acc);
         			 if (acc == -1) break; //-1 is return always except when running tensorflowSize

         			 if (acc < AccConstrain || runningTime > T_max) {
         				 //update of the cost (with a penalty) on the unfeasible configurations wrt acc
         				 //double originalCost = state.getTrainingSet().getConfig(i).getSnd()-previousCost-3*this.prevMaxVar;
         				 state.removeTrainingSample(conf);
         				 state.addTrainingSample(conf, costConf+maxCost+3*this.maxVar);
         				 counter ++;
         				 i--;
         			 }
         		 }
        		 if (state.getTrainingSet().size() > initTrainSamples) {
        			 final TestSet<C, M> allTestConfigsNoTarget = this.fullTestSet();
        			 for (i = 0; i < state.getTrainingSet().size(); i++) {
        				 C confg = state.getTrainingSet().getConfig(i).getFst();
        				 double actDFO = distanceFromOpt(allTestConfigsNoTarget, confg, T_max);
        				 if (actDFO < this.currDFO) {
        					 this.currDFO = actDFO;
        				 }
        			}
        		 }

    		  }
    	  }else {
        	  if (runtimeCost > maxCost) {
                  maxCost = runtimeCost;
               }
    	  }



         //System.out.println("runtimeCost = " + runtimeCost + " ; costPerTimeUnit = " + costPerTimeUnit + " ; timeTaken = " + timeTaken);

         return new SamplingResult<>(runtimeCost, timeTaken, config, runtimeCost);

      }


      /* with timeout, there are two situations that must be considered:
       * 	- there is no current optimum yet
       * 	- there is already a best config so far
       *
       * to stop the exploration earlier, we will consider 2 regions:
       * 	- region 1: composed of the configs that are too slow (time > MAX_TIME and acc < 0.85)
       * 	- region 2: composed of the configs that are more expensive than the current optimum (cost(c) > cost(c*) and time < MAX_TIME)
       * all configs outside of these regions are possible optimums */

      double realCost;         // how much is really paid
      double bestCostSoFar;

      double costToAcc;     // how much it costs for current config to get acc = 0.85
      double timeToAcc;     // how long it takes for current config to get acc = 0.85 (minutes)
      double costPerTimeUnit = costGenerator.costPerConfigPerMinute(config);   // $$/minute

      double stoppingTime = 0.0;   // time when the exploration was stopped
      double modelCost = 0.0;      // cost with which the model will be updated
      double[] discreteTimes = {0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0};

      double error = 0;
      Pair<C, Double> best = null;
      C bestConfigSoFar;

      if (state.getTrainingSet().size() != 0) {
         PredictiveModel<C, M> model = buildPredictiveModel(state.getTrainingSet(), state.getParams(),state.getTestSet(), this.seed);
         model.train();
         //best = bestInTrain(state.getTrainingSet(), T_max, model);
         best = bestInTrain(state.getTrainingSet(), T_max);
      }

      if (timeoutType != timeout.FALSE && best != null) {
         bestConfigSoFar = (C) best.getFst();                           // best config so far: c*
         bestCostSoFar = costGenerator.deploymentCost(state, bestConfigSoFar);   // cost of the best config so far: cost(c*)

         costToAcc = costGenerator.deploymentCost(state, config);   // money paid until stopping
         timeToAcc = costToAcc / costPerTimeUnit;   // duration of the exploration

         /* current config is a new best */
         if (timeToAcc <= T_max && costToAcc < bestCostSoFar) {
            //System.out.println("New best " +  config);
            realCost = costToAcc;
            modelCost = costToAcc;
         } else {
            /* current config is not new best */
            if (timeToAcc > T_max && costToAcc < bestCostSoFar) {   // stop exploring when time = T_max
               //System.out.println("STOP: time > T_max");
               if (timeoutType == timeout.IDEAL) {
                  stoppingTime = T_max;
               } else {
                  stoppingTime = ((int) Math.ceil((T_max * 60) / 30) * 30) / 60.0;   /* added this for discrete version */
               }
               realCost = stoppingTime * costPerTimeUnit;
            } else if (timeToAcc <= T_max && costToAcc >= bestCostSoFar) {   // stop exploring when cost(config) = cost(c*)
               //System.out.println("STOP: cost(config) > cost(c*)");
               stoppingTime = bestCostSoFar / costPerTimeUnit;
               if (timeoutType == timeout.IDEAL) {
                  realCost = bestCostSoFar;
               } else {  // discrete version
                  if (stoppingTime * 60 < 30) {   // make sure we reach the 1st discrete position of the intermediateValues list
                     stoppingTime = 0.5;
                  } else {
                     //System.out.println("PRE_ROUND: stoppingTime = " + stoppingTime);
                     stoppingTime = discreteTimes[(int) Math.round((stoppingTime * 60) / 30) - 1];
                     //System.out.println("POST_ROUND: stoppingTime = " + stoppingTime);
                  }
                  realCost = stoppingTime * costPerTimeUnit;
               }
            } else {   // time > T_max and cost(config) > cost(c*) ; decide whether to stop due to cost or to time
               double timeToBestCost = bestCostSoFar / costPerTimeUnit;
               if (timeToBestCost <= T_max) {   // stop due to cost
                  //System.out.println("STOP: cost(config) > cost(c*)");
                  stoppingTime = bestCostSoFar / costPerTimeUnit;
                  if (timeoutType == timeout.IDEAL) {
                     realCost = bestCostSoFar;
                  } else {  // discrete version
                     if (stoppingTime * 60 < 30) {   // make sure we reach the 1st discrete position of the intermediateValues list
                        stoppingTime = 0.5;
                     } else {
                        //System.out.println("PRE_ROUND: stoppingTime = " + stoppingTime);
                        stoppingTime = discreteTimes[(int) Math.round((stoppingTime * 60) / 30) - 1];
                        //System.out.println("POST_ROUND: stoppingTime = " + stoppingTime);
                     }
                     realCost = stoppingTime * costPerTimeUnit;
                  }
               } else {   // stop due to time
                  //System.out.println("STOP: time > T_max");
                  if (timeoutType == timeout.IDEAL) {
                     stoppingTime = T_max;
                  } else {
                     stoppingTime = ((int) Math.ceil((T_max * 60) / 30) * 30) / 60.0;   /* added this for discrete version */
                  }
                  realCost = stoppingTime * costPerTimeUnit;
               }
            }

            if (timeoutType == timeout.IDEAL) {
               modelCost = costToAcc;
            } else {
               modelCost = estimateCost(costGenerator, config, stoppingTime, costPerTimeUnit, timeoutType);
               if (modelCost == 0.0) {
                  modelCost = costToAcc;
               }
            }

            /* add the error to the array */
            timeoutCounter++;
            error = Math.abs(costToAcc - modelCost) / costToAcc;
            estimationErrors.add(error);
            //System.out.println("currentAcc = " + costGenerator.getAccForSpecificTime(stoppingTime) + " ; performance = " + costGenerator.getAccForSpecificTime(timeToAcc));
            //System.out.println("realTime = " + stoppingTime + " ; timeToAcc = "  + timeToAcc + " ; estimatedTime = " + modelCost/costPerTimeUnit);
            //System.out.println("realCost=" + realCost + " ; costToAcc=" + costToAcc + " ; estimatedCost=" + modelCost + " ; error=" + error);

         }

         if (lyn_print) {
            System.out.println("bestCost = " + bestCostSoFar + " ; costToAcc = " + costToAcc + " ; realcost = " + realCost + " ; timeToAcc = " + timeToAcc + " ; T_max = " + T_max);
            System.out.println("Sampled " + config + " with cost per time unit ($/min) " + costPerTimeUnit);
         }
         if (debug_logs) {
            System.out.println("bestCost = " + bestCostSoFar + " ; costToAcc = " + costToAcc + " ; realcost = " + realCost + " ; timeToAcc = " + timeToAcc + " ; T_max = " + T_max);
            System.out.println("Sampled " + config + " with cost per time unit ($/min) " + costPerTimeUnit);
         }

         //System.out.println("bestCost = " + bestCostSoFar + " ; costToAcc = " + costToAcc + " ; realcost = " + realCost + " ; modelCost = " + modelCost + " ; timeToAcc = " + timeToAcc + " ; T_max = " + T_max);

      } else {  /* there is no best config yet */
         costToAcc = costGenerator.deploymentCost(state, config);   // money paid until stopping
         timeToAcc = costToAcc / costPerTimeUnit;   // duration of the exploration

         // current config meets the time constraint
         if (timeToAcc <= T_max) {
            //System.out.println("New best " +  config);
            realCost = costToAcc;
            modelCost = costToAcc;
         } else { // time constraint is not met; only run that config for MAX_TIME
            //System.out.println("NO BEST CONFIG YET");
            if (timeoutType == timeout.IDEAL) {
               stoppingTime = T_max;
            } else {
               stoppingTime = ((int) Math.ceil((T_max * 60) / 30) * 30) / 60.0;   /* added this for discrete version */
               modelCost = estimateCost(costGenerator, config, stoppingTime, costPerTimeUnit, timeoutType);
            }
            realCost = stoppingTime * costPerTimeUnit;
            if (modelCost == 0.0) {
               modelCost = costToAcc;
            }

            /* add the error to the array */
            timeoutCounter++;
            error = Math.abs(costToAcc - modelCost) / costToAcc;
            estimationErrors.add(error);
            //System.out.println("realTime = " + stoppingTime + " ; timeToAcc = "  + timeToAcc + " ; estimatedTime = " + modelCost/costPerTimeUnit);
            //System.out.println("realCost=" + realCost + " ; costToAcc=" + costToAcc + " ; estimatedCost=" + modelCost + " ; error=" + error);

         }

         if (lyn_print) {
            System.out.println("Sampled " + config + " with running cost ($) " + realCost + " corresponding to time " + timeToAcc + " min");
         }
         if (debug_logs) {
            debugWriter.println("Sampled " + config + " with running cost ($) " + realCost + " corresponding to time " + timeToAcc + " min");
            debugWriter.flush();
         }
         //System.out.println("costToAcc = " + costToAcc + " ; realcost = " + realCost + " ; modelCost = " + modelCost + " ; timeToAcc = " + timeToAcc + " ; T_max = " + T_max);
      }

      if (timeoutType != timeout.IDEAL) {
         timeToAcc = modelCost / costPerTimeUnit;
      }

      if (penalty_cost){
		  if (modelCost > maxCost) {
      		 maxCost = modelCost;

      		 int counter = 0;
      		 for (i = 0; i < state.getTrainingSet().size()-counter; i++) {
      			 C conf = state.getTrainingSet().getConfig(i).getFst();
      			 double acc = costGenerator.getAccForConfig(conf);
      			 double costConf = costGenerator.deploymentCost(state, conf);
      			 double runningTime =  costConf / costGenerator.costPerConfigPerMinute(conf); // in minutes

      			 //System.out.println(i +"" +conf.toString() +"" + acc);
      			 if (acc == -1) break; //-1 is return always except when running tensorflowSize

      			 if (acc < AccConstrain || runningTime > T_max) {
      				 //update of the cost (with a penalty) on the unfeasible configurations wrt acc
      				 //double originalCost = state.getTrainingSet().getConfig(i).getSnd()-previousCost-3*this.prevMaxVar;
      				 state.removeTrainingSample(conf);
      				 state.addTrainingSample(conf, costConf+maxCost+3*this.maxVar);
      				 counter ++;
      				 i--;
      			 }
      		 }
     		 if (state.getTrainingSet().size() > initTrainSamples) {
     			 final TestSet<C, M> allTestConfigsNoTarget = this.fullTestSet();
     			 for (i = 0; i < state.getTrainingSet().size(); i++) {
     				 C confg = state.getTrainingSet().getConfig(i).getFst();
     				 double actDFO = distanceFromOpt(allTestConfigsNoTarget, confg, T_max);
     				 if (actDFO < this.currDFO) {
     					 this.currDFO = actDFO;
     				 }
     			}
     		 }

 		  }
      }else {
    	  if (modelCost > maxCost) {
    		  maxCost = modelCost;
    	  }
      }

      //System.out.println("Sampled " + config + " with cost per time unit ($/min) " + costPerTimeUnit);

      return new SamplingResult<>(modelCost, timeToAcc, config, realCost);
   }

   private double estimateCost(CostGenerator<C> costGenerator, C config, double stoppingTime, double costPerTimeUnit, timeout timeoutType) {

      double estimatedCost = 0.0;   // estimated cost to reach target acc
      double estimatedTime = 0.0;   // estimated time to reach target acc
      double targetAcc = 0.85;      // target accuracy to stop running the job
      double currentAcc = 0.0;      // accuracy at stopping time;
      double[] xAxis = null;      // intermediate acc values
      double[] yAxis = null;      // time values
      int counter = 0;
      double timeStep = 0.5;      // it is supposed to be 30 secs
      int numPoints = 0;

      currentAcc = costGenerator.getAccForSpecificTime(stoppingTime, config);
      if (currentAcc == 0) {   // this means that there are no intermediate values for the config being used
         return 0;
      } else if (currentAcc >= 0.85) {      // if for the stoppingTime the accuracy is already good enough, then I will not estimate backwards and I will assume the job has finished
         estimatedCost = stoppingTime * costPerTimeUnit;
         //System.out.println("stoppingTime = " + stoppingTime + " ; estimatedTime = " + stoppingTime + " ; currentAcc = " + currentAcc + " ; estimatedCost = " + stoppingTime*costPerTimeUnit);
      } else {
         numPoints = (int) Math.round(stoppingTime / timeStep);
//		   System.out.println("numPoints for regression: " + numPoints + " ; stoppingTime = " + stoppingTime);
         xAxis = costGenerator.getIntermediateValues(config, numPoints, timeoutType);
         yAxis = new double[xAxis.length];

         if (timeoutType != timeout.LOG) {
            yAxis[0] = 0;
         } else {
            yAxis[0] = timeStep;
         }
         counter = 1;
         while (counter < yAxis.length) {
            yAxis[counter] = yAxis[counter - 1] + timeStep;
            counter++;
         }

//		   for (int k = 0; k < yAxis.length; k++) {
//				System.out.println("xAxis[k] =" + xAxis[k]);
//				System.out.println("yAxis[k] =" + yAxis[k]);
//   			}


         switch (timeoutType) {   // I need to use the regressions to estimate when I will reach 85% acc based on the intermediate points I have so far
            // x Axis will have the accuracy and y axis will have the time
            case LIN:
               TrendLine linear;
               if (xAxis.length == 1) {
                  //System.out.println("1 point is not enough for a regression");
               } else if (xAxis.length == 2) {
                  //System.out.println("only one point besides the origin: (" + xAxis[1] + "," + yAxis[1] + ")");
                  linear = new LinTrendLine();
                  linear.setValues(yAxis, xAxis);
                  estimatedTime = linear.predict(0.85);
               } else {
                  linear = new PolyTrendLine(1);
                  linear.setValues(yAxis, xAxis);
                  estimatedTime = linear.predict(0.85);
               }
//		   		   System.out.println("estimatedTime = " + estimatedTime);
               break;

            case POLI:
               TrendLine polinomial = new PolyTrendLine(2);
               polinomial.setValues(yAxis, xAxis);
               estimatedTime = polinomial.predict(0.85);
               break;

            case LOG:
               TrendLine logarithmic = new LogTrendLine();
               logarithmic.setValues(yAxis, xAxis);
               estimatedTime = logarithmic.predict(0.85);
               break;
            default:
               if (lyn_print) {
                  System.out.println("Timeout type " + Main.timeoutToStr(timeoutType) + " does not need estimation for time");
               }
               estimatedTime = stoppingTime;
               break;
         }
         estimatedCost = estimatedTime * costPerTimeUnit;
         //System.out.println("stoppingTime = " + stoppingTime + " ; estimatedTime = " + estimatedTime + " ; currentAcc = " + currentAcc + " ; estimatedCost = " + estimatedCost);
      }
      return estimatedCost;
   }

   private final long setupConfig(C newConfig, State<C, M> currState, CostGenerator<C> costGenerator) {
      long setUpCost = (long) (costGenerator.setupCost(currState, newConfig));
      //long runtimecost = (long) (costGenerator.deploymentCost(currState, newConfig));
      //currState.setBudget(currState.getBudget() - (deploycost));
      currState.setCurrentConfiguration(newConfig);
      //test set has already been done
      //currState.addTrainingSample(newConfig, runtimecost);
      this.cumulativeCost += (setUpCost);
      if (lyn_print) {
         System.out.println("Deployed " + currState + " with deployment cost " + setUpCost);
      }
      if (debug_logs) {
         debugWriter.println("Deployed " + currState + " with deployment cost " + setUpCost);
         debugWriter.flush();
      }
      return (setUpCost);
   }

   private Pair<C, Double> bestInTrain(TrainingSet<C, M> train, double t_max) {
      Pair<C, Double> best = null;
      Pair<C, Double> notComplyingBest = null;
      for (int i = 0; i < train.size(); i++) {
         Pair<C, Double> c = train.getConfig(i);
         double time = c.getSnd() / costGenerator.costPerConfigPerMinute(c.getFst());	//mins
         if (time <= t_max) {
        	 double acc = costGenerator.getAccForConfig(c.getFst());
        	 if (acc == -1) {
 	        	if (best == null || best.getSnd() > c.getSnd())
 	        		best = c;
 	        }else {
 	        	if ((best == null || best.getSnd() > c.getSnd()) && acc >= AccConstrain)
 	        		best = c;
 	        	}
         } else {
            if (notComplyingBest == null || notComplyingBest.getSnd() > c.getSnd()) {
               notComplyingBest = c;
            }
         }
      }
      if (best == null)
         return notComplyingBest;
      return best;
   }


   private Pair<C, Double> bestInTrain(TrainingSet<C, M> train, double t_max, PredictiveModel<C, M> model) {
	      Pair<C, Double> best = null;
	      Pair<C, Double> notComplyingBest = null;
	      Pair<C, Double> maxCostInTrain = new Pair(null, Double.NEGATIVE_INFINITY);

	      if (train.size() == 0) {
	         return null;
	      }

	      for (int i = 0; i < train.size(); i++) {
	         Pair<C, Double> c = train.getConfig(i);
	         double time = c.getSnd() / costGenerator.costPerConfigPerMinute(c.getFst()); //mins

	         // check if config is feasible
	         if (time <= t_max) {
	        	 double acc = costGenerator.getAccForConfig(c.getFst());
	        	 if (acc == -1) {
	 	        	if (best == null || best.getSnd() > c.getSnd())
	 	        		best = c;
	 	        }else {
	 	        	if ((best == null || best.getSnd() > c.getSnd()) && acc >= AccConstrain)
	 	        		best = c;
	 	        	}

	         } else {
	            if (notComplyingBest == null || notComplyingBest.getSnd() > c.getSnd()) {
	               notComplyingBest = c;
	            }
	         }

	         if (c.getSnd() > maxCostInTrain.getSnd()) {
	            maxCostInTrain = c;
	         }

	      }
	      if (best == null) {
	         return new Pair(maxCostInTrain.getFst(), maxCostInTrain.getSnd() + 3 * model.maxVariance(testSet));
	      }
	      return best;
	   }



   private Pair<C, Double> bestInTest(TestSet<C, M> test, double t_max) {
      Pair<C, Double> best = null;
      Pair<C, Double> notComplyingBest = null;
      int i;

      for (i = 0; i < test.size(); i++) {
         C tc = test.getConfig(i);
         Pair<C, Double> c = new Pair<>(tc, costGenerator.deploymentCost(null, tc));
         double time = c.getSnd() / costGenerator.costPerConfigPerMinute(c.getFst());	//mins

         if (time <= t_max) {
        	double acc = costGenerator.getAccForConfig(tc);
	        if (acc == -1) {
	        	if (best == null || best.getSnd() > c.getSnd())
	        		best = c;
	        }else {
	        	if ((best == null || best.getSnd() > c.getSnd()) && acc >= AccConstrain)
	        		best = c;
	        	}
         } else {
        	 if (notComplyingBest == null || notComplyingBest.getSnd() > c.getSnd()) {
        		 notComplyingBest = c;
        	 }
         }
      }

      if (best == null) {
	         if (true)
	            throw new RuntimeException("No optimal config within constraint found. This should not happen because we always set tmax at least as high as the minimum in the test set");
	         return notComplyingBest;
	      }
	      return best;
   }



   final static boolean skip_cost_check = false;

   static {
      if (skip_cost_check)
         System.out.println(">>>> SKIPPING COST CHECK IN POPULATE <<<<<<");
   }

   private void populateWithFeasibleByCost(Queue<C> feasibleSet, State<C, M> currState, PredictiveModel<C, M> model,PredictiveModel<C, M> modelAccuracy, double stdvRange, boolean firstTime) {
      //System.out.println("CURR_TRAIN \n" + currState.getTrainingSet().instances());

      for (int i = 0; i < currState.getTestSet().size(); i++) {
         C config = currState.getTestSet().getConfig(i);
         double cost = model.evaluate(config);   // mean of the model cost
         double accuracy = modelAccuracy.evaluate(config);	// mean of the model accuracy

         double sigma_cost = model.stdv(config);      // standard deviation of the model
         double cost99 = cost + stdvRange * sigma_cost;
         double cost90 = cost + 1.282 * sigma_cost;

         double sigma_acc = modelAccuracy.stdv(config);      // standard deviation of the model
         double acc99 = accuracy - stdvRange * sigma_acc;
         double acc90 = accuracy - 1.282 * sigma_acc;

         double time = cost/this.costGenerator.costPerConfigPerMinute(config);
         double sigma_time = sigma_cost / this.costGenerator.costPerConfigPerMinute(config);
         double time90 = time + 1.282 * sigma_time;

         if (log_transform) {
            cost99 = Math.exp(cost99);
            //acc99 = Math.exp(acc99);
         }
         if (false) {
            System.out.println("Budget: " + currState.getBudget() + " Mean " + cost + " sigma " + sigma_cost + " total (transformed if needed) " + cost99 + " untransformed cost " + Math.exp(cost99));
         }

         //we exclude configs that are predicted to not meet the accuracy constrain
         //TODO: and time constrain
         //if (firstTime ==  true) {
	      if(accuracy >= AccConstrain && cost <= currState.getBudget() && time <= T_max) {
	        	 feasibleSet.add(config);
	         }
	     //}else {
	      //   if(cost <= currState.getBudget()) {
	      // 	 feasibleSet.add(config);
	      //  	 coefficientVariationList.add(sigma_acc/accuracy);
	       //  }
	        	 
	     //}
         //if (skip_cost_check || cost99 < currState.getBudget()) {
         //	 feasibleSet.add(config);
         //}

         if (debug_logs) {
            debugWriter.println("[populateWithFeasibleByCost] C: " + config + " cost99 (transformed if needed) =" + cost99 + " cost = " + cost + " stdv = " + sigma_cost);
            debugWriter.flush();
         }

         /* trying an alternative */
//         if (sigma == 0) {
//             //System.out.println("STDV is zero.");
//             //TODO: should we do something specific when std = 0?
//             sigma = 0.000000000000000000001;
//          }
//         final NormalDistribution distribution = new NormalDistribution(cost, sigma);
//         double cost_per_unit = costGenerator.costPerConfigPerMinute(config);
//         double max_cost = T_max * cost_per_unit;
//         double p_feasible = distribution.cumulativeProbability(max_cost);	// returns P(cost <= max_cost)
//
//         if (p_feasible >= 0.90) {
//        	 //System.out.println(p_feasible);
//        	 feasibleSet.add(config);
//         }
//         if (feasibleSet.isEmpty()) {
//        	 System.out.println("no feasible config");
//         }
      }

      //System.out.println(feasibleSet.size() + " Feasible configs found out of " + currState.getTestSet().size());
   }

   private ConfigUtility<C> nextConfig(State<C, M> currState, long horizon, long seed) {
      //Train the model
      //System.out.println("Evaluating feasible configs with params " + currState.getParams());
      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(),currState.getTestSet(), this.seed);
      model.train();

      PredictiveModel<C, M> modelAccuracy = buildPredictiveModel(currState.getTrainingSetAccuracy(), currState.getParamsAcc(),currState.getTestSet(), this.seed);
      modelAccuracy.train();

      /* Identify the configurations that are within the budget with .99 probability */
      final Queue<C> feasibleSet = new ConcurrentLinkedQueue<>();
      populateWithFeasibleByCost(feasibleSet, currState, model, modelAccuracy, z_99, true);

      Random random = new Random(seed);
      //if(feasibleSet.size() == 0) {
    	//populateWithFeasibleByCost(feasibleSet, currState, model, modelAccuracy, z_99, false);
      //}

      if (feasibleSet.size() == 0) {
    	  feasibleSet.add(currState.getTestSet().getConfig(random.nextInt(currState.getTestSet().size())));
       //  System.out.println("LYN: No more feasible solutions");
        // return null;
      }


      final Queue<ConfigUtility<C>> results = new ConcurrentLinkedQueue<>();
      final Queue<Double> times = new ConcurrentLinkedQueue<>();

      final Pair<C, Double> bestSoFar = bestInTrain(currState.getTrainingSet(), T_max);
      //final Pair<C, Double> bestSoFar = bestInTrain(currState.getTrainingSet(), T_max, model);

      if (THREADS > 1) {
         final int num_t = Math.min(THREADS, feasibleSet.size());
         final Thread[] threads = new Thread[num_t];
         for (int t = 0; t < threads.length; t++) {
            threads[t] = new LynceusThread(feasibleSet, results, currState, horizon, bestSoFar, times);
            threads[t].start();
         }

         for (Thread thread : threads) {
            try {
               thread.join();
            } catch (InterruptedException e) {
               throw new RuntimeException(e);
            }
         }
      } else {
         C next;
         ConfigUtility<C> res;
         long start;
         long elapsedTime;

         while (feasibleSet.size() > 0) {
            next = feasibleSet.remove();
            start = System.currentTimeMillis();
            //System.out.println("Starting utility for " + next);
            res = utility(currState, next, horizon, bestSoFar);
            //System.out.println("Ended utility for " + next);
            if (debug_logs) {
               debugWriter.println("[NextConfig] path: " + res);
               debugWriter.flush();
            }
            elapsedTime = System.currentTimeMillis() - start;
            results.add(res);
            times.add(elapsedTime / 1000.0);
         }
      }

      /* order results */
      Queue<ConfigUtility<C>> orderedResults = sortCU(results);

      /* compute avg time to compute Utility */
      int time_samples = times.size();
      double time_sum = 0.0;
      while (times.size() > 0) {
         time_sum += times.poll();
      }
      double avg_time = time_sum / time_samples;
      if (lyn_print) {
         System.out.println("Average time to compute Utility = " + avg_time + " secs");
      }

      //System.out.println("calling best config utility");
      return bestConfigUtility(orderedResults);
   }

   private ConfigUtility<C> approxNextConfig(State<C, M> currState, long horizon) {
      /*Train the model*/
      final PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(),currState.getTestSet(), this.seed);
      model.train();

      final PredictiveModel<C, M> modelAccuracy = buildPredictiveModel(currState.getTrainingSetAccuracy(), currState.getParamsAcc(),currState.getTestSet(), this.seed);
      modelAccuracy.train();

      /*Identify the configurations that are within the budget with .99 probability */
      final Queue<C> feasibleSet = new ConcurrentLinkedQueue<>();
      populateWithFeasibleByCost(feasibleSet, currState, model, modelAccuracy, z_99, true);
      //if (feasibleSet.size() == 0) {
    //	  populateWithFeasibleByCost(feasibleSet, currState, model, modelAccuracy, z_99, false);
      //}
      if (feasibleSet.size() == 0) {
        System.out.println("No more feasible solutions");
        return null;
      }

      /* Generate the sampled points */
      generateCandidateStartingPointSet(feasibleSet, this.searchSpaceCardinality);
      if (feasibleSet.size() == 0) {
         throw new RuntimeException("No candidates for approx");
      }

      final Queue<ConfigUtility<C>> results = new ConcurrentLinkedQueue<>();
      final Queue<Double> times = new ConcurrentLinkedQueue<>();

      final Pair<C, Double> bestSoFar = bestInTrain(currState.getTrainingSet(), T_max);
      //final Pair<C, Double> bestSoFar = bestInTrain(currState.getTrainingSet(), T_max, model);

      /* Find the EI corresponding to the selected points */
      final int num_t = Math.min(THREADS, feasibleSet.size());
      final Thread[] threads = new Thread[num_t];
      for (int t = 0; t < threads.length; t++) {
         threads[t] = new LynceusThread(feasibleSet, results, currState, horizon, bestSoFar, times);
         threads[t].start();
      }

      for (Thread thread : threads) {
         try {
            thread.join();
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      }


      /* FIT a statistical model on the *Utility* for all points */
      TrainingSet<C, M> trainingSet = emptyTrainingSetForApprox();
      HashMap<C, ConfigUtility<C>> fullResults = new HashMap<>();
      for (ConfigUtility<C> cu : results) {
         double norm = opt_type.equals(optimizer.LYNCEUS) ? cu.getCost() : 1.0D;
         double u = cu.getUtility() / norm;
         trainingSet.add(cu.getConfiguration(), u);
         fullResults.put(cu.getConfiguration(), cu);
      }
      PredictiveModel<C, M> approxModel = buildPredictiveModelForApprox(trainingSet);
      approxModel.train();

      /* Now find the configuration with the best predicted *Utility* */
      double opt = 0;
      C best = null;

      ConfigUtility<C> curr;
      for (int ci = 0; ci < currState.getTestSet().size(); ci++) {
         C test = currState.getTestSet().getConfig(ci);
         double predU;
         //If you have tried the config, then use the "real value"
         if ((curr = fullResults.get(test)) != null) {
            double norm = opt_type.equals(optimizer.LYNCEUS) ? curr.getCost() : 1.0D;
            predU = curr.getUtility() / norm;
         } else {  //use the predicted one
            predU = approxModel.evaluate(test);
         }
         if (best == null || opt > predU) {
            best = test;
            opt = predU;
         }
      }


      if (fullResults.containsKey(best)) {
         return fullResults.get(best);
      }

      /* compute avg time to compute Utility */
      int time_samples = times.size();
      double time_sum = 0.0;
      while (times.poll() != null) {
         time_sum += times.poll();
      }
      double avg_time = time_sum / time_samples;
      if (lyn_print) {
         System.out.println("Average time to compute Utility = " + avg_time + " secs");
      }

      /* Run the simulation for the predicted optimal config to get a compatible output*/
      feasibleSet.clear();
      feasibleSet.add(best);
      results.clear();
      times.clear();
      LynceusThread lt = new LynceusThread(feasibleSet, results, currState, horizon, bestSoFar, times);
      lt.start();
      try {
         lt.join();
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
      return results.remove();
   }

   /**
    * Given a set of results, take the one with the best utility. The definition of "best" utility depends on the
    * optimizer
    *
    * @param results
    * @return
    */
   private ConfigUtility<C> bestConfigUtility(Queue<ConfigUtility<C>> results) {
      //We return best utility divided by cost
      double opt = 0;
      ConfigUtility<C> best = null;
      double[] ei = new double[results.size()];
      this.avgEI = 0;

      int i = 0;

      for (ConfigUtility<C> cu : results) {
         /*if (cu.getCost() <= 0) {
            System.out.println(cu.getConfiguration() + " has a negative cost. Filtering it out. But check it out.");
            continue; //we filter out
         }
         */
         double norm = opt_type.equals(optimizer.LYNCEUS) ? cu.getCost() : 1.0D;
         double u = cu.getUtility() / norm;
         //System.out.println("BC: C " + cu.getConfiguration() + " EI " + cu.getUtility() + " C " + cu.getCost() * 100D + " U " + u);
         if (best == null || opt <= u) {    //higher is better
            //System.out.println("best = " + best + " ; opt = " + opt + " ; u = " + u + " ; cu = " + cu);
            best = cu;
            opt = u;
         }
         this.avgEI += u;
         ei[i] = u;
         i++;
      }

      this.avgEI /= results.size();
      Arrays.sort(ei);

      int index_50 = (int) Math.floor((50.0 / 100) * results.size());
      if (index_50 == results.size())
         index_50 = results.size() - 2; //not the max

      this.ei50 = ei[index_50];

      int index_90 = (int) Math.floor((90.0 / 100) * results.size());
      if (index_90 == results.size())
         index_90 = results.size() - 2; //not the max

      this.ei90 = ei[index_90];

      return best;
   }


   /**
    * Compute the expected improvement of a configuration given an icumbent
    *
    * @param c
    * @param model
    * @param bestSoFar
    * @param print
    * @return
    */
   private Triple<Double, Double, Double> ExpectedImprovement(C c, PredictiveModel<C, M> model, Pair<C, Double> bestSoFar, boolean print) {
      final double y_min = bestSoFar.getSnd();
      final double mu_x = model.evaluate(c);
      double s_x = model.stdv(c);

      //This is never happening apparently: tr uses s_x at the denominator
      if (s_x == 0) {
         //System.out.println("STDV is zero.");
         stdv_counter++;
         if (debug_logs) {
            debugWriter.println("[constrainedExpectedImprovement] sigma is zero");
            debugWriter.flush();
         }
         //TODO: should we do something specific when std = 0?
         s_x = 0.000000000000000000001;
      }

      final double u = (y_min - mu_x) / s_x;
      final NormalDistribution standardN = new NormalDistribution(); // Create a normal distribution with mu = 0 and stdev = 1
      final double FI = standardN.cumulativeProbability(u);          // CDF of the standard normal distribution
      final double fi = standardN.density(u);                   // PDF of the standard normal distribution
      final double ei = s_x * (fi + u * FI);                   // ei = stdv * (u * CDF + PDF)

      if (print) {
         System.out.println("EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei + " " + c);
      }
      if (debug_logs) {
         debugWriter.println(" --- EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei + " " + c);
         debugWriter.flush();
      }
      return new Triple<>(ei, mu_x, s_x);
   }

   private Triple<Double, Double, Double> ExpectedImprovement(C c, double mu_x, double s_x, Pair<C, Double> bestSoFar, boolean print) {
      stdv_total_counter++;
      final double y_min = bestSoFar.getSnd();
      if (s_x == 0.0) {
         //System.out.println("sigma is zero");
         stdv_counter++;
         if (debug_logs) {
            debugWriter.println("[ExpectedImprovement] XPTO ---  sigma is zero --- ");
            debugWriter.flush();
         }
      }
      final double u = (y_min - mu_x) / s_x;
      final NormalDistribution standardN = new NormalDistribution(); // Create a normal distribution with mu = 0 and stdev = 1
      final double FI = standardN.cumulativeProbability(u);          // CDF of the standard normal distribution
      final double fi = standardN.density(u);                   // PDF of the standard normal distribution
      final double ei = s_x * (fi + u * FI);                   // ei = stdv * (u * CDF + PDF)

      if (print) {
         System.out.println("EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei);
      }
      if (debug_logs) {
         debugWriter.println("C: " + c + " EI: u " + u + " fi " + fi + " FI " + FI + " best " + y_min + " mean " + mu_x + " stdv " + s_x + " ei " + ei);
         debugWriter.flush();
      }
      return new Triple<>(ei, mu_x, s_x);
   }

   /**
    * Compute the GP-UCB improvement over the incumbent
    *
    * @param c
    * @param model
    * @param bestSoFar
    * @param print
    * @return
    */
   private Triple<Double, Double, Double> UCBImprovement(C c, PredictiveModel<C, M> model, Pair<C, Double> bestSoFar, boolean print) {
      //We compute beta given delta following Theorem  1 in     https://arxiv.org/pdf/0912.3995.pdf
      final double t = cumulativeExplorations + 1; //
      assert searchSpaceCardinality > 0;
      final double card_D = searchSpaceCardinality;
      final double beta_t = 2 * Math.log(card_D * t * t * Math.PI * Math.PI / (6 * delta));
      final double mu_x = model.evaluate(c);
      final double s_x = model.stdv(c);
      return new Triple<>(mu_x - beta_t * s_x, mu_x, s_x);
   }


   private Triple<Double, Double, Double> constrainedExpectedImprovement(C c, double mu_x, double s_x, Pair<C, Double> bestSoFar, boolean print) {

      if (true) {
         throw new RuntimeException("This should be unused");
      }
      Triple<Double, Double, Double> tr;
      if (this.opt_type == optimizer.CHERRYPICK_UCB) {
         throw new UnsupportedOperationException("UCB not supported");
      } else {
         tr = ExpectedImprovement(c, mu_x, s_x, bestSoFar, print);
      }
      final double ei = tr.fst;
      if (s_x == 0) {
         //System.out.println("STDV is zero.");
         //TODO: should we do something specific when std = 0?
         s_x = 0.000000000000000000001;
      }
      final NormalDistribution distribution = new NormalDistribution(mu_x, s_x);
      double cost_per_unit = costGenerator.costPerConfigPerMinute(c);
      double max_cost = T_max * cost_per_unit;
      double p_feasible = distribution.cumulativeProbability(max_cost);   // returns P(cost <= max_cost)


      double improvement;
      if (this.opt_type == optimizer.LYNCEUS_PURCIARO || this.opt_type == optimizer.CHERRYPICK_PURCIARO) {
         improvement = ei / mu_x;
      } else {
         improvement = ei;
      }

      return new Triple<>(improvement * p_feasible, improvement, p_feasible);
      //return new Triple<>(improvement, improvement, p_feasible);
   }

   /**
    * Compute the constrained expected improvement. The expected improvement can be plain EI or its "UCB version"
    *
    * @param c
    * @param model
    * @param bestSoFar
    * @param print
    * @return
    */
   private Triple<Double, Double, Double> constrainedExpectedImprovement(C c, PredictiveModel<C, M> model, PredictiveModel<C, M> AccModel,Pair<C, Double> bestSoFar, boolean print) {

      stdv_total_counter++;

      Triple<Double, Double, Double> tr;
      if (this.opt_type == optimizer.CHERRYPICK_UCB) {
         tr = UCBImprovement(c, model, bestSoFar, print);
         throw new UnsupportedOperationException("NO UCB");
      } else {
         tr = ExpectedImprovement(c, model, bestSoFar, print);
      }
      final double ei = tr.fst;
      final double mu_x = tr.snd;   // mean of the model
      double s_x = tr.trd;         // stdv of the model

      final double mu_acc = AccModel.evaluate(c);
      double s_acc = AccModel.stdv(c);
      final NormalDistribution distributionAcc = new NormalDistribution(mu_acc, s_acc);
      double p_feasibleAcc = 1 - distributionAcc.cumulativeProbability(AccConstrain);

      //System.out.println("CP EIc fo " + c + " mu " + mu_x + " s_x " + s_x);

      //NB: apache's Normal distribution takes mean and STDV as input
      //The stdv has to be > 0 otherwise the NormalDistribution freaks out

      /* ****************************************************************
       * NOTE: our predictive model does *not* predict runtime but cost *
       *	   So, how can we know if the RUNTIME is below T_max?		*
       * Solution:														*
       *		Instead of checking that runtime < t_max we check		*
       *		that  runtime * cost_of_config < t_max * cost_of_config	*
       ******************************************************************/

      final NormalDistribution distribution = new NormalDistribution(mu_x, s_x);   // we assume that the predictions of the model follow a normal distribution
      //double max_cost = T_max * costGenerator.deploymentCost(null, c);
      double cost_per_unit = costGenerator.costPerConfigPerMinute(c);
      double max_cost = T_max * cost_per_unit;
      if (log_transform) {
         max_cost = Math.log(max_cost);
      }
      double p_feasible = distribution.cumulativeProbability(max_cost);  // p_feasible = P(cost <= max_cost)
      // all the configs whose cost is lower than the max cost should satisfy the time constraint

         /*

      2018.06.26
      Why? Let's do the following instead.
      Let's take the predicted cost, divide it by the cost_per-second
      and get the predicted time .
      But I do not have the distribution of the runtime, do I?
      Can i just divide the distribution of cost by the *fixed* cost_per_second?


   ==> not going to do this, but I leave this here for future ref
      */


      double improvement;
      if (this.opt_type == optimizer.LYNCEUS_PURCIARO || this.opt_type == optimizer.CHERRYPICK_PURCIARO) {
         improvement = ei / mu_x;
      } else {
         improvement = ei;
      }

      // EIc(x) = EI(x) * P[T(x) <= Tmax]
      return new Triple<>(improvement * p_feasible * p_feasibleAcc, improvement, p_feasible);
      //return new Triple<>(improvement, improvement, p_feasible);
   }

   //TODO: double check the deployment cost

   /**
    * Computes the long-term utility corresponding to starting a sampling path with initial config "config"
    *
    * @param state
    * @param config
    * @param horizon
    * @param bestSoFar
    * @return
    */
   private ConfigUtility<C> utility(State<C, M> state, C config, long horizon, Pair<C, Double> bestSoFar) {
      //System.out.println(state.getTrainingSet().printAll());
      //System.out.println("{" + horizon + "} " + state + " Utility for " + config);
      //System.out.println("Utility with " + state.getParams());

      final PredictiveModel<C, M> model = buildPredictiveModel(state.getTrainingSet(), state.getParams(),state.getTestSet(), this.seed);
      model.train();

      final PredictiveModel<C, M> modelAccuracy = buildPredictiveModel(state.getTrainingSetAccuracy(), state.getParamsAcc(), state.getTestSet(), this.seed);
      modelAccuracy.train();

      final double avgCost = model.evaluate(config);
      final double stdCost = model.stdv(config);

      final double avgAcc = modelAccuracy.evaluate(config);
      final double stdAcc = modelAccuracy.stdv(config);
      //System.out.println("In utility: " + config + " <" + avgCost + ", " + stdCost + ">");

//      if (stdCost == 0.0)
//         System.out.println("[Lynceus-utility] stdv is zero");

      //System.out.println(state.getTrainingSet().size());
      //model.testOnTrain(state.getTrainingSet());
      double U = 0, C = 0;
      //We add the EIc right away as expected value computed in closed form.
      //System.out.println("Computing EI. BestSoFar " + bestSoFar);
      //final double eic = constrainedExpectedImprovement(config, avgCost, stdCost, bestSoFar, false).fst;
      final double eic = constrainedExpectedImprovement(config, model, modelAccuracy, bestSoFar, false).fst;


      if (lyn_print) {
         System.out.println("[utility] " + config + " ; eic = " + eic + " ; avgCost = " + avgCost + " ; stdCost = " + stdCost);
      }
      if (debug_logs) {
         debugWriter.println("[utility] " + config + " ; eic = " + eic + " ; avgCost = " + avgCost + " ; stdCost = " + stdCost);
         debugWriter.flush();
      }

      //System.out.println(state.getTrainingSet().printAll());
      //System.out.println("EIc " + config + " = " + eic);
      //Add the deployment cost (which only depends on the current state)
      //The runtime cost is added from time to time depending on the GH decomposition
      //final double deployment_cost = costGenerator.deploymentCost(state, config);
      final double setup_cost = costGenerator.setupCost(state, config);

      if (!log_transform) {
         C += (setup_cost + avgCost);
      } else {
         C += Math.exp(avgCost);
      }
      //C += costGenerator.deploymentCost(state, config);	// early version of the code, cost was updated this way
      U += eic;

      /*
      Now EIC already takes into account the specific acquisition function
      if (this.opt_type == optimizer.LYNCEUS_PURCIARO) {
         U += (eic / deployment_cost);
      } else {
         U += eic;
      }
      */

      if (horizon == 0) {
         // with the cost updated as in the earlier version, C returned is C = avgCost + deploymentCost
         return new ConfigUtility<C>(config, U, C);
      }

      /* Go in depth. Assume the current config has a given cost and see the utility of the path
      starting from such current config
       */

      final GaussHermiteParams ghp = NormalGaussHermiteQuadrature.decompose(this.gaussHermitePoints, avgCost, stdCost * stdCost); //N takes mu, variance
      final GaussHermiteParams ghpa = NormalGaussHermiteQuadrature.decompose(this.gaussHermitePoints, avgAcc, stdAcc * stdAcc); //N takes mu, variance
      
      /* check approximation error */
      ghp.checkIntegral(avgCost);
      ghpa.checkIntegral(avgAcc);
      
      
      //cardinality is the same where 
      for (int g = 0; g < ghp.cardinality(); g++) {  
    	  for (int ga = 0; ga < ghp.cardinality(); ga++) {  
	        final double target_y = ghp.getValue(g);      // ghp value that corresponds to weight g
	        final double gh_weight = ghp.getWeight(g);
	        
  	         final double target_acc = ghpa.getValue(ga);      // ghp value that corresponds to weight g
  	         final double ghAcc_weight = ghpa.getWeight(ga);
  	         
	    	 /* Negative target values have two consequences:
	            1. The budget will be reduced by a negative value, i.e., will be increased
	            2. The predictor will have a negative label
	
	            The problem is: we compute EI(x) in closed form, that takes into account
	            also the possibility that y is negative (with a low probability, but the
	            value of this probability actually depends on the stdv).
	            Then, when we speculate  on possible values of x with GH, we only consider the
	            positive x's. So we are computing EI(x) on a "distribution" and then we
	            are speculating in depth only in some other paths
	            */
	
	
	         if (target_y <= 0 && !log_transform) continue;
	         //System.out.println("Adding fake " + target_y + " with weight " + gh_weight);

	         // After deploying this,  there's no more budget, so there's a null utility
	         if (!log_transform) {
	            if (state.getBudget() - target_y <= 0)
	               continue;
	         } else {
	            if (state.getBudget() - Math.exp(target_y) <= 0)
	               continue;
	         }
	
	         /* Clone state as input for the in-depth simulation */
	         State<C, M> clone_curr = state.clone();
	         clone_curr.setCurrentConfiguration(config);
	         // System.out.println("Adding " + target_y + " as speculated ");
	         clone_curr.addTrainingSample(config, target_y);
	         clone_curr.addTrainingSampleAccuracy(config, target_acc);
	         
	         if (!log_transform) {
	            clone_curr.setBudget((clone_curr.getBudget() - target_y - setup_cost));
	         } else {
	            clone_curr.setBudget((clone_curr.getBudget() - Math.exp(target_y)));
	         }
	
	         clone_curr.removeTestSample(config);
	         //C nextC = policy(clone_curr, (horizon - 1));
	         C nextC = optimized_policy(clone_curr, (horizon - 1));
	         if (nextC == null || nextC.equals(config)) {
	            continue;
	         }
	         /* Select next point and add the corresponding cost/utility*/
	         ConfigUtility<C> nextUtil = utility(clone_curr, nextC, (horizon - 1), bestInTrain(clone_curr.getTrainingSet(), T_max));
	         //ConfigUtility<C> nextUtil = utility(clone_curr, nextC, (horizon - 1), bestInTrain(clone_curr.getTrainingSet(), T_max, model));
	         U += gamma * gh_weight * ghAcc_weight * nextUtil.getUtility();
	         // The cost is the exploration cost of the current config (target_y) + the cost of the path
	
	         //TODO: should we mutiply by gamma?
	
	         /* If we discount the utility, we should also discount the price...
	         	I *do* discount by gamma.
	         	In this way, with gamma = 0, also the price of the next config goes away and everything should boil down
	         	to a cherry-pick like approach */
	
	         //Note that the gh weigth is a constant, so it's not affected by the log
	         //Cost is already denormalized in the utlity
	         C += gamma * gh_weight * ghAcc_weight* (nextUtil.getCost());
    	  }
      }
      return new ConfigUtility<>(config, U, C);
   }


   /**
    * An optimized version to get results more quickly
    *
    * @param state
    * @param horizon
    * @return
    */
   private C optimized_policy(State<C, M> state, long horizon) {
      if (HC) {
         throw new UnsupportedOperationException("HC is not supported in the optimized impl");
      }
      //System.out.println("Opt-policy");
      //state.getTrainingSet().printAll();
      final PredictiveModel<C, M> model = buildPredictiveModel(state.getTrainingSet(), state.getParams(),state.getTestSet(), this.seed);
      model.train();

      final PredictiveModel<C, M> modelAccuracy = buildPredictiveModel(state.getTrainingSetAccuracy(), state.getParamsAcc(), state.getTestSet(), this.seed);
      modelAccuracy.train();

      C config;
      C opt = null;
      double cost, sigma_cost, cost99, cost90;
      double acc, sigma_acc, acc99, acc90, ei;
      double time, time90, sigma_time;
      double curr_max_ei = -1;

      final Pair<C, Double> bestSoFar = bestInTrain(state.getTrainingSet(), T_max);
      //final Pair<C, Double> bestSoFar = bestInTrain(state.getTrainingSet(), T_max, model);
      for (int i = 0; i < state.getTestSet().size(); i++) {
         config = state.getTestSet().getConfig(i);
         cost = model.evaluate(config);
         sigma_cost = model.stdv(config);
         cost99 = cost + z_99 * sigma_cost;
         cost90 = cost + 1.282 * sigma_cost;


         acc = modelAccuracy.evaluate(config);
         sigma_acc = modelAccuracy.stdv(config);
         acc99 = acc - z_99 * sigma_acc;
         acc90 = acc - 1.282 * sigma_acc;

         sigma_time = sigma_cost / this.costGenerator.costPerConfigPerMinute(config);
         time = cost /this.costGenerator.costPerConfigPerMinute(config);
         time90 = cost + 1.282 * sigma_time;

         //if (cost <= state.getBudget()) {
         if (acc >= AccConstrain && cost <= state.getBudget() && time <= T_max ) {
         //Cost is ok. Let's compute acquisition function
            //ei = constrainedExpectedImprovement(config, cost, sigma, bestSoFar, false).fst;
            ei = constrainedExpectedImprovement(config, model, modelAccuracy, bestSoFar, false).fst;
            //config, cost, sigma, bestSoFar, false).fst;
            if (opt == null || ei > curr_max_ei) {
               opt = config;
               curr_max_ei = ei;
            }
         }
      }
      //System.out.println("Policy done");
	return opt;
   }

   /**
    * Compute the next config to sample based on the policy
    *
    * @param state
    * @param horizon
    * @return
    */
   private C policy(State<C, M> state, long horizon) {
      PredictiveModel<C, M> model = buildPredictiveModel(state.getTrainingSet(), state.getParams(),state.getTestSet(), this.seed);
      model.train();

      PredictiveModel<C, M> modelAccuracy = buildPredictiveModel(state.getTrainingSetAccuracy(), state.getParamsAcc(),state.getTestSet(), this.seed);
      modelAccuracy.train();

      //Identify the configurations that are within the budget with .99 probability
      final Queue<C> feasibleSet = new LinkedList<>();
      populateWithFeasibleByCost(feasibleSet, state, model, modelAccuracy, z_99, true);
      //if (feasibleSet.size() == 0) {
    //	  populateWithFeasibleByCost(feasibleSet, state, model, modelAccuracy, z_99, false);
     //}
      if (feasibleSet.size() == 0) {
         return null;
      }


      C opt = null;
      /*
      If the horizon is > 1, find the config with highest constrained improvement
      according to the recursive step of the base policy
       */
      //if (horizon > 1) {
      if (true) {
         double curr_max_ei = 0;
         final Pair<C, Double> bestSoFar = bestInTrain(state.getTrainingSet(), T_max);
         //final Pair<C, Double> bestSoFar = bestInTrain(state.getTrainingSet(), T_max, model);

         if (HC) { /* HILL CLIMBING */
            System.out.println("first " + (WekaConfiguration) state.getCurrentConfiguration());
            WekaConfiguration config = (WekaConfiguration) state.getCurrentConfiguration();
            opt = (C) discreteHillClimbing(config, model, modelAccuracy, bestSoFar, feasibleSet);
         } else {    /* CONSTRAINED EXPECTED IMPROVEMENT */
            for (C c : feasibleSet) {
               double v = constrainedExpectedImprovement(c, model, modelAccuracy, bestSoFar, false).fst;
               if (opt == null || v > curr_max_ei) {
                  opt = c;
                  curr_max_ei = v;
               }
            }
         }
      } else {
         if (true)
            throw new RuntimeException("Should not be here. We cannot know when it is our last exploration so we don't discriminate between horizon = 0 or not. We always pick the EIP_c");
         /*
         If horizon = 0 we are at the base step of the policy. This step is greedy: it does not maximize expected
         improvement. Rather, it greedily selects the configuration with the lowest predictive mean that likely
         (with prob > 0.99)  satisfies the cost constraint.
          */
         double curr_min_cost = Double.MAX_VALUE;
         for (C c : feasibleSet) {
            //NB: there's a problem here: I am also considering the configs > T_max.
            //I should filter them out
            System.exit(-1);
            double cost = model.evaluate(c);
            double sigma = model.stdv(c);
            double cost99 = cost + z_99 * sigma;
            //TODO: be aware that deploymentcost is the *real* cost. Is this what you want?
            if (cost99 < T_max * costGenerator.deploymentCost(state, c)) {
               if (opt == null || cost < curr_min_cost) {
                  opt = c;
                  curr_min_cost = cost;
               }
            }
         }
      }
      return opt;
   }

   protected State<C, M> init(TestSet<C, M> allConfigs, double budget, long seed, TestSet<C, M> allTestConfigsNoTarget, long startTime) {
      if (lyn_print) {
         System.out.println("Initializing");
      }
      if (debug_logs) {
         debugWriter.println("Initializing");
         debugWriter.flush();
      }

      String timeout = Main.timeoutToStr(timeoutType);

      final State<C, M> currState = new State<>();
      final TrainingSet<C, M> trainingSet = emptyTrainingSet();
      final TrainingSet<C, M> trainingSetAccuracy = emptyTrainingSet();
      currState.setBudget(budget);
      currState.setTestSet(allConfigs);
      currState.setTrainingSet(trainingSet);
      currState.setTrainingSetAccuracy(trainingSetAccuracy);
      int total = initTrainSamples;
      Random r = new Random(seed);

      double error = 0;

      while (total > 0) {
         C nextConfig = allConfigs.getConfig(r.nextInt(allConfigs.size()));

         double costConfig = this.costGenerator.deploymentCost(null, nextConfig);
         double runningTime = this.costGenerator.deploymentCost(null, nextConfig) / this.costGenerator.costPerConfigPerMinute(nextConfig); // in minutes         this.timeExp += (runningTime*60.0);	//in seconds

         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);
         double accConfig = costGenerator.getAccForConfig(nextConfig);


         long setupCost = setupConfig(nextConfig, currState, this.costGenerator);
         SamplingResult<C> samplingResult = sample(nextConfig, this.costGenerator, currState);
         updateState(currState, setupCost, samplingResult);
         this.timeExp += ((costGenerator.deploymentCost(null, nextConfig) / costGenerator.costPerConfigPerMinute(nextConfig))*60.0);
         total--;

         if(nextDFO < this.currDFO && accConfig >= AccConstrain && runningTime <= T_max) {
             this.currDFO = this.nextDFO;
             this.nexToOpt = (int) (cumulativeExplorations - 1);
         }

         if (runLogs) {
           double actTime =  ((System.currentTimeMillis()-startTime)/1000.0 + this.timeExp)/60.0; //in MINS
        	 //runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO");
        	 runLogsWriter.println(seed + ";" + Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";0;0.0;0;" + maxTimePerc + ";" + cumulativeExplorations + ";" + nextConfig.toString() + ";" + nextDFO + ";" + currDFO + ";" + runningTime + ";" + accConfig + ";" + costConfig + ";0;" + this.avgMuCost + ";" + this.avgSigmaCost + ";" + this.avgEI + ";" + this.mu50Cost + ";" + this.sigma50Cost + ";" + this.ei50 + ";" + this.mu90Cost + ";" + this.sigma90Cost + ";" + this.ei90 + ";" + this.avgMuAcc + ";" + this.avgSigmaAcc + ";" + this.mu50Acc + ";" + this.sigma50Acc + ";" + this.mu90Acc + ";" + this.sigma90Acc +  ";" + actTime);
        	 runLogsWriter.flush();
         }
      }

      if(penalty_cost) {
	      //variance until now is 0 - update the penality in the costs
	      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(),currState.getTestSet(), this.seed);
	      model.train();
	      this.maxVar = model.maxVariance(allConfigs);
	      int counter = 0;

	      for (int i = 0; i < currState.getTrainingSet().size()-counter; i++) {
	 		 C conf = currState.getTrainingSet().getConfig(i).getFst();
	 		 double acc = costGenerator.getAccForConfig(conf);
	 		 //System.out.println(i +"" +conf.toString() +"" + acc);
	      	 if (acc == -1) break; //-1 is return always except when running tensorflowSize

	      	 if (acc < AccConstrain) {
	      		 //update of the cost (with a penalty) on the unfeasible configurations
	     		 currState.removeTrainingSample(conf);
	      		 currState.addTrainingSample(conf,  currState.getTrainingSet().getConfig(i).getSnd()+3*this.maxVar);
	      		 counter ++;
	      		 i--;
	      	  }
	       }
      }


      //System.out.println("Initialized.");
      if (lyn_print) {
         System.out.println("Initialized.");
      }
      if (debug_logs) {
         debugWriter.println("Initialized.");
         debugWriter.flush();
      }
      //currState.getTrainingSet().printAll();
      System.out.println("---------------");
      return currState;
   }

   protected State<C, M> initLHS(TestSet<C, M> allConfigs, double budget, long seed, TestSet<C, M> allTestConfigsNoTarget, long startTime) {
      if (lyn_print) {
         System.out.println("Initializing");
      }
      if (debug_logs) {
         debugWriter.println("Initializing");
         debugWriter.flush();
      }

      String timeout = Main.timeoutToStr(timeoutType);

      final State<C, M> currState = new State<>();
      final TrainingSet<C, M> trainingSet = emptyTrainingSet();
      final TrainingSet<C, M> trainingSetAccuracy = emptyTrainingSet();
      lhs.setDataset(trainingSet.instances());
      lhs.createLHSset((int) seed);
      currState.setBudget(budget);
      currState.setTestSet(allConfigs);
      currState.setTrainingSet(trainingSet);
      currState.setTrainingSetAccuracy(trainingSetAccuracy);
      int total = initTrainSamples;
      
      if(initSamples_budget == false) {
    	  // init samples is a percentage of search space
	      while (total > 0) {
	          C nextConfig = lhs.getSample();
	          double costConf = this.costGenerator.deploymentCost(null, nextConfig);
	          double runningTime = this.costGenerator.deploymentCost(null, nextConfig) / this.costGenerator.costPerConfigPerMinute(nextConfig); // in minutes
	
	          this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);
	          double accConfig = costGenerator.getAccForConfig(nextConfig);
	
	          long setupCost = setupConfig(nextConfig, currState, this.costGenerator);
	          SamplingResult<C> samplingResult = sample(nextConfig, this.costGenerator, currState);
	          updateState(currState, setupCost, samplingResult);
	          this.timeExp += ((costGenerator.deploymentCost(null, nextConfig) / costGenerator.costPerConfigPerMinute(nextConfig))*60.0);
	          total--;
	
	          if(nextDFO < this.currDFO && accConfig >= AccConstrain && runningTime <= T_max) {
	              this.currDFO = this.nextDFO;
	              this.nexToOpt = (int) (cumulativeExplorations - 1);
	          }
	
	
	          if (runLogs) {
	            double actTime =  ((System.currentTimeMillis()-startTime)/1000.0 + this.timeExp)/60.0; //in MINS
	 	         //runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO");
	         	 runLogsWriter.println(seed + ";" + Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";0;0.0;0;" + maxTimePerc + ";" + cumulativeExplorations + ";" + nextConfig.toString() + ";" + nextDFO + ";" + currDFO + ";" + runningTime + ";" + accConfig + ";" + costConf + ";0;" + this.avgMuCost + ";" + this.avgSigmaCost + ";" + this.avgEI + ";" + this.mu50Cost + ";" + this.sigma50Cost + ";" + this.ei50 + ";" + this.mu90Cost + ";" + this.sigma90Cost + ";" + this.ei90 + ";" + this.avgMuAcc + ";" + this.avgSigmaAcc + ";" + this.mu50Acc + ";" + this.sigma50Acc + ";" + this.mu90Acc + ";" + this.sigma90Acc + ";" + actTime);
	         	 runLogsWriter.flush();
	          }
	
	       }
      }else {
    	  // init samples is accord to a inital budget
    	  double initBudget = 5.0;

    	  if(Main.file.contains("cnn")) {
    		  initBudget = 5.0;
    	  }else if(Main.file.contains("multilayer")) {
    		  initBudget = 1.0;
    	  }else {
    		  initBudget = 0.5;
    	  }

    	  
    
		while (currState.getBudget() >= 0.0 && initBudget >= 0.0) {			
	         C nextConfig = lhs.getSample();
	         
	         System.out.println(initBudget);
	         double costConf = this.costGenerator.deploymentCost(null, nextConfig);
	         double runningTime = this.costGenerator.deploymentCost(null, nextConfig) / this.costGenerator.costPerConfigPerMinute(nextConfig); // in minutes
	
	         this.nextDFO = distanceFromOpt(allTestConfigsNoTarget, nextConfig, T_max);
	         double accConfig = costGenerator.getAccForConfig(nextConfig);
	
	         long setupCost = setupConfig(nextConfig, currState, this.costGenerator);
	         SamplingResult<C> samplingResult = sample(nextConfig, this.costGenerator, currState);
	         updateState(currState, setupCost, samplingResult);
	         this.timeExp += ((costGenerator.deploymentCost(null, nextConfig) / costGenerator.costPerConfigPerMinute(nextConfig))*60.0);
	         initBudget -= costConf;
	
	         if(nextDFO < this.currDFO && accConfig >= AccConstrain && runningTime <= T_max) {
	             this.currDFO = this.nextDFO;
	             this.nexToOpt = (int) (cumulativeExplorations - 1);
	         }
	
	
	         if (runLogs) {
	           double actTime =  ((System.currentTimeMillis()-startTime)/1000.0 + this.timeExp)/60.0; //in MINS
		         //runLogsWriter.println("runID;wkld;optimizer;budget;bootstrapMethod;initSamples;timeout;lookahead;gamma;gh;maxTimePerc;explorationNumber;config;DFO(config);currBestDFO");
	        	 runLogsWriter.println(seed + ";" + Main.file + ";" + this.opt_type + ";" + discreteBudget + ";" + Main.bootstrap + ";" + initTrainSamples + ";" + timeout + ";0;0.0;0;" + maxTimePerc + ";" + cumulativeExplorations + ";" + nextConfig.toString() + ";" + nextDFO + ";" + currDFO + ";" + runningTime + ";" + accConfig + ";" + costConf + ";0;" + this.avgMuCost + ";" + this.avgSigmaCost + ";" + this.avgEI + ";" + this.mu50Cost + ";" + this.sigma50Cost + ";" + this.ei50 + ";" + this.mu90Cost + ";" + this.sigma90Cost + ";" + this.ei90 + ";" + this.avgMuAcc + ";" + this.avgSigmaAcc + ";" + this.mu50Acc + ";" + this.sigma50Acc + ";" + this.mu90Acc + ";" + this.sigma90Acc + ";" + actTime);
	        	 runLogsWriter.flush();
	         }
	
	      }
      }
      
      if(penalty_cost) {
	      //variance until now is 0 - update the penality in the costs
	      PredictiveModel<C, M> model = buildPredictiveModel(currState.getTrainingSet(), currState.getParams(), currState.getTestSet(), this.seed);
	      model.train();
	      this.maxVar = model.maxVariance(allConfigs);
	      int counter = 0;

	      for (int i = 0; i < currState.getTrainingSet().size()-counter; i++) {
	 		 C conf = currState.getTrainingSet().getConfig(i).getFst();
	 		 double acc = costGenerator.getAccForConfig(conf);
	 		 double costConf = currState.getTrainingSet().getConfig(i).getSnd();
	 		 double runningTime =  costConf / costGenerator.costPerConfigPerMinute(conf); // in minutes
	 		 //System.out.println(i +"" +conf.toString() +"" + acc);
	      	 if (acc == -1) break; //-1 is return always except when running tensorflowSize

	      	 if (acc < AccConstrain || runningTime > T_max ) {
	      		 //update of the cost (with a penalty) on the unfeasible configurations
	     		 currState.removeTrainingSample(conf);
	      		 currState.addTrainingSample(conf,  costConf+3*this.maxVar);
	      		 counter ++;
	      		 i--;
	      	  }
	       }
      }


      //System.out.println("Initialized.");
      if (lyn_print) {
         System.out.println("Initialized.");
      }
      if (debug_logs) {
         debugWriter.println("Initialized.");
         debugWriter.flush();
      }

      return currState;
   }

   private class LynceusThread extends Thread {
      private Queue<C> allConfigs;
      private Queue<ConfigUtility<C>> results;
      private Queue<Double> times;
      private lynceus.State<C, M> state;
      private long horizon;
      private Pair<C, Double> bestSoFar;

      LynceusThread(Queue<C> allConfigs, Queue<ConfigUtility<C>> results, lynceus.State<C, M> state, long horizon, Pair<C, Double> bestSoFar, Queue<Double> times) {
         this.allConfigs = allConfigs;
         this.results = results;
         this.state = state;
         this.horizon = horizon;
         this.bestSoFar = bestSoFar;
         this.times = times;

         if (retrain_in_depth) {
            this.state.resetParams();
         }
      }

      @Override
      public void run() {
         C c;
         ConfigUtility<C> res;
         long start;
         long elapsedTime;

         while ((c = allConfigs.poll()) != null) {
            start = System.currentTimeMillis();
            res = utility(this.state, c, horizon, bestSoFar);
            elapsedTime = System.currentTimeMillis() - start;
            results.add(res);
            times.add(elapsedTime / 1000.0);
         }
      }
   }


   /*
      The easiest algorithm to find the minimum of a function is hill climbing.
      Problem: how do I climb a space with discrete and conditional variables??
      Possible solution: first, enumerate all coherent subspaces, i.e., subspaces where
      you can do coordinate descent. Then, apply coordinate descent within each subspace
                                        Evaluating feasible config
      Solution 1. I enumerate the possible sub-sets that I can navigate using hill climbing

      OR: Change strategy.  Draw random configurations, fit a model over it and
      use the model to take the next point.
      If we have a candidate set with size > T, we draw K points.

      Let's do this. It's far easier
       */

   //Method that returns the initial points for the random search
   private void generateCandidateStartingPointSet(Queue<C> allCandidates, long searchSpaceCardinality) {
      //We sample at least 1/3 of the space to build a model

      int numCandidates = Math.min(allCandidates.size(), (int) (((double) searchSpaceCardinality) / 3.0));
      if (numCandidates == 0)
         numCandidates = 1;
      final ArrayList<C> set = new ArrayList<>(allCandidates);
      final long seed = allCandidates.size();
      final Random r = new Random(seed);
      Collections.shuffle(set, r);
      allCandidates.clear();
      while (numCandidates > 0) {
         C c = set.remove(0);
         allCandidates.add(c);
         numCandidates--;
      }
   }


   public WekaConfiguration discreteHillClimbing(WekaConfiguration config, PredictiveModel<C, M> model, PredictiveModel<C, M> modelAccuracy, Pair<C, Double> bestSoFar, Queue<C> feasibleSet) {

      WekaConfiguration currentConfig = config;
      WekaConfiguration nextConfig;
      double nextEval, aux;
      ArrayList<WekaConfiguration> neighbours = null;


      while (true) {
         /* find the neighbours of the current config */
         neighbours = (ArrayList<WekaConfiguration>) currentConfig.neighbourhood();

         System.out.println("[Lynceus] current " + currentConfig + "neighbours:");
         for (WekaConfiguration c : neighbours) {
            System.out.println(c);
         }
         nextConfig = null;
         nextEval = Double.NEGATIVE_INFINITY;

         /* find the best neighbour */
         for (WekaConfiguration c : neighbours) {
            if (feasibleSet.contains(c)) {   // = if P(Cost(c) < B) >= 0.99
               aux = evalHillClimbing((C) c, model, modelAccuracy, bestSoFar);
               if (aux >= nextEval) {
                  nextConfig = c;
                  nextEval = aux;
               }
            }
         }
         System.out.println("[Lynceus] HC: best " + nextConfig + " ; eval = " + nextEval);

         /* Return current node since no better neighbours exist */
         if (nextEval < evalHillClimbing((C) currentConfig, model, modelAccuracy, bestSoFar)) {
            return currentConfig;
         }
         currentConfig = nextConfig;
      }
   }


   private double evalHillClimbing(C c, PredictiveModel<C, M> model, PredictiveModel<C, M> modelAccuracy, Pair<C, Double> bestSoFar) {
      double v = constrainedExpectedImprovement(c, model,modelAccuracy, bestSoFar, false).fst;
      return v;
   }


//   public static void main(String[] args){
//
//	   ExtendedWekaScoutLynceus wekaLyn = new ExtendedWekaScoutLynceus(0, 8, 0.0, 0.9, optimizer.CHERRYPICK, 1);
//
//	   final ExtendedScoutVMConfigWekaTestSet testSet = new ExtendedScoutVMConfigWekaTestSet("files/wkld1_test_set.arff");
//
//	   final ExtendedScoutVMConfigWekaTrainingSet trainingSet = new ExtendedScoutVMConfigWekaTrainingSet("files/wkld1_training_set.arff");
//
//	   PredictiveModel model = wekaLyn.buildPredictiveModel(trainingSet);
//       model.train();
//
//       model.test(testSet);
//
//   }


}
