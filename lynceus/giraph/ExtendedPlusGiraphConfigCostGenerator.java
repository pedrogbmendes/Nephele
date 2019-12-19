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

public class ExtendedPlusGiraphConfigCostGenerator implements CostGenerator<ExtendedPlusGiraphConfig> {

	/* class attributes */
	private double avgCost;
	private double maxTime;
	private double minTime;
	private Map<ExtendedPlusGiraphConfig, Double> costMap = null;
	private Map<ExtendedPlusGiraphConfig, Double> timeMap = null;
	private String dataset;
	
	/* class constructor */
	public ExtendedPlusGiraphConfigCostGenerator(String dataset) throws IOException{
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
	public double setupCost(State state, ExtendedPlusGiraphConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double deploymentCost(State state, ExtendedPlusGiraphConfig config) {
		final Double cost = costMap.get(config);

	      if (cost == null)
	         throw new RuntimeException("[ExtendedPlusGiraphConfigCostGenerator] " + config + " with hashcode " + config.hashCode() + " not in cost map for wkld " + this.dataset + " map size " + costMap.size());
	      return cost;
	}


	@Override
	public double costPerConfigPerMinute(ExtendedPlusGiraphConfig config) {
		int num_instances = (config.getNr_workers() + 1);
		switch(config.getVm_flavor()) {
			case C4_LARGE:
				return num_instances * (0.119 + 0.027)/60.0;
			case C4_XLARGE:
				return num_instances * (0.237 + 0.053)/60.0;
			case C4_2XLARGE:
				return num_instances * (0.476 + 0.105)/60.0;
			case C4_4XLARGE:
				return num_instances * (0.95 + 0.210)/60.0;
			case R4_XLARGE:
				return num_instances * (0.312 + 0.067)/60.0;
			case R4_2XLARGE:
				return num_instances * (0.624 + 0.133)/60.0;
			case R4_4XLARGE:
				return num_instances * (1.248 + 0.266)/60.0;
			case R4_8XLARGE:
				return num_instances * (2.496 + 0.270)/60.0;
			default:
				throw new RuntimeException("[ExtendedPlusGiraphConfigCostGenerator] Inexistent vm flavor " + config.getVm_flavor());
		}
	}


	@Override
	public double[] getIntermediateValues(ExtendedPlusGiraphConfig config, int amount, timeout timeoutType) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public double getAccForSpecificTime(double time, ExtendedPlusGiraphConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public double getAccForConfig(ExtendedPlusGiraphConfig config) {
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
			System.out.print("[ExtendedPlusGiraphConfigCostGenerator] ");
			 e.printStackTrace();
		}
		
		CSVParser parser = CSVParser.parse(csvData, CSVFormat.DEFAULT);
		for (CSVRecord csvRecord : parser) {
			if(counter >= 1){			    
			    String aux_graph	 = csvRecord.get(0);
			    String aux_wkld		 = csvRecord.get(1);
			    String flavor	 	 = csvRecord.get(2);
				int nr_workers		 = Integer.parseInt(csvRecord.get(3));
				double totalCores	 = Double.parseDouble(csvRecord.get(4));
				double totalMem		 = (double) Math.round(Double.parseDouble(csvRecord.get(5)) * 100) / 100D;
			    double time			 = Double.parseDouble(csvRecord.get(6)) / 60;	// = training time (mins)
			    
			    int graph = 0;
			    int wkld = 0;
			    switch(aux_graph){
			    	case "Orkut":
			    		graph = 0;
			    		break;
			    	default:
			           throw new RuntimeException("[ExtendedPlusGiraphConfigCostGenerator] Unknown graph type " + aux_graph);
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
				        throw new RuntimeException("[ExtendedPlusGiraphConfigCostGenerator] Unknown workload " + aux_wkld);
			    }
			    ExtendedPlusGiraphConfig config = new ExtendedPlusGiraphConfig(GiraphDirectory.vmFlavorFromString(flavor), nr_workers, totalMem, totalCores, GiraphDirectory.vmCostPerMinFromAttributes(GiraphDirectory.vmFlavorFromString(flavor), nr_workers), graph, wkld);
			    
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
		for (Map.Entry<ExtendedPlusGiraphConfig, Double> config : timeMap.entrySet()) {
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
