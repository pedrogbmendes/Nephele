package weka;

import java.util.concurrent.atomic.AtomicLong;

import jep.JepGP;
import lynceus.Configuration;
import lynceus.PredictiveModel;
import lynceus.TestSet;
import lynceus.TrainingSet;
import lynceus.WekaConfiguration;
import lynceus.cherrypick.CherrypickConfig;
import lynceus.giraph.ExtendedGiraphConfig;
import lynceus.giraph.ExtendedPlusGiraphConfig;
import lynceus.giraph.GiraphConfig;
import lynceus.giraph.GiraphPlusConfig;
import lynceus.scout.ExtendedScoutVMConfig;
import lynceus.scout.ReducedScoutVMConfig;
import lynceus.scout.ScoutVMConfig;
import lynceus.tensorflow.TensorflowConfig;
import lynceus.tensorflow.TensorflowConfigSize;
import lynceus.tm.TMConfig;
import lynceus.vm.VMConfig;
import weka.cherrypick.WekaCherrypickConfig;
import weka.classifiers.Classifier;
import weka.classifiers.functions.GaussianProcesses;
import weka.core.Instance;
import weka.core.Instances;
import weka.ensemble.EnsembleClassifier;
import weka.extendedGiraph.ExtendedWekaGiraphConfig;
import weka.extendedPlusGiraph.ExtendedPlusWekaGiraphConfig;
import weka.extendedScout.ExtendedWekaScoutVMConfig;
import weka.gauss.LynGaussianProcess;
import weka.giraph.WekaGiraphConfig;
import weka.giraphPlus.WekaGiraphPlusConfig;
import weka.reducedScout.ReducedWekaScoutVMConfig;
import weka.scout.WekaScoutVMConfig;
import weka.tensorflow.WekaTensorflowConfig;
import weka.tensorflow.WekaTensorflowConfigSize;
import weka.tm.WekaTMConfig;
import weka.tuning.ModelParams;
import weka.vm.WekaVMConfig;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public class WekaGaussianProcess<C extends Configuration> implements PredictiveModel<C, WekaModelSample> {

   /* class attributes */
   protected Classifier model;
   protected WekaSet<C> trainingSet;
   protected WekaSet<C> testSet;

   private boolean trained = false;

   public static boolean ensemble = true;
   public final static boolean jep = false;
   public final static boolean useNominalAttributes = ensemble || true;
   public final static boolean debug_gp = false;

   private final static int numClassifiers = 5;

   private static final AtomicLong _id = new AtomicLong(0);

   //Hack
   public Classifier peekClassifier(){
      return model;
   }

   /* class constructor */
   public WekaGaussianProcess(WekaSet<C> set, ModelParams params) {

      this.trainingSet = set;

      if (!ensemble) {
         try {
            if (!jep) {
               this.model = new LynGaussianProcess(set.instances(),params).build();
            } else {
               this.model = new JepGP("" + _id.getAndIncrement());
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

      } else {
         //if (numClassifiers < 10) {
         //   throw new RuntimeException("I strongly suggest you to use at least 10 learners.");
         //IF not, I've seen the EI never converging...
         //}
         if (set.instances().size() < 10) {
            this.model = new EnsembleClassifier(set.instances().size(), 5);
         } else {
            this.model = new EnsembleClassifier(numClassifiers, 5);
         }
      }
   }

   
   /* class constructor */
   public WekaGaussianProcess(WekaSet<C> set, ModelParams params, WekaSet<C> testSet, long seed) {

      this.trainingSet = set;
      this.testSet = testSet;
      
      if (!ensemble) {
         try {
            if (!jep) {
               this.model = new LynGaussianProcess(set.instances(),params,testSet.instances()).build(seed);
            } else {
               this.model = new JepGP("" + _id.getAndIncrement());
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

      } else {
         //if (numClassifiers < 10) {
         //   throw new RuntimeException("I strongly suggest you to use at least 10 learners.");
         //IF not, I've seen the EI never converging...
         //}
         if (set.instances().size() < 10) {
            this.model = new EnsembleClassifier(set.instances().size(), 5);
         } else {
            this.model = new EnsembleClassifier(numClassifiers, 5);
         }
      }
   }

   

   /* getters */
   public Classifier getModel() {
      return this.model;
   }


   /* other methods */
   private double stddev(Instances instances) {
      double mean = 0;
      double std = 0;
      for (Instance instance : instances) {
         mean += instance.value(instance.numAttributes() - 1);
      }
      mean /= instances.size();
      for (Instance instance : instances) {
         std += (instance.value(instance.numAttributes() - 1) - mean) * (instance.value(instance.numAttributes() - 1) - mean);
      }
      std /= instances.size();
      System.out.println("Mean " + mean + " stdv " + Math.sqrt(std));
      return Math.sqrt(std);
   }

   public void test(TestSet<C, WekaModelSample> testSet) {
      if (!trained) {
         throw new RuntimeException("Not trained");
      }
      WekaSet ws = (WekaSet) testSet;

      System.out.println("INSTANCE: real VS prediction");
      for (Instance instance : ws.instances()) {
         try {
            Instance ii = (Instance) instance.copy();
            ii.setClassMissing();
            double pred = this.model.classifyInstance(instance);
            double real = instance.classValue();
            //System.out.println(instance + ": " + real + " vs " + pred);
         } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
         }
      }
   }

   public void testOnTrain(TrainingSet<C, WekaModelSample> testSet) {
      if (!trained) {
         throw new RuntimeException("Not trained");
      }
      WekaSet ws = (WekaSet) testSet;
      for (Instance instance : ws.instances()) {
         try {
            Instance ii = (Instance) instance.copy();
            ii.setClassMissing();
            double pred = this.model.classifyInstance(ii);
            double real = instance.classValue();
            //System.out.println(instance + ": " + pred + " vs " + real);
         } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
         }
      }
   }

   public void train() {
      if (trained) return;
      try {
         model.buildClassifier(this.trainingSet.instances());
         trained = true;
         //System.out.println(((GaussianProcesses) model).toString() + " " + Arrays.toString(((GaussianProcesses) model).getOptions()));

      } catch (Exception e) {
         e.printStackTrace();  // TODO: Customise this generated block
         throw new RuntimeException();
      }
   }

   private WekaConfiguration wekaconfigFromConfig(C config) {
      if (config instanceof TMConfig) {
         return new WekaTMConfig((TMConfig) config, this.trainingSet.instances());
      } else if (config instanceof VMConfig) {
         return new WekaVMConfig((VMConfig) config, this.trainingSet.instances());
      } else if (config instanceof ScoutVMConfig) {
         return new WekaScoutVMConfig((ScoutVMConfig) config, this.trainingSet.instances());
      } else if (config instanceof ExtendedScoutVMConfig) {
         return new ExtendedWekaScoutVMConfig((ExtendedScoutVMConfig) config, this.trainingSet.instances());
      } else if (config instanceof ReducedScoutVMConfig) {
         return new ReducedWekaScoutVMConfig((ReducedScoutVMConfig) config, this.trainingSet.instances());
      } else if (config instanceof TensorflowConfig) {
    	  return new WekaTensorflowConfig((TensorflowConfig) config, this.trainingSet.instances());
      }else if (config instanceof TensorflowConfigSize) {
       	  return new WekaTensorflowConfigSize((TensorflowConfigSize) config, this.trainingSet.instances());
      } else if (config instanceof GiraphConfig){
    	  return new WekaGiraphConfig((GiraphConfig) config, this.trainingSet.instances());
      } else if (config instanceof GiraphPlusConfig){
    	  return new WekaGiraphPlusConfig((GiraphPlusConfig) config, this.trainingSet.instances());
      } else if (config instanceof ExtendedGiraphConfig){
    	  return new ExtendedWekaGiraphConfig((ExtendedGiraphConfig) config, this.trainingSet.instances());
      } else if (config instanceof ExtendedPlusGiraphConfig){
    	  return new ExtendedPlusWekaGiraphConfig((ExtendedPlusGiraphConfig) config, this.trainingSet.instances());
      } else if (config instanceof CherrypickConfig) {
    	  return new WekaCherrypickConfig ((CherrypickConfig) config, this.trainingSet.instances());
   	  } else {
         throw new RuntimeException(config.getClass() + " not supported");
      }
   }

   public double evaluate(C config) {
      if (!trained) {
         throw new RuntimeException("[WekaGaussianProcess] Not trained");
      }
      try {
         WekaConfiguration wc = wekaconfigFromConfig(config);
         final Instance ii = wc.toInstance();
         ii.setClassMissing();
         double v = model.classifyInstance(ii);
         //double[] vv = model.distributionForInstance(ii);
         return v;
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException();
      }
   }

   //just to be called from Hack...
   protected WekaGaussianProcess() {

   }

   public double stdv(C config) {

      if (!trained) {
         throw new RuntimeException("Not trained");
      }
      final Instance i = wekaconfigFromConfig(config).toInstance();
      if (!ensemble) {
         try {
            if (jep)
               return ((JepGP) model).getStandardDeviation(i);
            return ((GaussianProcesses) model).getStandardDeviation(i);
         } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
         }
      } else {
         return ((EnsembleClassifier) model).getStandardDeviation(i);
      }
   }

   /**
    * Find the maximum variance amongst all UNexplored configs
    *
    * @param testSet (set of the unexplored configs)
    * @return maximum variance
    */
   public double maxVariance(TestSet<C, WekaModelSample> testSet) {
      double maxStdv = Double.NEGATIVE_INFINITY;
      double curr_stdv = 0;

      WekaSet<C> ws = (WekaSet<C>) testSet;

      for (Instance i : ws.instances()) {
         if (ensemble)
            curr_stdv = ((EnsembleClassifier) model).getStandardDeviation(i);
         else {
            try {
               if (jep) {
                  curr_stdv = ((JepGP) model).getStandardDeviation(i);
               } else {
                  curr_stdv = ((GaussianProcesses) model).getStandardDeviation(i);
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }
         if (curr_stdv > maxStdv) {
            maxStdv = curr_stdv;
         }
      }

      return Math.sqrt(maxStdv);
   }

}
