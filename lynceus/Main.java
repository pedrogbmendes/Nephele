package lynceus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import lynceus.cherrypick.CherrypickConfigCostGenerator;
import lynceus.cherrypick.CherrypickLHS;
import lynceus.giraph.ExtendedGiraphConfigCostGenerator;
import lynceus.giraph.ExtendedPlusGiraphConfigCostGenerator;
import lynceus.giraph.GiraphConfigCostGenerator;
import lynceus.giraph.GiraphPlusConfigCostGenerator;
import lynceus.results.ExpParam;
import lynceus.results.OptResult;
import lynceus.scout.ExtendedScoutVMCostGenerator;
import lynceus.scout.ReducedScoutVMCostGenerator;
import lynceus.scout.ScoutVMCostGenerator;
import lynceus.tensorflow.TensorflowConfigCostGenerator;
import lynceus.tensorflow.TensorflowConfigCostGeneratorSize;
import lynceus.tm.TMCostGenerator;
import weka.cherrypick.WekaCherrypickConfigFactory;
import weka.cherrypick.WekaCherrypickConfigLynceus;
import weka.extendedGiraph.ExtendedWekaGiraphConfigFactory;
import weka.extendedGiraph.ExtendedWekaGiraphConfigLynceus;
import weka.extendedPlusGiraph.ExtendedPlusWekaGiraphConfigFactory;
import weka.extendedPlusGiraph.ExtendedPlusWekaGiraphConfigLynceus;
import weka.extendedScout.ExtendedWekaScoutLynceus;
import weka.extendedScout.ExtendedWekaScoutVMConfigFactory;
import weka.giraph.WekaGiraphConfigFactory;
import weka.giraph.WekaGiraphConfigLynceus;
import weka.giraphPlus.WekaGiraphPlusConfigFactory;
import weka.giraphPlus.WekaGiraphPlusConfigLynceus;
import weka.reducedScout.ReducedWekaScoutLynceus;
import weka.reducedScout.ReducedWekaScoutVMConfigFactory;
import weka.scout.WekaScoutLynceus;
import weka.scout.WekaScoutVMConfigFactory;
import weka.tensorflow.WekaTensorflowConfigFactory;
import weka.tensorflow.WekaTensorflowConfigLynceus;
import weka.tensorflow.WekaTensorflowConfigLynceusSize;
import weka.tm.WekaTMConfigLynceus;
import weka.vm.WekaVMConfigLynceus;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 15.03.18
 */
public class Main {

   public static String fileName;
   static String file;
   static String datasetFile;
   static double budget;
   static Lynceus.optimizer optimizer;
   static int numSeeds;
   static long horizon;  // 2 = default value in Remi's paper
   static int initialTrainingSamples;
   static boolean hillClimbing = false;
   static boolean allBudgets;
   static double acc;
   //static boolean timeout;
   //static boolean discreteTimeout;
   static double gamma = 0.9;
   static long gh = 5;
   static double max_time = 0;      // in mins
   static double max_time_perc = 0;
   static int WKLD_ID = 0;
   static String bootstrap;

   static boolean is_fixed_budget = false;
   static double fixed_budget = 7.0;


   private enum test {
      VM, SCOUT, TM, EXTENDED_SCOUT, TENSORFLOW, TENSORFLOWSIZE, GIRAPH, REDUCED_SCOUT, CHERRYPICK, EXTENDED_GIRAPH, EXTENDED_PLUS_GIRAPH, GIRAPH_PLUS
   }

   private static test target;

   public enum timeout {
      FALSE, IDEAL, LIN, POLI, LOG
   }

   public static timeout timeoutType;

   public static String timeoutToStr(timeout timeout) {
      switch (timeout) {
         case FALSE:
            return "false";
         case IDEAL:
            return "ideal";
         case LIN:
            return "linear";
         case POLI:
            return "poli";
         case LOG:
            return "log";
         default:
            return "unrecognized timeout type";
      }
   }

   public static timeout strToTimeout(String input) {
      if (input.equalsIgnoreCase("n")) {
         System.out.println("Running WITHOUT timeout");
         return timeout.FALSE;
      } else if (input.equalsIgnoreCase("ideal")) {
         System.out.println("Running with ideal timeout");
         return timeout.IDEAL;
      } else if (input.equalsIgnoreCase("linear")) {
         System.out.println("Running with linear timeout");
         return timeout.LIN;
      } else if (input.equalsIgnoreCase("poli")) {
         System.out.println("Running with poli timeout");
         return timeout.POLI;
      } else if (input.equalsIgnoreCase("log")) {
         System.out.println("Running with log timeout");
         return timeout.LOG;
      } else {
         System.out.println("unrecognized timeout type. Setting to false.");
         return timeout.FALSE;
      }
   }

   private static Lynceus buildLynceus(long horizon, double budget, double epsilon, double gamma, Lynceus.optimizer opt, long wkld) {
      //Lynceus.initTrainSamples = initialTrainingSamples;
      switch (target) {
         case VM:
            //Lynceus.initTrainSamples = initialTrainingSamples;
            return new WekaVMConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case TM:
            //Lynceus.initTrainSamples = initialTrainingSamples;
            return new WekaTMConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case SCOUT:
            /*
            Cherrypick uses 3 samples, but does not stop with less than 6.
            To me, building a model on 3 samples is a questionable choice: how can it really differ from a plain mean?
            Sure, in our ensemble with bagging, we even reduce the number of samples per predictor.
            Even more so, we need more samples.
            We have 5 dimensions (type, cpu, ecus, ram, num_instances) so I propose we use 5 initial samples
             */
            //Lynceus.initTrainSamples = initialTrainingSamples; //TODO: I wonder what happens to the ensemble with so few samples
            return new WekaScoutLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case EXTENDED_SCOUT:
            //Lynceus.initTrainSamples = initialTrainingSamples; //TODO: I wonder what happens to the ensemble with so few samples
            return new ExtendedWekaScoutLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case REDUCED_SCOUT:
            return new ReducedWekaScoutLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case TENSORFLOW:
            //Lynceus.initTrainSamples = initialTrainingSamples;
            Lynceus.HC = hillClimbing;
            return new WekaTensorflowConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case TENSORFLOWSIZE:
             //Lynceus.initTrainSamples = initialTrainingSamples;
             Lynceus.HC = hillClimbing;
             return new WekaTensorflowConfigLynceusSize(horizon, budget, epsilon, gamma, opt, wkld);
         case GIRAPH:
            //Lynceus.initTrainSamples = initialTrainingSamples;
            return new WekaGiraphConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case GIRAPH_PLUS:
        	 return new WekaGiraphPlusConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case EXTENDED_GIRAPH:
        	 return new ExtendedWekaGiraphConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case EXTENDED_PLUS_GIRAPH:
        	 return new ExtendedPlusWekaGiraphConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         case CHERRYPICK:
            return new WekaCherrypickConfigLynceus(horizon, budget, epsilon, gamma, opt, wkld);
         default:
            throw new UnsupportedOperationException(target + " is not a supported test");
      }
   }

   public static void main(String[] args) {

      //welcome();

      System.out.println(Arrays.toString(args));
      parseArgs(args);

      /* class attributes to compute duration of an experiment */
      double totalDuration = 0.0;
      double[] duration;
      ArrayList<Double> timer = new ArrayList<Double>();   // stores how long each seed took
      ArrayList<ArrayList<Double>> seedDurations = new ArrayList<ArrayList<Double>>();
      double[] avgSeedDuration;
      long start;
      long startTimer;
      long endTimer;
      long elapsedTime;
      int counter = 0;
      int budgetIndex = 0;
      double time_sum = 0.0;
      long countTime = 0;
      double __gamma;

      // By taking the avg cost we decide how much of the param space we want to let explore
      //The number of configs is roughly 150 so 150 * budget allows almost
      //complete exploration.  15 gives one tenth

      final Random shuffleRandom = new Random(30031987);

      // #########################
      // #		DEBUG MODE		 #
      // #########################
      // Lynceus.setDebugLogs();


      // #####################################
      // #		ESTIMATION ERRORS LOGS		 #
      // #####################################
      // Lynceus.setEstimationErrorsLogs();


      List<Integer> wklds = new ArrayList<>();
      int numWklds, initWklds;
      switch (target) {
         case TM:   // 100 wklds
            numWklds = 1;
            initWklds = 1; //Do not consider the header file in Proteus
//            if (true)
//               throw new IllegalArgumentException("TM not supported anymore");
            break;
         case SCOUT:
            numWklds = 1;
            initWklds = 1;
            break;
         case EXTENDED_SCOUT:
            numWklds = 1;
            initWklds = 1;
            if (numWklds != 1) {
               throw new IllegalArgumentException("Scout should have only 1 wkld ");
            }
            break;
         case REDUCED_SCOUT:
            numWklds = 1;
            initWklds = 1;
            if (numWklds != 1) {
               throw new IllegalArgumentException("Scout should have only 1 wkld ");
            }
            break;
         case TENSORFLOW:
            /* there is only one workload */
            numWklds = 1;
            initWklds = 1;
            break;
         case TENSORFLOWSIZE:
             /* there is only one workload */
             numWklds = 1;
             initWklds = 1;
             break;  
         case GIRAPH:
            /* there are 3 giraph workloads */
            numWklds = 1;
            initWklds = 1;
            break;
         case GIRAPH_PLUS:
             /* there are 3 giraph workloads */
             numWklds = 1;
             initWklds = 1;
             break;
         case EXTENDED_GIRAPH:
             /* there are 3 giraph workloads */
             numWklds = 1;
             initWklds = 1;
             break;
         case EXTENDED_PLUS_GIRAPH:
             /* there are 3 giraph workloads */
             numWklds = 1;
             initWklds = 1;
             break;
         case CHERRYPICK:
            numWklds = 1;
            initWklds = 1;
            break;
         default:
            throw new UnsupportedOperationException(target + " is not a supported test");
      }
      for (int i = initWklds; i <= numWklds; i++) {      //Start from wkld 2!!
         wklds.add(i);
      }
      Collections.shuffle(wklds, shuffleRandom);

      System.out.println("System;Wkld;Budget;Epsilon;A-DFO;A-NEX;A-CEX;50-DF0;50-NEX;50-CEX;90-DFO;90-NEX;90-CEX;99-DFO;99-NEX;99-CEX;WITHIN_CONSTRAINT;TOTAL_RUNS");
      for (int id : wklds) {
         counter = 0;
         budgetIndex = 0;
         time_sum = 0.0;

         double avgBudget;
         /* all times in the 'switch' are in min */
         switch (target) {
            case TM:
               //TMCostGenerator.setTargetWkld(id);
               TMCostGenerator costGeneratorTM = new TMCostGenerator();
               avgBudget = costGeneratorTM.getAvgCost();
               //if (true) throw new UnsupportedOperationException("TM is not supported now");
               double maxTimeTM = costGeneratorTM.getMaxTime();
               double minTimeTM = costGeneratorTM.getMinTime();
//               //double tmax = minTime + max_time_perc * (maxTime - minTime);
               double tmaxTM = minTimeTM * (1.0D + max_time_perc);
               //max_time = tmaxTM;
               max_time = costGeneratorTM.getTimeForSpecificConstraint(max_time_perc);
//               //Half-way from min to max
               System.out.println("MinTime " + minTimeTM + " maxTime " + maxTimeTM + " Setting T_max " + max_time);
               Lynceus.setT_max(max_time);
               break;
            case SCOUT:
               //ScoutVMCostGenerator.setTargetWkld(id);
               ScoutVMCostGenerator costGenerator = new ScoutVMCostGenerator();
               avgBudget = costGenerator.getAvgCost();
               double maxTime = costGenerator.getMaxTime();
               double minTime = costGenerator.getMinTime();
               //double tmax = minTime + max_time_perc * (maxTime - minTime);
               double tmax = minTime * (1.0D + max_time_perc);
               //max_time = tmax;
               max_time = costGenerator.getTimeForSpecificConstraint(max_time_perc);
               //Half-way from min to max
               System.out.println("MinTime " + minTime + " maxTime " + maxTime + " Setting T_max " + max_time);
               Lynceus.setT_max(max_time);
               break;
            case EXTENDED_SCOUT:
               //ExtendedScoutVMCostGenerator.setTargetWkld(id);
               ExtendedScoutVMCostGenerator costGenerator2 = new ExtendedScoutVMCostGenerator();
               avgBudget = costGenerator2.getAvgCost();
               double maxTime2 = costGenerator2.getMaxTime();
               double minTime2 = costGenerator2.getMinTime();
               //double tmax2 = minTime2 + max_time_perc * (maxTime2 - minTime2);
               double tmax2 = minTime2 * (1.0D + max_time_perc);
               //max_time = tmax2;
               max_time = costGenerator2.getTimeForSpecificConstraint(max_time_perc);
               //Half-way from min to max
               System.out.println("Avg cost is " + avgBudget);
               System.out.println("MinTime " + minTime2 + " maxTime " + maxTime2 + " perc is " + max_time_perc + " Setting T_max " + max_time);
               Lynceus.setT_max(max_time);
               break;
            case REDUCED_SCOUT:
               ReducedScoutVMCostGenerator costGenerator3 = new ReducedScoutVMCostGenerator();
               avgBudget = costGenerator3.getAvgCost();
               System.out.println("Avg cost is " + avgBudget);
               double maxTime3 = costGenerator3.getMaxTime();
               double minTime3 = costGenerator3.getMinTime();
               //double tmax3 = minTime3 + max_time_perc * (maxTime3 - minTime3);
               double tmax3 = minTime3 * (1.0D + max_time_perc);
               //max_time = tmax3;
               max_time = costGenerator3.getTimeForSpecificConstraint(max_time_perc);
               //Half-way from min to max
               System.out.println("MinTime " + minTime3 + " maxTime " + maxTime3 + " Setting T_max " + max_time);
               Lynceus.setT_max(max_time);
               break;
            case TENSORFLOW:
               avgBudget = 0.15;
               TensorflowConfigCostGenerator TFcostGenerator;
               try {
                  TFcostGenerator = new TensorflowConfigCostGenerator(datasetFile);
                  avgBudget = TFcostGenerator.getAvgCost();
                  System.out.println("Avg cost is " + avgBudget);
                  double maxTimeTF = TFcostGenerator.getMaxTime();
                  double minTimeTF = TFcostGenerator.getMinTime();
                  //double tmaxTF = minTimeTF + max_time_perc * (maxTimeTF - minTimeTF);
                  double tmaxTF = minTimeTF * (1.0D + max_time_perc);
                  //max_time = tmaxTF;
                  max_time = TFcostGenerator.getTimeForSpecificConstraint(max_time_perc);
                  System.out.println("MinTime " + minTimeTF + " maxTime " + maxTimeTF + " Setting T_max " + max_time + " tmaxTF " + tmaxTF + " max_time_perc " + max_time_perc);
                  Lynceus.setT_max(max_time);
               } catch (IOException e1) {
                  System.out.println("[Main] Error setting up avgBudget");
                  e1.printStackTrace();
               }
               break;
            case TENSORFLOWSIZE:
                avgBudget = 0.15;
                TensorflowConfigCostGeneratorSize TFcostGeneratorSize;
                try {
                   TFcostGeneratorSize = new TensorflowConfigCostGeneratorSize(datasetFile);
                   avgBudget = TFcostGeneratorSize.getAvgCost();
                   System.out.println("Avg cost is " + avgBudget);
                   double maxTimeTFsize = TFcostGeneratorSize.getMaxTime();
                   double minTimeTFsize = TFcostGeneratorSize.getMinTime();
                   //double tmaxTF = minTimeTF + max_time_perc * (maxTimeTF - minTimeTF);
                   double tmaxTFsize = minTimeTFsize * (1.0D + max_time_perc);
                   //max_time = tmaxTF;
                   max_time = TFcostGeneratorSize.getTimeForSpecificConstraint(max_time_perc);
                   System.out.println("MinTime " + minTimeTFsize + " maxTime " + maxTimeTFsize + " Setting T_max " + max_time + " tmaxTF " + tmaxTFsize + " max_time_perc " + max_time_perc);
                   Lynceus.setT_max(max_time);
                } catch (IOException e1) {
                   System.out.println("[Main] Error setting up avgBudget");
                   e1.printStackTrace();
                }
                break;
            case GIRAPH:
               avgBudget = 0.15;
               GiraphConfigCostGenerator giraphCostGenerator;
               try {
                  giraphCostGenerator = new GiraphConfigCostGenerator(datasetFile);
                  avgBudget = giraphCostGenerator.getAvgCost();
                  System.out.println("Avg cost is " + avgBudget);
                  double maxTimeG = giraphCostGenerator.getMaxTime();
                  double minTimeG = giraphCostGenerator.getMinTime();
                  //double tmaxG = minTimeG + max_time_perc * (maxTimeG - minTimeG);
                  double tmaxG = minTimeG * (1.0D + max_time_perc);
                  //max_time = tmaxG;
                  max_time = giraphCostGenerator.getTimeForSpecificConstraint(max_time_perc);
                  System.out.println("MinTime " + minTimeG + " maxTime " + maxTimeG + " Setting T_max " + max_time);
                  Lynceus.setT_max(max_time);
               } catch (IOException e1) {
                  System.out.println("[Main] Error setting up avgBudget");
                  e1.printStackTrace();
               }
               break;
            case GIRAPH_PLUS:
                avgBudget = 0.15;
                GiraphPlusConfigCostGenerator giraphPlusCostGenerator;
                try {
                	giraphPlusCostGenerator = new GiraphPlusConfigCostGenerator(datasetFile);
                   avgBudget = giraphPlusCostGenerator.getAvgCost();
                   System.out.println("Avg cost is " + avgBudget);
                   double maxTimeG = giraphPlusCostGenerator.getMaxTime();
                   double minTimeG = giraphPlusCostGenerator.getMinTime();
                   //double tmaxG = minTimeG + max_time_perc * (maxTimeG - minTimeG);
                   double tmaxG = minTimeG * (1.0D + max_time_perc);
                   //max_time = tmaxG;
                   max_time = giraphPlusCostGenerator.getTimeForSpecificConstraint(max_time_perc);
                   System.out.println("MinTime " + minTimeG + " maxTime " + maxTimeG + " Setting T_max " + max_time);
                   Lynceus.setT_max(max_time);
                } catch (IOException e1) {
                   System.out.println("[Main] Error setting up avgBudget");
                   e1.printStackTrace();
                }
                break;
            case EXTENDED_GIRAPH:
                avgBudget = 0.15;
                ExtendedGiraphConfigCostGenerator extendedGiraphCostGenerator;
                try {
                   extendedGiraphCostGenerator = new ExtendedGiraphConfigCostGenerator(datasetFile);
                   avgBudget = extendedGiraphCostGenerator.getAvgCost();
                   System.out.println("Avg cost is " + avgBudget);
                   double maxTimeG = extendedGiraphCostGenerator.getMaxTime();
                   double minTimeG = extendedGiraphCostGenerator.getMinTime();
                   //double tmaxG = minTimeG + max_time_perc * (maxTimeG - minTimeG);
                   double tmaxG = minTimeG * (1.0D + max_time_perc);
                   //max_time = tmaxG;
                   max_time = extendedGiraphCostGenerator.getTimeForSpecificConstraint(max_time_perc);
                   System.out.println("MinTime " + minTimeG + " maxTime " + maxTimeG + " Setting T_max " + max_time);
                   Lynceus.setT_max(max_time);
                } catch (IOException e1) {
                   System.out.println("[Main] Error setting up avgBudget");
                   e1.printStackTrace();
                }
                break;
            case EXTENDED_PLUS_GIRAPH:
                avgBudget = 0.15;
                ExtendedPlusGiraphConfigCostGenerator extendedPLusGiraphCostGenerator;
                try {
                	extendedPLusGiraphCostGenerator = new ExtendedPlusGiraphConfigCostGenerator(datasetFile);
                   avgBudget = extendedPLusGiraphCostGenerator.getAvgCost();
                   System.out.println("Avg cost is " + avgBudget);
                   double maxTimeG = extendedPLusGiraphCostGenerator.getMaxTime();
                   double minTimeG = extendedPLusGiraphCostGenerator.getMinTime();
                   //double tmaxG = minTimeG + max_time_perc * (maxTimeG - minTimeG);
                   double tmaxG = minTimeG * (1.0D + max_time_perc);
                   //max_time = tmaxG;
                   max_time = extendedPLusGiraphCostGenerator.getTimeForSpecificConstraint(max_time_perc);
                   System.out.println("MinTime " + minTimeG + " maxTime " + maxTimeG + " Setting T_max " + max_time);
                   Lynceus.setT_max(max_time);
                } catch (IOException e1) {
                   System.out.println("[Main] Error setting up avgBudget");
                   e1.printStackTrace();
                }
                break;
            case CHERRYPICK:
               CherrypickConfigCostGenerator cherrypickCostGenerator;
               cherrypickCostGenerator = new CherrypickConfigCostGenerator();
               avgBudget = cherrypickCostGenerator.getAvgCost();
               System.out.println("Avg cost is " + avgBudget);
               double maxTimeCP = cherrypickCostGenerator.getMaxTime();
               double minTimeCP = cherrypickCostGenerator.getMinTime();
               //double tmax3 = minTime3 + max_time_perc * (maxTime3 - minTime3);
               double tmaxCP = minTimeCP * (1.0D + max_time_perc);
               // max_time = tmaxCP;
               max_time = cherrypickCostGenerator.getTimeForSpecificConstraint(max_time_perc);
               //Half-way from min to max
               System.out.println("MinTime " + minTimeCP + " maxTime " + maxTimeCP + " Setting T_max " + max_time);
               Lynceus.setT_max(max_time);
               break;
            default:
               throw new UnsupportedOperationException(target + " is not a supported test");
         }

         //max_time = 10;

         if (max_time == 0) {
            throw new RuntimeException("Max time cannot be zero");
         }

         long[] seeds = new long[numSeeds];
         for (int s = 1; s <= numSeeds; s++) {
            seeds[s - 1] = s;
         }


         double[] budgets;

         if (allBudgets) {
            //budgets = new double[]{8, 10, 15, 20};
            budgets = new double[]{1, 1.25, 1.5, 1.75, 2, 3, 4, 5};
            avgSeedDuration = new double[budgets.length];
            duration = new double[budgets.length];
         } else {
            //budgets = new double[]{avgBudget * budget};
            budgets = new double[]{budget};
            avgSeedDuration = new double[1];
            duration = new double[1];
         }

         double[] epsilons = {0.0};

         HashMap<ExpParam, OptResult> hL = new HashMap<>();

         Lynceus.optimizer[] optimizers = {optimizer};

         /* Do experiments for workload*/
         //budgetIndex = 0;
         for (double b : budgets) {
            double actualBudget = 0;
            if (!is_fixed_budget)
               actualBudget = b * avgBudget * initialTrainingSamples;
            else
               actualBudget = b * avgBudget * fixed_budget;
            
            //double actualBudget = b * avgBudget;
            actualBudget = b;
            System.out.println("D: Actual money " + actualBudget);
            budget = b;  //Set this for the name
            for (double e : epsilons) {
               start = 0;
               elapsedTime = 0;
               start = System.currentTimeMillis();
               for (long s : seeds) {
                  startTimer = 0;
                  endTimer = 0;
                  counter = 0;
                  time_sum = 0;
                  startTimer = System.currentTimeMillis();
                  countTime = 0;
                  for (Lynceus.optimizer o : optimizers) {
                     if (o.equals(Lynceus.optimizer.CHERRYPICK_UCB)) {
                        __gamma = 0.1;                                //default value in Srinivas paper (though they do CV to scale it down)
                     } else {
                        __gamma = gamma; //0.9 is the default value in Remi's paper
                     }

                     // #########################
                     // #		RUN LOGS		 #
                     // #########################
                     Lynceus.setRunLogs(s);

                     Lynceus.setDiscreteBudget(budget);
                     Lynceus.setMaxTimePerc(max_time_perc);
                     Lynceus lyn = buildLynceus(horizon, actualBudget, e, __gamma, o, id);
                     ExpParam expParam = new ExpParam(b, e, s, id, o);
                     hL.put(expParam, lyn.doYourJob(s));
                     System.out.println("adding to hashmap: dist = " + hL.get(expParam).getDistFromOpt());


                  }
                  endTimer = System.currentTimeMillis() - startTimer;
                  timer.add(endTimer / 1000.0);
               }
               elapsedTime = System.currentTimeMillis() - start;
               duration[budgetIndex] = elapsedTime / 1000.0;
               totalDuration += duration[budgetIndex];
               while (counter < timer.size()) {
                  time_sum += timer.get(counter);
                  counter++;
               }
               avgSeedDuration[budgetIndex] = time_sum / timer.size();
               seedDurations.add((ArrayList<Double>) timer.clone());
               timer.clear();
               budgetIndex++;
            }
         }

         /* set-up results file */
         PrintWriter writer = null;
         String resultsFile = "files/results/";
         resultsFile = setFileName(id, resultsFile);
         try {
            File f = new File(resultsFile);
            if (f.exists()) {
               f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
            }
            writer = new PrintWriter(resultsFile, "UTF-8");
         } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }

         /* set-up log file */
         PrintWriter logWriter = null;
         String logFile = "files/logs/";
         logFile = setFileName(id, logFile);
         try {
            File f = new File(logFile);
            if (f.exists()) {
               f.renameTo(new File(f.getAbsolutePath().concat(".").concat(String.valueOf(System.currentTimeMillis())).concat(".txt")));
            }
            logWriter = new PrintWriter(logFile, "UTF-8");
         } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }



         /* Print results for workload and copy them to file */
         budgetIndex = 0;
         for (double b : budgets) {
            double actualBudget = 0;
            if (!is_fixed_budget)
               actualBudget = b * avgBudget * initialTrainingSamples;
            else
               actualBudget = b * avgBudget * fixed_budget;
            for (double e : epsilons) {
               for (Lynceus.optimizer o : optimizers) {
                  printResults(hL, o, b, e, id, seeds, writer, budgetIndex, duration, avgSeedDuration, actualBudget, logWriter, seedDurations);
               }
            }
            budgetIndex++;
         }
         writer.print("Overall duration = " + totalDuration + "\n");
         writer.close();
         logWriter.close();
      }
   }

   private static String setFileName(int id, String resultsFile) {
      String timeout = timeoutToStr(timeoutType);

      resultsFile = resultsFile + optimizer + "_timeout" + timeout + "_initSamples" + initialTrainingSamples + "_budget" + budget + "_lookahead" + horizon + "_gamma" + gamma + "_gh" + gh + "_maxTime" + max_time_perc + "_bootstrapMethod_" + bootstrap;

      if (target.equals(test.TENSORFLOW) || target.equals(test.TENSORFLOWSIZE)) {   // tensorflow has the acc parameter that the others don't have
         resultsFile = resultsFile + "_accThreshold" + acc + "_wkld" + file;
      } else if (target.equals(test.GIRAPH)) {
         resultsFile = resultsFile + "_wkld" + file;
      } else {
         resultsFile = resultsFile + "_wkld" + file;
      }

      return resultsFile + ".txt";
   }

   /*
   Optimizer timeout budget workload [wkld_param (acc or dataset)] threads horizon numInitSamples gamma max_time_perc gh numSeeds bootstrapMethod
    */

   private static Lynceus.optimizer fromString(String text) {
      for (Lynceus.optimizer b : Lynceus.optimizer.values()) {
         if (b.toString().equalsIgnoreCase(text)) {
            return b;
         }
      }
      throw new RuntimeException("Optimizer not recognized "+text);
   }

   private static void parseArgs(String[] args) {
      String input;
      int i = 0;
      input = args[i++];
      int searchSpaceSize = 1;
      int percentage_initialTrainingSamples = 0;

      optimizer = fromString(input);


      System.out.println("Main: Optimizer " + optimizer);

      input = args[i++];
      timeoutType = strToTimeout(input);
      Lynceus.setTIMEOUT(timeoutType);


      input = args[i++];
      double[] allowedBudgets = {1, 1.25, 1.5, 1.75, 2, 3, 4, 5, 6, 8, 10, 15, 20, 30, 40, 60, 1000};
      boolean budgetSet = false;
      int j = 0;

      if (input.equalsIgnoreCase("all")) {
         allBudgets = true;
      } else {
         allBudgets = false;
         budget = Double.parseDouble(input);
         while (!budgetSet && j < allowedBudgets.length) {
            if (budget == allowedBudgets[j]) {
               budget = allowedBudgets[j];
               budgetSet = true;
            }
            j++;
         }
         if (!budgetSet) {
            budget = 10;
         }
      }

      System.out.println("Budget  = " + (allBudgets ? "all" : budget));

      input = args[i++];

      if (input.equalsIgnoreCase("scout")) {
         target = test.SCOUT;
         input = args[i++];
         WKLD_ID = Integer.parseInt(input);
         file = "SCOUT" + input;
         ScoutVMCostGenerator.setTargetWkld(Integer.parseInt(input));
         WekaScoutVMConfigFactory.setTargetWkld(Integer.parseInt(input));
         searchSpaceSize = 69;
      } else if (input.equalsIgnoreCase("extended_scout")) {
         target = test.EXTENDED_SCOUT;
         input = args[i++];
         WKLD_ID = Integer.parseInt(input);
         file = "EXT_SCOUT" + input;
         ExtendedScoutVMCostGenerator.setTargetWkld(Integer.parseInt(input));
         ExtendedWekaScoutVMConfigFactory.setTargetWkld(Integer.parseInt(input));
         searchSpaceSize = 69;
      } else if (input.equalsIgnoreCase("reduced_scout")) {
         target = test.REDUCED_SCOUT;
         input = args[i++];
         WKLD_ID = Integer.parseInt(input);
         file = "RED_SCOUT" + input;
         ReducedScoutVMCostGenerator.setTargetWkld(Integer.parseInt(input));
         ReducedWekaScoutVMConfigFactory.setTargetWkld(Integer.parseInt(input));
         searchSpaceSize = 69;
      } else if (input.equalsIgnoreCase("cherrypick")) {
         target = test.CHERRYPICK;
         input = args[i++];
         WKLD_ID = Integer.parseInt(input);
         file = "CP_" + input;
         CherrypickConfigCostGenerator.setTargetWkld(Integer.parseInt(input));
         WekaCherrypickConfigFactory.setTargetWkld(Integer.parseInt(input));
         CherrypickLHS.setTargetWkld(Integer.parseInt(input));
         if (Integer.parseInt(input) == 1) {   // kmeans wkld
            searchSpaceSize = 47;
         } else if (Integer.parseInt(input) == 2) {   // spark wkld
            searchSpaceSize = 59;
         } else if (Integer.parseInt(input) == 3) {   // tersort wkld
            searchSpaceSize = 72;
         } else if (Integer.parseInt(input) == 4) {   // tpcds wkld
            searchSpaceSize = 61;
         } else {
            searchSpaceSize = 52;
         }
      } else if (input.equalsIgnoreCase("tensorflow")) {
         target = test.TENSORFLOW;
         input = args[i++];
         if (Integer.parseInt(input) == 1) {   // t2_cnn_pruned
            file = "t2_cnn_intermediate_values_pruned";
            //file = "t2_pruned";
            WekaTensorflowConfigFactory.setPruned(true);
            searchSpaceSize = 288;
         } else if (Integer.parseInt(input) == 2) {   // t2_multilayer
            file = "t2_multilayer_intermediate_values";
            WekaTensorflowConfigFactory.setPruned(false);
            searchSpaceSize = 384;
         } else if (Integer.parseInt(input) == 3) {   // t2_rnn
            file = "t2_rnn";
            WekaTensorflowConfigFactory.setPruned(false);
            searchSpaceSize = 384;
         } else if (Integer.parseInt(input) == 4){   // t2_cnn
            file = "t2_cnn_intermediate_values";
        	//file = "cnn60000_avg";
            WekaTensorflowConfigFactory.setPruned(false);
            searchSpaceSize = 288;
         }else if (Integer.parseInt(input) == 5) {
            file = "cnn60000_avg";
            WekaTensorflowConfigFactory.setPruned(false);
            searchSpaceSize = 288;
         } else if (Integer.parseInt(input) == 6) {
             file = "multilayer60000_avg";
             WekaTensorflowConfigFactory.setPruned(false);
             searchSpaceSize = 288;
         } else {
        	 file = "rnn60000_avg";
        	 WekaTensorflowConfigFactory.setPruned(false);
        	 searchSpaceSize = 288;
         }
         
         datasetFile = "files/" + file + ".csv";
         System.out.println(datasetFile);
         WekaTensorflowConfigLynceus.setDatasetFile(datasetFile);

//         input = args[i++];
//         acc = Double.parseDouble(input);
//         if (acc < 0 || acc > 1) {
//            acc = 0.0;
//         }
         acc = 0.0;
         System.out.println("Acc " + acc);

         TensorflowConfigCostGenerator.setAccThreshold(acc);

      }else if (input.equalsIgnoreCase("tensorflowsize")) {
          target = test.TENSORFLOWSIZE;
          input = args[i++];
          if (Integer.parseInt(input) == 1) {
             file = "cnn";
             WekaTensorflowConfigFactory.setPruned(true);
             searchSpaceSize = 1440;
          } else if (Integer.parseInt(input) == 2) {
              file = "multilayer";
              WekaTensorflowConfigFactory.setPruned(false);
              searchSpaceSize = 1440;
          } else  {
              file = "rnn";
              WekaTensorflowConfigFactory.setPruned(false);
              searchSpaceSize = 1440;
          }
          datasetFile = "files/" + file + ".csv";
          System.out.println(datasetFile);
          WekaTensorflowConfigLynceusSize.setDatasetFile(datasetFile);
          
          acc = 0.0;
          System.out.println("Acc " + acc);

          TensorflowConfigCostGeneratorSize.setAccThreshold(acc);

      } else if (input.equalsIgnoreCase("giraph")) {
         target = test.GIRAPH;
         input = args[i++];
         searchSpaceSize = 42;
         if (Integer.parseInt(input) == 0) { // SSSP
            file = "SSSP";
            WekaGiraphConfigFactory.setWkld(0);
         } else if (Integer.parseInt(input) == 1) {   // PR
            file = "PR";
            WekaGiraphConfigFactory.setWkld(1);
         } else if (Integer.parseInt(input) == 2) {   // CC
            file = "CC";
            WekaGiraphConfigFactory.setWkld(2);
         } else {
            file = "SSSP";
            WekaGiraphConfigFactory.setWkld(0);
         }
         System.out.println("File " + file);

         datasetFile = "files/giraph_" + file + "_dataset.csv";
         WekaGiraphConfigLynceus.setDatasetFile(datasetFile);
      } else if (input.equalsIgnoreCase("giraph_plus")) {
          target = test.GIRAPH_PLUS;
          input = args[i++];
          searchSpaceSize = 42;
          if (Integer.parseInt(input) == 0) { // SSSP
             file = "SSSP";
             WekaGiraphPlusConfigFactory.setWkld(0);
          } else if (Integer.parseInt(input) == 1) {   // PR
             file = "PR";
             WekaGiraphPlusConfigFactory.setWkld(1);
          } else if (Integer.parseInt(input) == 2) {   // CC
             file = "CC";
             WekaGiraphPlusConfigFactory.setWkld(2);
          } else {
             file = "SSSP";
             WekaGiraphPlusConfigFactory.setWkld(0);
          }
          System.out.println("File " + file);

          datasetFile = "files/giraph_" + file + "_dataset.csv";
          WekaGiraphPlusConfigLynceus.setDatasetFile(datasetFile);
       } else if (input.equalsIgnoreCase("extended_giraph")) {
    	  target = test.EXTENDED_GIRAPH;
          input = args[i++];
          searchSpaceSize = 42;
          if (Integer.parseInt(input) == 0) { // SSSP
             file = "SSSP";
             ExtendedWekaGiraphConfigFactory.setWkld(0);
          } else if (Integer.parseInt(input) == 1) {   // PR
             file = "PR";
             ExtendedWekaGiraphConfigFactory.setWkld(1);
          } else if (Integer.parseInt(input) == 2) {   // CC
             file = "CC";
             ExtendedWekaGiraphConfigFactory.setWkld(2);
          } else {
             file = "SSSP";
             ExtendedWekaGiraphConfigFactory.setWkld(0);
          }
          System.out.println("File " + file);

          datasetFile = "files/giraph_" + file + "_dataset.csv";
          ExtendedWekaGiraphConfigLynceus.setDatasetFile(datasetFile);
      } else if (input.equalsIgnoreCase("extended_plus_giraph")) {
    	  target = test.EXTENDED_PLUS_GIRAPH;
          input = args[i++];
          searchSpaceSize = 42;
          if (Integer.parseInt(input) == 0) { // SSSP
             file = "SSSP";
             ExtendedPlusWekaGiraphConfigFactory.setWkld(0);
          } else if (Integer.parseInt(input) == 1) {   // PR
             file = "PR";
             ExtendedPlusWekaGiraphConfigFactory.setWkld(1);
          } else if (Integer.parseInt(input) == 2) {   // CC
             file = "CC";
             ExtendedPlusWekaGiraphConfigFactory.setWkld(2);
          } else {
             file = "SSSP";
             ExtendedPlusWekaGiraphConfigFactory.setWkld(0);
          }
          System.out.println("File " + file);

          datasetFile = "files/giraph_" + file + "_dataset.csv";
          ExtendedPlusWekaGiraphConfigLynceus.setDatasetFile(datasetFile);
      } else if (input.equalsIgnoreCase("tm")) {
         target = test.TM;
         input = args[i++];
         WKLD_ID = Integer.parseInt(input);
         file = "TM" + input;
         System.out.println(file);
         TMCostGenerator.setTargetWkld(Integer.parseInt(input));
      } else {
         System.out.println("\nChoosing default workload: Tensorflow");
         target = test.TENSORFLOW;
      }

      input = args[i++];

      int threads = Integer.parseInt(input);
      if (threads <= 0) {
         //These are already hardware cores.
         threads = (int) (((double) Runtime.getRuntime().availableProcessors()) * 1.5D);

      }

      System.out.println("Threads " + threads);

      Lynceus.setTHREADS(threads);

      input = args[i++];

      horizon = Integer.parseInt(input);
      if (horizon < 0) {      //We allow zero-horizon to fall back to a cherrypick-like  optimizer
         horizon = 2;
      }

      System.out.println("Horizon " + horizon);

      input = args[i++];


      percentage_initialTrainingSamples = Integer.parseInt(input);
      if (searchSpaceSize != 1) {
         initialTrainingSamples = (int) Math.round(percentage_initialTrainingSamples / 100.0 * searchSpaceSize);
      } else {
         initialTrainingSamples = percentage_initialTrainingSamples;
      }
      if (initialTrainingSamples <= 0) {
         initialTrainingSamples = 5;
      }
      Lynceus.setInitTrainingSamples(initialTrainingSamples);
      System.out.println("TrainSample " + initialTrainingSamples);


      input = args[i++];
      gamma = Double.parseDouble(input);
      System.out.println("Gamma " + gamma);

      input = args[i++];
      max_time_perc = Double.parseDouble(input) / 100D;
      System.out.println("Max Perc " + max_time_perc);


      //TODO: Our ExpParam does not take gamma nor gh nor lookahead as input
      //It means that it has to be fixed

      input = args[i++];
      gh = Long.parseLong(input);
      Lynceus.setGaussHermitePoints((int) gh);
      System.out.println("gh " + gh);

      //numSeeds = 50;
      input = args[i++];
      numSeeds = Integer.parseInt(input);
      System.out.println("numSeeds " + numSeeds);

      bootstrap = args[i++];
      System.out.println("bootstrap method " + bootstrap);

   }


   private static void printResults(HashMap<ExpParam, OptResult> hL, Lynceus.optimizer opt, double b, double e,
                                    long id, long[] seeds, PrintWriter writer, int budgetIndex, double[] duration, double[] avgSeedDuration, double actualBudget, PrintWriter logWriter, ArrayList<ArrayList<Double>> seedDurations) {

      double avgDist = 0, avgtimeExp = 0, avgExpl = 0, avgCost = 0, avgInitDist = 0, avgError = 0, avgStdv = 0, avgExplToOpt = 0;
      double p99Dist = -1, p99timeExp = -1, p99Expl = -1, p99Cost = -1, p99InitDist = -1, p99Error = -1, p99ExplToOpt = -1;
      double p90Dist = -1, p90timeExp = -1, p90Expl = -1, p90Cost = -1, p90InitDist = -1, p90Error = -1, p90ExplToOpt = -1;
      double p50Dist = -1, p50timeExp = -1, p50Expl = -1, p50Cost = -1, p50InitDist = -1, p50Error = -1, p50ExplToOpt = -1;

      int i = 0;

      double withinTMax = 0;

      /* check how many results comply with the constraint */
      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         if (o.isWithinTmax()) {
            withinTMax++;
         }
      }

      double[] dists = new double[(int) withinTMax];
      double[] timeExp = new double[(int) withinTMax];
      double[] expls = new double[(int) withinTMax];
      double[] costs = new double[(int) withinTMax];
      double[] init_dists = new double[(int) withinTMax];
      double[] estimationErrors = new double[(int) withinTMax];
      double[] numTimeouts = new double[(int) withinTMax];
      double[] stdvs = new double[(int) withinTMax];
      double[] explsToOpt = new double[(int) withinTMax];

      ArrayList<Double> exec_times = new ArrayList<Double>();
      exec_times = seedDurations.get(budgetIndex);

      logWriter.println("system;wkld;budget;bootstrapMethod;initSamples;lookahead;gamma;gh;timeout;numSeeds;seed;maxTimePerc;dfo;nex;cost;estimationError;numTimeouts;exec_time;init_DFO;nexToOpt;ExpTime");
      String timeout = timeoutToStr(timeoutType);

      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         if (o.isWithinTmax()) {
            dists[i] = o.getDistFromOpt();
            timeExp[i] = o.getExperimentTime()/60.0;//minutes
            expls[i] = o.getExplNumExpls();
            costs[i] = o.getExplCost();
            init_dists[i] = o.getInitDistFromOpt();
            estimationErrors[i] = o.getEstimationError();
            numTimeouts[i] = o.getNumTimeouts();
            explsToOpt[i] = o.getNexToOpt();
            //System.out.print("seed " + s + " ==> " + dists[i] + " ; " + expls[i] + " ; " + costs[i] + " ### ");
            avgDist += o.getDistFromOpt();
            avgtimeExp += o.getExperimentTime()/60.0;
            avgExpl += o.getExplNumExpls();
            avgCost += o.getExplCost();
            avgInitDist += o.getInitDistFromOpt();
            avgError += o.getEstimationError();
            avgExplToOpt += o.getNexToOpt();
            logWriter.println(opt + ";" + file + ";" + b + ";" + bootstrap + ";" + initialTrainingSamples + ";" + horizon + ";" + gamma + ";" + gh + ";" + timeout + ";" + numSeeds + ";" + +s + ";" + max_time_perc + ";" + dists[i] + ";" + expls[i] + ";" + costs[i] + ";" + estimationErrors[i] + ";" + numTimeouts[i] + ";" + exec_times.get((int) s - 1) + ";" + init_dists[i] + ";" + explsToOpt[i] + ";" +  timeExp[i]);
            i++;
         }
      }

      if (dists.length > 0) {
	      avgDist /= (int) withinTMax;
	      avgtimeExp /= (int) withinTMax;
	      avgCost /= (int) withinTMax;
	      avgExpl /= (int) withinTMax;
	      avgInitDist /= (int) withinTMax;
	      avgError /= (int) withinTMax;
	      avgExplToOpt /= (int) withinTMax;
	      Arrays.sort(dists);
	      Arrays.sort(timeExp);
	      Arrays.sort(expls);
	      Arrays.sort(costs);
	      Arrays.sort(init_dists);
	      Arrays.sort(estimationErrors);
	      Arrays.sort(explsToOpt);

	      // this needs to be computed here instead of in the previous
	      // loop because the avgDist is necessary to compute the stdv
	      i = 0;
	      for (long s : seeds) {
	         ExpParam expParam = new ExpParam(b, e, s, id, opt);
	         OptResult o = hL.get(expParam);
	         if (o == null) {
	            continue;
	         }
	         if (o.isWithinTmax()) {
	            stdvs[i] = Math.pow(o.getDistFromOpt() - avgDist, 2);
	            avgStdv += stdvs[i];
	            i++;
	         }
	      }

	      avgStdv /= (int) withinTMax;
	      avgStdv = Math.sqrt(avgStdv);

	      int index_99 = (int) Math.floor((99.0 / 100) * ((double) withinTMax));
	      if (index_99 == (int) withinTMax)
	         index_99 = (int) withinTMax - 2; //not the max

	      p99Cost = costs[index_99];
	      p99timeExp = timeExp[index_99];
	      p99Expl = expls[index_99];
	      p99Dist = dists[index_99];
	      p99InitDist = init_dists[index_99];
	      p99Error = estimationErrors[index_99];
	      p99ExplToOpt = explsToOpt[index_99];

	      int index_90 = (int) Math.floor((90.0 / 100) * ((double) withinTMax));
	      if (index_90 == (int) withinTMax)
	         index_90 = (int) withinTMax - 2; //not the max

	      p90Cost = costs[index_90];
	      p90timeExp = timeExp[index_90];
	      p90Expl = expls[index_90];
	      p90Dist = dists[index_90];
	      p90InitDist = init_dists[index_90];
	      p90Error = estimationErrors[index_90];
	      p90ExplToOpt = explsToOpt[index_90];

	      int index_50 = (int) Math.floor((50.0 / 100) * ((double) withinTMax));
	      if (index_50 == (int) withinTMax)
	         index_50 = (int) withinTMax - 2; //not the max

	      p50Cost = costs[index_50];
	      p50timeExp = timeExp[index_50];
	      p50Expl = expls[index_50];
	      p50Dist = dists[index_50];
	      p50InitDist = init_dists[index_50];
	      p50Error = estimationErrors[index_50];
	      p50ExplToOpt = explsToOpt[index_50];
      }

      String dfos = dfos(hL, opt, b, e, id, seeds);


      writeResults(writer, opt, id, b, actualBudget, e, avgDist, avgtimeExp, avgExpl, avgCost, p50Dist, p50timeExp, p50Expl, p50Cost, p90Dist, p90timeExp, p90Expl, p90Cost, p99Dist, p99timeExp, p99Expl, p99Cost, withinTMax, seeds.length, budgetIndex, duration, avgSeedDuration, file, acc, target, horizon, gamma, gh, dfos, avgInitDist, p50InitDist, p90InitDist, p99InitDist, avgError, p50Error, p90Error, p99Error, avgStdv, p50ExplToOpt, p90ExplToOpt, p99ExplToOpt, avgExplToOpt);

      System.out.println("\n" + opt + ";" + id + ";" + b + ";" + actualBudget + ";" + e + ";" + avgDist + ";" + avgtimeExp + ";"+ avgExpl + ";" + avgCost + ";" + avgError + ";" + p50Dist + ";" + p50timeExp + ";" + p50Expl + ";" + p50Cost + ";" + p50Error + ";" + p90Dist + ";" + p90timeExp + ";" + p90Expl + ";" + p90Cost + ";" + p90Error + ";" + p99Dist + ";" + p99timeExp + ";" + p99Expl + ";" + p99Cost + ";" + p99Error + "; " + withinTMax + "; " + seeds.length);
   }

   /* print results to the output result's file */
   private static void writeResults(PrintWriter writer, Lynceus.optimizer opt, long id, double budget, double actualBudget, double e, double avgDist, double avgtimeExp, double avgExpl, double avgCost, double p50Dist, double p50timeExp, double p50Expl, double p50Cost, double p90Dist, double p90timeExp, double p90Expl, double p90Cost, double p99Dist, double p99timeExp, double p99Expl, double p99Cost, double withinTMax, int numSeeds, int budgetIndex, double[] duration, double[] avgSeedDuration, String file, double acc, test job, double horizon, double gamma, long gh, String dfos, double avgInitDist, double p50InitDist, double p90InitDist, double p99InitDist, double avgError, double p50Error, double p90Error, double p99Error, double avgStdv, double p50ExplToOpt, double p90ExplToOpt, double p99ExplToOpt, double avgExplToOpt) {
      if (target == test.SCOUT || target == test.EXTENDED_SCOUT || target == test.REDUCED_SCOUT)
         id = (long) WKLD_ID;

      String timeout = timeoutToStr(timeoutType);

      String line = opt + ";wkldID=" + id + ";budget=" + budget + ";money=" + actualBudget + ";e=" + e + ";A-DFO=" + avgDist + ";A-ExpTime=" + avgtimeExp +";A-NEX=" + avgExpl + ";A-CEX=" + avgCost + ";A-STDV=" + avgStdv + ";50-DFO=" + p50Dist + ";50-ExpTime=" + p50timeExp +";50-NEX=" + p50Expl + ";50-CEX=" + p50Cost + ";90-DFO=" + p90Dist + ";90-ExpTime=" + p90timeExp +";90-NEX=" + p90Expl + ";90-CEX=" + p90Cost + ";99-DFO=" + p99Dist + ";99-ExpTime=" + p99timeExp +";99-NEX= " + p99Expl + ";99-CEX=" + p99Cost + ";constraint=" + withinTMax + ";numSeeds=" + numSeeds + ";duration=" + duration[budgetIndex] + ";avgSeedDuration=" + avgSeedDuration[budgetIndex] + ";wkld=" + file + ";timeout=" + timeout + ";acc=" + acc + ";job=" + job + ";horizon=" + horizon + ";gamma=" + gamma + ";gh=" + gh + ";max_time=" + max_time + ";max_time_perc=" + max_time_perc + ";num_init_samples=" + initialTrainingSamples + ";" + dfos + ";A-EERR=" + avgError + ";50-EERR=" + p50Error + ";90-EERR=" + p90Error + ";99-EERR=" + p99Error + ";A-initDFO=" + avgInitDist + ";50-initDFO=" + p50InitDist + ";90-initDFO=" + p90InitDist + ";99-initDFO=" + p99InitDist + ";bootstrapMethod=" + bootstrap + ";AVG-NEX-TO-OPT=" + avgExplToOpt + ";50-NEX-TO-OPT=" + p50ExplToOpt + ";90-NEX-TO-OPT=" + p90ExplToOpt + ";99-NEX-TO-OPT=" + p99ExplToOpt + "\n ";
      writer.println(line);
   }

   /* return a string with the stats considering all seeds, regardless of the constraint */
   private static String dfos(HashMap<ExpParam, OptResult> hL, Lynceus.optimizer opt, double b, double e, long id, long[] seeds) {
      double avgDist = 0, avgExpl = 0, avgCost = 0, avgInitDist = 0, avgError = 0, avgStdv = 0, avgExplToOpt = 0;
      double p99Dist = -1, p99Expl = -1, p99Cost = -1, p99InitDist = -1, p99Error = -1, p99ExplToOpt = -1;
      double p90Dist = -1, p90Expl = -1, p90Cost = -1, p90InitDist = -1, p90Error = -1, p90ExplToOpt = -1;
      double p50Dist = -1, p50Expl = -1, p50Cost = -1, p50InitDist = -1, p50Error = -1, p50ExplToOpt = -1;
      int i = 0;

      double[] dists = new double[seeds.length];
      double[] expls = new double[seeds.length];
      double[] costs = new double[seeds.length];
      double[] init_dists = new double[seeds.length];
      double[] estimationError = new double[seeds.length];
      double[] stdvs = new double[seeds.length];
      double[] explsToOpt = new double[seeds.length];

      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         dists[i] = o.getDistFromOpt();
         expls[i] = o.getExplNumExpls();
         costs[i] = o.getExplCost();
         init_dists[i] = o.getInitDistFromOpt();
         estimationError[i] = o.getEstimationError();
         explsToOpt[i] = o.getNexToOpt();
         avgDist += o.getDistFromOpt();
         avgExpl += o.getExplNumExpls();
         avgCost += o.getExplCost();
         avgInitDist += o.getInitDistFromOpt();
         avgError += o.getEstimationError();
         avgExplToOpt += o.getNexToOpt();
         i++;
      }

      System.out.println(dists.length);
      if (dists.length == 0)
         return "ALL_A-DFO=" + avgDist + ";ALL_A-NEX=" + avgExpl + ";ALL_A-CEX=" + avgCost + ";ALL_50-DFO=" + p50Dist + ";ALL_50-NEX = " + p50Expl + ";ALL_50-CEX=" + p50Cost + ";ALL_90-DFO=" + p90Dist + ";ALL_90-NEX=" + p90Expl + ";ALL_90-CEX=" + p90Cost + ";ALL_99-DFO=" + p99Dist + ";ALL_99-NEX= " + p99Expl + ";ALL_99-CEX=" + p99Cost + ";ALL_A-EERR=" + avgError + ";ALL_50-EERR=" + p50Error + ";ALL_90-EERR=" + p90Error + ";ALL_99-EERR=" + p99Error + ";ALL_A-INIT-DFO=" + avgInitDist + ";ALL_50-INIT-DFO=" + p50InitDist + ";ALL_90-INIT-DFO=" + p90InitDist + ";ALL_99-INIT-DFO=" + p99InitDist + ";ALL_AVG-NEX-TO-OPT=" + avgExplToOpt + ";ALL_50-NEX-TO-OPT=" + p50ExplToOpt + ";ALL_90-NEX-TO-OPT=" + p90ExplToOpt + ";ALL_99-NEX-TO-OPT=" + p99ExplToOpt;

      avgDist /= seeds.length;
      avgCost /= seeds.length;
      avgExpl /= seeds.length;
      avgInitDist /= seeds.length;
      avgError /= seeds.length;
      avgExplToOpt /= seeds.length;
      Arrays.sort(dists);
      Arrays.sort(expls);
      Arrays.sort(costs);
      Arrays.sort(init_dists);
      Arrays.sort(estimationError);
      Arrays.sort(explsToOpt);

      i = 0;
      for (long s : seeds) {
         ExpParam expParam = new ExpParam(b, e, s, id, opt);
         OptResult o = hL.get(expParam);
         if (o == null) {
            continue;
         }
         stdvs[i] = Math.pow(o.getDistFromOpt() - avgDist, 2);
         avgStdv += stdvs[i];
         i++;
      }
      avgStdv /= (int) seeds.length;
      avgStdv = Math.sqrt(avgStdv);

      int index_99 = (int) Math.floor((99.0 / 100) * ((double) seeds.length));
      if (index_99 == seeds.length)
         index_99 = seeds.length - 2; //not the max

      p99Cost = costs[index_99];
      p99Expl = expls[index_99];
      p99Dist = dists[index_99];
      p99InitDist = init_dists[index_99];
      p99Error = estimationError[index_99];
      p99ExplToOpt = explsToOpt[index_99];

      int index_90 = (int) Math.floor((90.0 / 100) * ((double) seeds.length));
      if (index_90 == seeds.length)
         index_90 = seeds.length - 2; //not the max

      p90Cost = costs[index_90];
      p90Expl = expls[index_90];
      p90Dist = dists[index_90];
      p90InitDist = init_dists[index_90];
      p90Error = estimationError[index_90];
      p90ExplToOpt = explsToOpt[index_90];

      int index_50 = (int) Math.floor((50.0 / 100) * ((double) seeds.length));
      if (index_50 == seeds.length)
         index_50 = seeds.length - 2; //not the max

      p50Cost = costs[index_50];
      p50Expl = expls[index_50];
      p50Dist = dists[index_50];
      p50InitDist = init_dists[index_50];
      p50Error = estimationError[index_50];
      p50ExplToOpt = explsToOpt[index_50];

      return "ALL_A-DFO=" + avgDist + ";ALL_A_STDV=" + avgStdv + ";ALL_A-NEX=" + avgExpl + ";ALL_A-CEX=" + avgCost + ";ALL_50-DFO=" + p50Dist + ";ALL_50-NEX = " + p50Expl + ";ALL_50-CEX=" + p50Cost + ";ALL_90-DFO=" + p90Dist + ";ALL_90-NEX=" + p90Expl + ";ALL_90-CEX=" + p90Cost + ";ALL_99-DFO=" + p99Dist + ";ALL_99-NEX= " + p99Expl + ";ALL_99-CEX=" + p99Cost + ";ALL_A-EERR=" + avgError + ";ALL_50-EERR=" + p50Error + ";ALL_90-EERR=" + p90Error + ";ALL_99-EERR=" + p99Error + ";ALL_A-INIT-DFO=" + avgInitDist + ";ALL_50-INIT-DFO=" + p50InitDist + ";ALL_90-INIT-DFO=" + p90InitDist + ";ALL_99-INIT-DFO=" + p99InitDist + ";ALL_AVG-NEX-TO-OPT=" + avgExplToOpt + ";ALL_50-NEX-TO-OPT=" + p50ExplToOpt + ";ALL_90-NEX-TO-OPT=" + p90ExplToOpt + ";ALL_99-NEX-TO-OPT=" + p99ExplToOpt;
   }

}
