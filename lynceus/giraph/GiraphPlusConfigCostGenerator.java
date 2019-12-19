package lynceus.giraph;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.State;

public class GiraphPlusConfigCostGenerator implements CostGenerator<GiraphPlusConfig> {

	/* class attributes */
	private double avgCost;
	private double maxTime;
	private double minTime;
	private Map<GiraphPlusConfig, Double> costMap = null;
	private Map<GiraphPlusConfig, Double> timeMap = null;
	private String dataset;
	
	/* class constructor */
	public GiraphPlusConfigCostGenerator(String dataset) throws IOException{
		this.dataset = dataset;
		updateCostOnWorkload(this.dataset);
	}

	
	/* getters */
	public double getAvgCost() {
		return avgCost;
	}

	public double getMinTime() {
		return this.minTime;
	}
   
	public double getMaxTime() {
		return maxTime;
	}
	  
	
	/* interface methods to be implemented */
	@Override
	public double setupCost(State state, GiraphPlusConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double deploymentCost(State state, GiraphPlusConfig config) {
		final Double cost = costMap.get(config);

	      if (cost == null)
	         throw new RuntimeException("[GiraphPlusConfigCostGenerator] " + config + " with hashcode " + config.hashCode() + " not in cost map for wkld " + this.dataset + " map size " + costMap.size());
	      return cost;
	}

	@Override
	public double costPerConfigPerMinute(GiraphPlusConfig config) {
		int num_instances = (config.getNr_workers() + 1);
		switch (config.getVm_type()) {
			case 0:
				switch (config.getVm_size()) {
					case 0: // c4.large
						return num_instances * (0.119 + 0.027)/60.0;
					case 1: // c4.xlarge
						return num_instances * (0.237 + 0.053)/60.0;
					case 2: // c4.2xlarge
						return num_instances * (0.476 + 0.105)/60.0;
					case 3: // c4.4xlarge
						return num_instances * (0.95 + 0.210)/60.0;
					default:
				           throw new RuntimeException("[GiraphPlusConfigCostGenerator] Inexistent vm flavor " + config.getVm_type() + "." + config.getVm_size());
				}
			case 1:
				switch (config.getVm_size()) {
					case 1: // r4.xlarge
						return num_instances * (0.312 + 0.067)/60.0;
					case 2: // r4.2xlarge
						return num_instances * (0.624 + 0.133)/60.0;
					case 3: // r4.4xlarge
						return num_instances * (1.248 + 0.266)/60.0;
					case 4: // r4.8xlarge
						return num_instances * (2.496 + 0.270)/60.0;
					default:
				           throw new RuntimeException("[GiraphPlusConfigCostGenerator] Inexistent vm flavor " + config.getVm_type() + "." + config.getVm_size());
				}
			default:
		           throw new RuntimeException("[GiraphPlusConfigCostGenerator] Inexistent vm flavor " + config.getVm_type() + "." + config.getVm_size());
		}

	}

	@Override
	public double[] getIntermediateValues(GiraphPlusConfig config, int amount, timeout timeoutType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getAccForSpecificTime(double time, GiraphPlusConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public double getAccForConfig(GiraphPlusConfig config) {
		// TODO Auto-generated method stub
		return -1;
	}
	

	/* other methods */
	private void updateCostOnWorkload(String dataset) throws IOException {
		FileReader csvData = null;
		int counter = 0;
		double t_max = 0;
        double t_min = Double.MAX_VALUE;
        
        costMap = new HashMap<>();
        timeMap = new HashMap<>();
		
		try{
			csvData = new FileReader(new File(dataset));
		}catch(Exception e) {
			System.out.print("[GiraphPlusConfigCostGenerator] ");
			 e.printStackTrace();
		}
		
		CSVParser parser = CSVParser.parse(csvData, CSVFormat.DEFAULT);
		for (CSVRecord csvRecord : parser) {
			if(counter >= 1){			    
			    String aux_graph	 = csvRecord.get(0);
			    String aux_wkld		 = csvRecord.get(1);
			    String flavor	 	 = csvRecord.get(2);
				int nr_workers		 = Integer.parseInt(csvRecord.get(3));
			    double time			 = Double.parseDouble(csvRecord.get(6)) / 60;	// = training time (mins)
			    
			    int vm_type = 0;
			    int vm_size = 0;
			    int graph = 0;
			    int wkld = 0;
			    
			    GiraphDirectory.GiraphVMFlavor vmFlavor;
			    
			    switch(flavor){
			    	case "c4.large":
			    		vm_type = 0;
			    		vm_size = 0;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.C4_LARGE;
			    		break;
			    	case "c4.xlarge":
			    		vm_type = 0;
			    		vm_size = 1;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.C4_XLARGE;
			    		break;
			    	case "c4.2xlarge":
			    		vm_type = 0;
			    		vm_size = 2;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.C4_2XLARGE;
			    		break;
			    	case "c4.4xlarge":
			    		vm_type = 0;
			    		vm_size = 3;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.C4_4XLARGE;
			    		break;
			    	case "r4.xlarge":
			    		vm_type = 1;
			    		vm_size = 1;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.R4_XLARGE;
			    		break;
			    	case "r4.2xlarge":
			    		vm_type = 1;
			    		vm_size = 2;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.R4_2XLARGE;
			    		break;
			    	case "r4.4xlarge":
			    		vm_type = 1;
			    		vm_size = 3;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.R4_4XLARGE;
			    		break;
			    	case "r4.8xlarge":
			    		vm_type = 1;
			    		vm_size = 4;
			    		vmFlavor = GiraphDirectory.GiraphVMFlavor.R4_8XLARGE;
			    		break;
			    	default:
				        throw new RuntimeException("[GiraphPlusConfigCostGenerator] Unknown flavor " + flavor);
			    }

			    
			    switch(aux_graph){
			    	case "Orkut":
			    		graph = 0;
			    		break;
			    	default:
			           throw new RuntimeException("[GiraphPlusConfigCostGenerator] Unknown graph type " + aux_graph);
			    }
		    
			    switch(aux_wkld){
			    	case "SSSP":
			    		wkld = 0;
			    		break;
			    	case "PageRank":
			    		wkld = 1;
			    		break;
			    	case "Connected Components":
			    		wkld = 2;
			    		break;
			    	default:
				        throw new RuntimeException("[GiraphPlusConfigCostGenerator] Unknown workload " + aux_wkld);
			    }
			    GiraphPlusConfig config = new GiraphPlusConfig(vm_type, vm_size, nr_workers, GiraphDirectory.vmCostPerMinFromAttributes(vmFlavor, nr_workers), graph, wkld);
			    
			    double cost = time * costPerConfigPerMinute(config);
	            costMap.put(config, cost);
	            timeMap.put(config, time);
	            
			    avgCost += cost;
	            if (time < t_min) {
	               t_min = time;
	            }
	            if (time > t_max) {
	            	t_max = time;
	            }
			}
			counter = counter + 1;
		}
		avgCost /= costMap.size();
        minTime = t_min;
        maxTime = t_max;
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
		for (Map.Entry<GiraphPlusConfig, Double> config : timeMap.entrySet()) {
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

	
}
