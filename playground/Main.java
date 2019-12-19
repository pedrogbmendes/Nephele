package playground;

import java.io.File;
import java.util.Random;
import java.util.logging.LogManager;

import jep.Jep;
import jep.JepGP;
import lynceus.scout.ExtendedScoutVMCostGenerator;
import weka.classifiers.functions.GaussianProcesses;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.core.converters.Saver;
import weka.ensemble.EnsembleClassifier;
import weka.extendedScout.ExtendedWekaScoutVMConfig;
import weka.extendedScout.ExtendedWekaScoutVMConfigFactory;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NominalToBinary;
import weka.gauss.CustomGP;
import weka.gauss.LynGaussianProcess;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 2019-02-15
 */
public class Main {

   //1. Take full data set and obtain sub-train and sub-tests 3,4,5,6

   //2. Evaluate with our GP

   //3.


   private static void generateTrainTest() throws Exception {
      System.out.println("(Re)generating train/test");
      Instances allData = ExtendedWekaScoutVMConfigFactory.buildInitTestSet("files/extended_scout.arff").getInstancesCopy();
      ExtendedScoutVMCostGenerator.setTargetWkld(5);
      ExtendedScoutVMCostGenerator cg = new ExtendedScoutVMCostGenerator();

      int folds = 10;
      final Random r = new Random(918273645);
      for (int f = 0; f < folds; f++) {
         Instances train = allData.trainCV(folds, f, r);
         NominalToBinary nominalToBinary = new NominalToBinary();
         nominalToBinary.setInputFormat(train);
         Instances cloned;
         cloned = new Instances(train);
         train = Filter.useFilter(train, nominalToBinary);

         CSVSaver csvSaver = new CSVSaver();
         csvSaver.setRetrieval(Saver.INCREMENTAL);
         csvSaver.setInstances(train);
         csvSaver.setFile(new File("files/play/ext_scout_train_" + f + ".csv"));

         CSVSaver csvSaverLog = new CSVSaver();
         csvSaverLog.setRetrieval(Saver.INCREMENTAL);
         csvSaverLog.setInstances(train);
         csvSaverLog.setFile(new File("files/play/ext_scout_train_log_" + f + ".csv"));


         for (int i = 0; i < train.numInstances(); i++) {
            Instance in = cloned.get(i);
            Instance in2 = train.get(i);
            double cost = cg.deploymentCost(null, new ExtendedWekaScoutVMConfig(in));
            in2.setClassValue(cost);
            csvSaver.writeIncremental(in2);

            double log_cost = Math.log(cost);
            Instance in2log = train.get(i);
            in2log.setClassValue(log_cost);
            csvSaverLog.writeIncremental(in2log);
         }
         csvSaver.getWriter().close();
         csvSaverLog.getWriter().close();


         Instances test = allData.testCV(folds, f);
         cloned = new Instances(test); //without filter, so that we can compute the deploymentCost
         nominalToBinary = new NominalToBinary();
         nominalToBinary.setInputFormat(test);
         test = Filter.useFilter(test, nominalToBinary);
         csvSaver = new CSVSaver();
         csvSaver.setRetrieval(Saver.INCREMENTAL);
         csvSaver.setInstances(test);
         csvSaver.setFile(new File("files/play/ext_scout_test_" + f + ".csv"));

         csvSaverLog = new CSVSaver();
         csvSaverLog.setRetrieval(Saver.INCREMENTAL);
         csvSaverLog.setInstances(test);
         csvSaverLog.setFile(new File("files/play/ext_scout_test_log_" + f + ".csv"));

         for (int i = 0; i < test.numInstances(); i++) {
            Instance in = cloned.get(i);
            Instance in2 = test.get(i);
            double cost = cg.deploymentCost(null, new ExtendedWekaScoutVMConfig(in));
            in2.setClassValue(cost);
            csvSaver.writeIncremental(in2);

            double log_cost = Math.log(cost);
            Instance in2log = train.get(i);
            in2log.setClassValue(log_cost);
            csvSaverLog.writeIncremental(in2log);
         }
         csvSaver.getWriter().close();
         csvSaverLog.getWriter().close();

      }
   }


   public static void compare() throws Exception {
      generateTrainTest();


      for (int f = 0; f < 1; f++) {

         CSVLoader loader = new CSVLoader();

         loader.setFile(new File("files/play/ext_scout_train_" + f + ".csv"));
         Instances train = loader.getDataSet();
         train.setClassIndex(train.numAttributes() - 1);

         loader = new CSVLoader();
         loader.setFile(new File("files/play/ext_scout_test_" + f + ".csv"));
         Instances test = loader.getDataSet();
         test.setClassIndex(test.numAttributes() - 1);

         LynGaussianProcess lgp = new LynGaussianProcess(train);
         GaussianProcesses gp = (GaussianProcesses) lgp.build();

         JepGP jepGP = new JepGP("1");
         jepGP.buildClassifier(train);


         for (Instance in : test) {
            double real = in.classValue();
            double predW = gp.classifyInstance(in);
            double stdW = gp.getStandardDeviation(in);
            double predJ = jepGP.classifyInstance(in);
            double stdJ = jepGP.getStandardDeviation(in);
            System.out.println("WEKA" + in + "( " + predW + ", " + stdW + ") err " + Math.abs(predW - real) / real);
            System.out.println("-JEP" + in + "( " + predJ + ", " + stdJ + ") err " + Math.abs(predJ - real) / real);

         }

      }

   }

   public static void evalWeka() throws Exception {
      for (int f = 9; f <= 9; f++) {
         CSVLoader loader = new CSVLoader();

         loader.setFile(new File("files/play/ext_scout_train_" + f + ".csv"));
         Instances train = loader.getDataSet();
         train.setClassIndex(train.numAttributes() - 1);

         loader = new CSVLoader();
         loader.setFile(new File("files/play/ext_scout_test_" + f + ".csv"));
         Instances test = loader.getDataSet();
         test.setClassIndex(test.numAttributes() - 1);

         LynGaussianProcess lgp = new LynGaussianProcess(train);
         GaussianProcesses gp = (GaussianProcesses) lgp.build();
         System.out.println("GP trained with theta " + ((CustomGP) gp).getTheta());

         EnsembleClassifier ensembleClassifier = new EnsembleClassifier(10, 5);
         ensembleClassifier.buildClassifier(train);

         System.out.println("GP");
         for (Instance in : test) {
            double real = in.classValue();
            double pred = gp.classifyInstance(in);
            double stdv = gp.getStandardDeviation(in);
            System.out.println(in + " => " + pred + ", " + stdv);
         }
         System.out.println("ENSEMBLE");
         for (Instance in : test) {
            double pred = ensembleClassifier.classifyInstance(in);
            double stdv = ensembleClassifier.getStandardDeviation(in);
            System.out.println(in + " => " + pred + ", " + stdv);
         }

      }
   }

   public static void evalJeb() throws Exception {
      System.out.println("JEB");

      Jep jep = new Jep();

      jep.eval("def attempt():\n" +
                     "    train = \"files/play/ext_scout_train_1.csv\"\n" +
                     "    test = \"files/play/ext_scout_test_1.csv\"\n" +
                     "    eval(train, test)");

      jep.eval("import numpy as np");
      jep.eval("from sklearn.gaussian_process import GaussianProcessRegressor");
      jep.eval("from sklearn.gaussian_process.kernels import (Matern)");
      jep.eval("def eval(train_file, test_file):\n" +
                     "    train_data = np.loadtxt(fname=train_file, delimiter=',', skiprows=1)\n" +
                     "    #print(train_data)\n" +
                     "    kernel = Matern(length_scale=1.0, length_scale_bounds=(1e-1, 10.0), nu=1.5)\n" +
                     "    gp = GaussianProcessRegressor(kernel=kernel)\n" +
                     "\n" +
                     "    test_data = np.loadtxt(fname=test_file, delimiter=',', skiprows=1)\n" +
                     "    last_col = np.size(train_data, 1)-1\n" +
                     "    X = np.delete(train_data, last_col, 1)\n" +
                     "    Y = np.delete(train_data, np.s_[0:last_col], 1)\n" +
                     "    #print(X)\n" +
                     "    #print(np.size(train_data, 0), \"x\", np.size(train_data, 1))\n" +
                     "    #print(np.size(X, 0), \"x\", np.size(X, 1))\n" +
                     "    #print(np.size(Y, 0), \"x\", np.size(Y, 1))\n" +
                     "    #print(Y)\n" +
                     "    gp.fit(X,Y)\n" +
                     "\n" +
                     "    Z = np.delete(test_data,last_col,1)\n" +
                     "    z,zs = gp.predict(Z,return_std = True)\n" +
                     "    W =  np.delete(test_data, np.s_[0:last_col], 1)\n" +
                     "    print(\"zed\",z)\n" +
                     "    print(\"zeds\",zs)\n" +
                     "    print(W)");

      jep.invoke("attempt");


   }

   public static void main(String[] args) throws Exception {
      LogManager.getLogManager().reset(); //TO remove te logging of
      //generateTrainTest();

      System.out.println("Main");
      evalWeka();
      //compare();
      //evalJeb();

   }
}
