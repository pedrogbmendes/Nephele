package lynceus.giraph;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.State;


public class GiraphConfigCostGenerator implements CostGenerator<GiraphConfig>{

	/* class attributes */
	private final int timeOut = 600;		// secs
	private ArrayList<GiraphDatapoint> dataset;
	private double avgCost;
	private double maxTime;
	private double minTime;
	
	
	/* class constructor */
	public GiraphConfigCostGenerator(String dataset) throws IOException{
		this.dataset = parseCSV(dataset);
		initAvgCost();
		initTimes();
	}
	
	
	/* interface methods to be implemented */
	@Override
	public double setupCost(State state, GiraphConfig config) {
		// TODO Auto-generated method stub
		return 0.0D;
	}
	
	@Override
	public double deploymentCost(State state, GiraphConfig config) {
		/* search for the datapoint corresponding to the config */
		GiraphDatapoint datapoint = new GiraphDatapoint();
		datapoint = GiraphDatapoint.findDatapoint(config, dataset);
		
		/* if there is no such point, assume worst case scenario cost */
		if(datapoint == null){
			//System.out.println("[ConfigCostGenerator] Could not find " + config + " in the dataset --- assuming worst case scenario deployment cost");
			return (costPerConfigPerMinute(config) / 60.0) * timeOut; // worst case scenario, the configuration cannot reach the accuracy threshold before the timeout
		}
		/* otherwise return real observed deployment cost */
		return datapoint.getPrice();
	}
	@Override
	public double costPerConfigPerMinute(GiraphConfig config) {
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
				           throw new RuntimeException("[GiraphConfigCostGenerator] Inexistent vm flavor " + config.getVm_type() + "." + config.getVm_size());
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
				           throw new RuntimeException("[GiraphConfigCostGenerator] Inexistent vm flavor " + config.getVm_type() + "." + config.getVm_size());
				}
			default:
		           throw new RuntimeException("[GiraphConfigCostGenerator] Inexistent vm flavor " + config.getVm_type() + "." + config.getVm_size());
		}
	}
	
	@Override
	public double getAccForSpecificTime(double time, GiraphConfig config) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public double[] getIntermediateValues(GiraphConfig config, int amount, timeout timeoutType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getAccForConfig(GiraphConfig config) {
		// TODO Auto-generated method stub
		return -1;
	}
	
	
	/* getters */
	public double getAvgCost(){
		return avgCost;
	}
	
	public double getMinTime() {
	    return this.minTime;
	}
	
	public double getMaxTime() {
	    return this.maxTime;
	}
	
	
	/* other methods */
	/** method for parsing a csv file. Expects a file with the attributes in the order:
	 * _id/$oid,n_workers,timestamp,learning_rate,batch_size,synchronism,training_time,n_ps,acc,vm_flavor,total_iterations
	 * @input: name of the csv file
	 * @return: list of the datapoints contained in the csv file */
	public ArrayList<GiraphDatapoint> parseCSV(String file_path) throws IOException{
		FileReader csvData = null;
		ArrayList<GiraphDatapoint> datapoints = new ArrayList<GiraphDatapoint>();
		GiraphDatapoint aux;
		int counter = 0;
		
		try{
			csvData = new FileReader(new File(file_path));
		}catch(Exception e) {
			System.out.print("[GiraphConfigCostGenerator] ");
			 e.printStackTrace();
		}
		
		CSVParser parser = CSVParser.parse(csvData, CSVFormat.DEFAULT);
		for (CSVRecord csvRecord : parser) {
			if(counter >= 1){			    
			    String aux_graph	 = csvRecord.get(0);
			    String aux_wkld		 = csvRecord.get(1);
			    String flavor	 	 = csvRecord.get(2);
				int nr_workers		 = Integer.parseInt(csvRecord.get(3));
			    double performance	 = Double.parseDouble(csvRecord.get(6));	// = training time

			    int vm_type = 0;
			    int vm_size = 0;
			    int graph = 0;
			    int wkld = 0;
			    
			    switch(flavor){
			    	case "c4.large":
			    		vm_type = 0;
			    		vm_size = 0;
			    		break;
			    	case "c4.xlarge":
			    		vm_type = 0;
			    		vm_size = 1;
			    		break;
			    	case "c4.2xlarge":
			    		vm_type = 0;
			    		vm_size = 2;
			    		break;
			    	case "c4.4xlarge":
			    		vm_type = 0;
			    		vm_size = 3;
			    		break;
			    	case "r4.xlarge":
			    		vm_type = 1;
			    		vm_size = 1;
			    		break;
			    	case "r4.2xlarge":
			    		vm_type = 1;
			    		vm_size = 2;
			    		break;
			    	case "r4.4xlarge":
			    		vm_type = 1;
			    		vm_size = 3;
			    		break;
			    	case "r4.8xlarge":
			    		vm_type = 1;
			    		vm_size = 4;
			    		break;
			    	default:
				        throw new RuntimeException("[GiraphConfigCostGenerator] Unknown flavor " + flavor);
			    }
			    
			    switch(aux_graph){
			    	case "Orkut":
			    		graph = 0;
			    		break;
			    	default:
			           throw new RuntimeException("[GiraphConfigCostGenerator] Unknown graph type " + aux_graph);
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
				        throw new RuntimeException("[GiraphConfigCostGenerator] Unknown workload " + aux_wkld);
			    }
			    
			    aux = new GiraphDatapoint(vm_type, vm_size, nr_workers, graph, wkld, performance);
			    datapoints.add(aux);
			 }
			counter = counter + 1;
		}
		
		return datapoints;
	}
	
	
	private void initAvgCost(){
		
		for(GiraphDatapoint d : dataset){
			avgCost += d.getPrice();
		}
		avgCost /= dataset.size();
	}
	
	private void initTimes(){
		double max = 0;
		double min = Double.MAX_VALUE;
		
		for (GiraphDatapoint d: dataset){
			if(d.getPerformance() < 600){
				if (d.getPerformance() > max){
					max = d.getPerformance();
				} else if(d.getPerformance() < min){
					min = d.getPerformance();
				}
			}
		}
		
		maxTime = max/60.0;
		minTime = min/60.0;
	}

	/** 
	 * Method for finding the max_time that corresponds to only having a given percentage
	 * of the search space feasible
	 * @input feasible percentage of the search space
	 * @return max_time constraint (in min)
	 */
	public double getTimeForSpecificConstraint(double perc) {
		double[] times = new double[dataset.size()];
		double maxTime = Double.POSITIVE_INFINITY;

		int numberOfFeasibleConfigs = (int) Math.round(dataset.size()*perc);
		
		for (int i = 0; i < times.length; i++) {
			times[i] = dataset.get(i).getPerformance();
		}
		Arrays.sort(times);
				
		if (numberOfFeasibleConfigs == dataset.size()) {
			maxTime = times[numberOfFeasibleConfigs-1]/60; // in min
		} else {
			maxTime = times[numberOfFeasibleConfigs]/60; // in min
		}
		System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs + " ; maxTime = " + maxTime);
		
		return maxTime;
	}
	
}
