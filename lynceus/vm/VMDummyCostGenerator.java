package lynceus.vm;

import static lynceus.vm.VMConfig.m5_12xlarge;
import static lynceus.vm.VMConfig.m5_24xlarge;
import static lynceus.vm.VMConfig.m5_2xlarge;
import static lynceus.vm.VMConfig.m5_4xlarge;
import static lynceus.vm.VMConfig.m5_large;
import static lynceus.vm.VMConfig.m5_xlarge;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.State;
import lynceus.TestSet;
import weka.vm.WekaVMConfigFactory;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public class VMDummyCostGenerator implements CostGenerator<VMConfig> {

   private final VMConfig argmin;
   private final String arff;


   public VMDummyCostGenerator(String arff, long seed) {
      this.arff = arff;
      this.argmin = WekaVMConfigFactory.buildInitTestSet(arff).getConfig(120);
      //WekaVMConfigFactory.randomFromTestSet(arff, seed);
   }

   public long cumulativeDeployBudget() {
      TestSet<VMConfig, ?> test = WekaVMConfigFactory.buildInitTestSet(arff);
      double sum = 0;
      for (int i = 0; i < test.size(); i++) {
         VMConfig c = test.getConfig(i);
         sum += deploymentCost(null, c);
      }
      return (long) sum;
   }

   public double maxDeployBudget() {
      TestSet<VMConfig, ?> test = WekaVMConfigFactory.buildInitTestSet(arff);
      double max = 0;
      for (int i = 0; i < test.size(); i++) {
         double c = deploymentCost(null, test.getConfig(i));
         if (c > max)
            max = c;
      }
      return max;
   }


   public double setupCost(State state, VMConfig config) {
      /*
      Here we have to put the cost to setup a new config given
      the config currently deployed (state.currentConfig)
      */
      return 0.0D;
   }


   private double rastrigin(VMConfig config) {
      double sum = 0;
      final double A = 10.0;
      final double min = 1;
      /*
      I actually want that the higher is the processing power, the lower is the cost
       */
      double xi;
      for (int i = 1; i < argmin.numAttributes(); i++) {
         xi = ((Double)config.at(i)) - (Double) argmin.at(i); //shifted
         sum += Math.pow(xi, 2) - A * Math.cos(2 * Math.PI * xi);
      }
      return min + (A * argmin.numAttributes() + sum);
   }


   private double linearCost(VMConfig config) {
      double sum = 0;
      final double A = 1.0;
      double xi;
      for (int i = 1; i < argmin.numAttributes(); i++) {
         xi = ((Double)config.at(i)) - (Double) argmin.at(i); //shifted
         sum += Math.pow(xi, 2);
      }
      return A * sum;
   }

   private double dummyCost(VMConfig config) {
      return 1500 - (config.getNum_instances() * config.getNum_instances());
   }

   private double WallClockTimeToRunJob(VMConfig config) {
      //return rastrigin(config);
      return linearCost(config);
      //return dummyCost(config);
   }

   public double deploymentCost(State state, VMConfig config) {
      /*
      Here we have to plug a nonlinear function that gives a lower result
      depending on the type and number of vms.
       */
      // System.out.println("Cost is " + (WallClockTimeToRunJob(config)) + " * " + costPerConfigPerMinute(config));
      return 1.0 + (WallClockTimeToRunJob(config)) * costPerConfigPerMinute(config);

   }

   public double costPerConfigPerMinute(VMConfig config) {
      switch (config.type) {

         case m5_large:
            return 0.096;
         case m5_xlarge:
            return 0.192;
         case m5_2xlarge:
            return 0.384;
         case m5_4xlarge:
            return 0.768;
         case m5_12xlarge:
            return 2.304;
         case m5_24xlarge:
            return 4.608;
         default:
            throw new RuntimeException("No cost for " + config);

      }
   }

	@Override
	public double getAccForSpecificTime(double time, VMConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getAccForConfig(VMConfig config) {
		// TODO Auto-generated method stub
		return -1;
	}

	
	
	@Override
	public double[] getIntermediateValues(VMConfig config, int amount, timeout timeoutType) {
		// TODO Auto-generated method stub
		return null;
	}

   /*
   The Rastrigin has the minimum at the origin. I don't want that.
   So I shift it by first defining where is the minimum and then shiftin x,y,z,w by that
    */

   //https://github.com/giskou/Optimization/blob/master/src/libs/functions/Rastrigin.java
}
