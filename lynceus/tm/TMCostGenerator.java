package lynceus.tm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.State;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 20.03.18
 */
public class TMCostGenerator implements CostGenerator<TMConfig> {

   /* class attributes */
   private final static String data = "files/lowerIsBetterDiego.csv";
   private static int targetWkld = 2;
   private Map<TMConfig, Double> costMap = null;
   private Map<TMConfig, Double> timeMap = null;

   private double avgCost;
   private double avgTime;
   private double maxTime;
   private double minTime;
   
   /* machine prices:
    * r5 = 0.126$$/hour for 2 cores
    * m5 = 0.096$$/hour for 2 cores
    * the price increases always 2x between number of cores
    * except from 16 to 48 (increases 3x) */
   private static double[] cores = {2.0, 4.0, 8.0, 16.0, 48.0, 96.0};
   private static double[] r5costsPerMin = {0.0021, 0.0042, 0.0084, 0.0168, 0.0504, 0.1008};
   private static double[] m5costsPerMin = {0.0016, 0.0032, 0.0064, 0.0128, 0.0384, 0.0768};
   
   /* class constructor */
   public TMCostGenerator() {
      updateCostOnWorkload(targetWkld);
   }
   
   /* class methods */
   
   /* getters and setters */
   public double getAvgCost() {
      return avgCost;
   }

   public static void setTargetWkld(int targetWkld) {
      TMCostGenerator.targetWkld = targetWkld;
   }

   public double getMinTime() {
      return this.minTime;
   }
   
   public double getMaxTime() {
      return maxTime;
   }
  
   /* interface methods to implement */
   @Override
   public double setupCost(State state, TMConfig config) {
      return 0;
   }

   @Override
   public double deploymentCost(State state, TMConfig config) {
      final Double cost = costMap.get(config);

      if (cost == null)
         throw new RuntimeException(config + " with hashcode " + config.hashCode() + " not in cost map for wkld " + targetWkld + " map size " + costMap.size());
      return cost;
   }

   @Override
   public double costPerConfigPerMinute(TMConfig config) {
	  double cost = 0.0;
	  
	  // if (true) return 1.0;//
//      cost = Math.pow(config.get_threads(), 0.3);	// or sqrt of it
//      if (config.get_tm().equals(TMConfig.tm.HTM)) {
//         cost *= 1.25; //htms are more costly
//      }
      
      int i = 0;
      boolean haveCost = false;
      while (i < cores.length && !haveCost) {
    	  if (config.get_threads() / cores[i] <= 1.0) {
    		  if (config.get_tm().equals(TMConfig.tm.HTM)) {
    			  cost = r5costsPerMin[i];
    		  } else {
    			  cost = m5costsPerMin[i];
    		  }
    		  haveCost = true;
    		  //System.out.println("nb threads = " + config.get_threads() + " ; cost = " + cost + " ; cores = " + cores[i]);
    	  }
    	  i ++;
      }
      
      return cost;
   }
   
   @Override
	public double getAccForSpecificTime(double time, TMConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}
   
	@Override
	public double getAccForConfig(TMConfig config) {
		// TODO Auto-generated method stub
		return -1;
	}

	@Override
	public double[] getIntermediateValues(TMConfig config, int amount, timeout timeoutType) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
   
   /* other methods */
   private void updateCostOnWorkload(int currWorkload) {
      try {
         //setup a map config-> cost taken from the file
         costMap = new HashMap<>();
         timeMap = new HashMap<>();
         avgCost = 0;
         avgTime = 0;
         double t_max = 0;
		 double t_min = Double.MAX_VALUE;
         String read = null;
         BufferedReader br = new BufferedReader(new FileReader(new File(data)));
         int i = 0;
         String header = br.readLine();
         String[] split_header = header.split(",");
         i++;
         while (i++ < currWorkload) {
            read = br.readLine();
         }
         assert read != null;
         System.out.println("Target wkld corresponds to row " + currWorkload);
         String[] kpis = read.split(",");
         System.out.println("Target wkld is " + kpis[0]);
         
         //out.write("\n"+kpis[0]+";");

         for (i = 1; i < kpis.length; i++) {
            TMConfig c = TMConfig.config(split_header[i]);
            double time = Double.parseDouble(kpis[i]);
            double cost =  time * costPerConfigPerMinute(c);
            
            //out.write(cost + ";");
            
            costMap.put(c, cost);
            timeMap.put(c, time);
            avgCost += cost;
            avgTime += time;
            if (time > t_max) {
			    t_max = time;
		    } else if (time < t_min) {
			    t_min = time;
		    }
         }
         avgCost /= (kpis.length - 1);
         avgTime /= (kpis.length - 1);
         maxTime = t_max;
		 minTime = t_min;
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

	/** 
	 * Method for finding the max_time that corresponds to only having a given percentage
	 * of the search space feasible
	 * @input feasible percentage of the search space
	 * @return max_time constraint (in min)
	 */
	public double getTimeForSpecificConstraint(double perc) {
		double[] times = new double[timeMap.size()];
		double maxTime = Double.POSITIVE_INFINITY;
		
		int numberOfFeasibleConfigs = (int) Math.round(timeMap.size()*perc);
		System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs);
		
		int i = 0;
		for (Map.Entry<TMConfig, Double> config : timeMap.entrySet()) {
			times[i] = config.getValue();
			i ++;
		}
		Arrays.sort(times);
		
		if (numberOfFeasibleConfigs == timeMap.size()) {
			maxTime = times[numberOfFeasibleConfigs-1]; // in min
		} else {
			maxTime = times[numberOfFeasibleConfigs]; // in min
		}
		
		System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs + " ; maxTime = " + maxTime);
		
		return maxTime;
	}

	
	/* write a file with the costs of each TM config for all benchmarks */
//	public static void main (String[] args){
//		
//		
//        BufferedWriter out = null;
//        
//        String file = "files/proteusTM_costs.txt";
//      
//        try {
//        	FileWriter writer = new FileWriter(file, true); //true tells to append data
//        	out = new BufferedWriter(writer);
//        } catch (IOException e) {
//            System.err.println("Error: " + e.getMessage());
//        }
//        
//        
//        for (int i = 2 ; i < 128; i++ ){
//        	TMCostGenerator.setTargetWkld(i);
//        	TMCostGenerator costGenerator = new TMCostGenerator(out);
//        }
//
//        try {
//			out.close();
//		} catch (IOException e) {
//			System.err.println("Error: " + e.getMessage());
//		}
//	}
        
}
