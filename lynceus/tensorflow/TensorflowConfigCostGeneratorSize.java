package lynceus.tensorflow;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lynceus.CostGenerator;
import lynceus.Main.timeout;
import lynceus.State;

public class TensorflowConfigCostGeneratorSize implements CostGenerator<TensorflowConfigSize>{

	private final int timeOut = 600;		// secs
	private final double accConstrain = 0.85;
	
	private double avgCost;
	private double maxTime;		// mins
	private double minTime;		// mins	
	private static double accThreshold;
	
	private Map<TensorflowConfigSize, Double> time = null;	//seconds
	private Map<TensorflowConfigSize, Double> accuracy = null;
	private Map<TensorflowConfigSize, Double> price = null;
	private Map<TensorflowConfigSize, ArrayList<Double> > intermediateValues = null;
	
	
	
	
	/* class constructor */
	public TensorflowConfigCostGeneratorSize(String dataset) throws IOException{
		time = new HashMap<>();
		accuracy = new HashMap<>();
		price = new HashMap<>();
		intermediateValues = new HashMap<>();
		
		parseCSV(dataset);
		initAvgCost();
		initTimes();
	}
	
	
	public static void setAccThreshold(double accThreshold) {
		TensorflowConfigCostGeneratorSize.accThreshold = accThreshold;
	}



	/* implement interface methods */
	@Override
	public double setupCost(State state, TensorflowConfigSize tensorflow) {
		// TODO Auto-generated method stub
		return 0.0D;
	}

	@Override
	/** Return the deployment cost of a config. The deployment cost is the
	 * 	expected cost to be paid to get the minimum desired accuracy
	 * 	@input: State state: current state of the algorithm
	 * 			TensorflowConfig tensorflow: config whose cost we want
	 * 	@return: double deploymentCost
	 */
	public double deploymentCost(State state, TensorflowConfigSize tensorflowConfig) {
		
		final Double cost = price.get(tensorflowConfig);
		

		/* if there is no such point, assume worst case scenario cost */
		if(cost == null){
			//System.out.println("[ConfigCostGenerator] Could not find " + config + " in the dataset --- assuming worst case scenario deployment cost");
			//return (costPerConfigPerMinute(tensorflowConfig) / 60.0) * timeOut; // worst case scenario, the configuration cannot reach the accuracy threshold before the timeout
			throw new RuntimeException(tensorflowConfig + " with hashcode " + tensorflowConfig.hashCode() + " not in cost map for wkld " + tensorflowConfig.toString() + " map size " + price.size());
			
		}
		/* otherwise return real observed deployment cost */
		return cost;
	}

	
	@Override
	/* cost per hour of a particular vm_flavor */
	public double costPerConfigPerMinute(TensorflowConfigSize tensorflow) {
		int num_instances = tensorflow.getNr_ps() + tensorflow.getNr_workers();
		
		/* costPerMin = costPerHour/60 */
		switch (tensorflow.getVm_flavor()) {
	        case 0: 	// "t2.small":
	           return num_instances * 0.023/60.0 + (0.3712/60.0);
	        case 1: 	// "t2.medium":
	           return num_instances * 0.0464/60.0 + (0.3712/60.0);
	        case 2: 	// "t2.xlarge":
	           return num_instances * 0.1856/60.0 + (0.3712/60.0);
	        case 3: 	// "t2.2xlarge":
	           return num_instances * 0.3712/60.0 + (0.3712/60.0);
	        default:
	           throw new RuntimeException("[ConfigCostGeneratorSize] Inexistent vm flavor " + tensorflow.getVm_flavor());
		}
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
	
	public Map<TensorflowConfigSize, Double> getTimeConfig() {
		return time;
	}
	
	public Map<TensorflowConfigSize, Double> getAccuracyConfig() {
		return accuracy;
	}
	
	public Map<TensorflowConfigSize, Double> getCostConfig() {
		return price;
	}
	
	public Map<TensorflowConfigSize, ArrayList<Double>> getIntermediateValuesConfig() {
		return intermediateValues;
	}
	
	



	/* other methods */
	/** method for parsing a csv file. Expects a file with the attributes in the order:
	 * _id/$oid,n_workers,timestamp,learning_rate,batch_size,synchronism,training_time,n_ps,acc,vm_flavor,total_iterations
	 * @input name of the csv file
	 * @return list of the datapoints contained in the csv file */
	public void parseCSV(String file_path) throws IOException{
		FileReader csvData = null;
		//ArrayList<TensorflowDatapoint> datapoints = new ArrayList<TensorflowDatapoint>();
		//TensorflowDatapoint aux;
		int counter = 0;
		try{
			csvData = new FileReader(new File(file_path));
		}catch(Exception e) {
			System.out.print("[TensorflowConfig Cost Generator] ");
			 e.printStackTrace();
		}
		
		CSVParser parser = CSVParser.parse(csvData, CSVFormat.RFC4180);
		for (CSVRecord csvRecord : parser) {
			if(counter >= 1){
				int nr_workers		 = Integer.parseInt(csvRecord.get(1));
			    double learning_rate = Double.parseDouble(csvRecord.get(3));
			    int batch_size		 = Integer.parseInt(csvRecord.get(4));
			    int synchronism	 	 = (csvRecord.get(6).equals("async") ? 0:1);
			    double performance	 = Double.parseDouble(csvRecord.get(7));	// = training time in seconds
			    int nr_ps			 = Integer.parseInt(csvRecord.get(10));
			    double acc  		 = Double.parseDouble(csvRecord.get(11));
			    String flavor	 	 = csvRecord.get(13);
			    double real_size 	 = (Double.parseDouble(csvRecord.get(15)));
			    double size 		 = real_size/60000.0;
			    int nr_iterations 	 = Integer.parseInt(csvRecord.get(17));
			    
			    ArrayList<Double> intermediateValues = new ArrayList<Double>();
			    
			    
			    ArrayList<String> intermediateValuesStr = new ArrayList<String>(Arrays.asList(csvRecord.get(9).split(" ")));
			    
				int i = 0;
				while(i < intermediateValuesStr.size()){
					intermediateValues.add(Double.parseDouble(intermediateValuesStr.get(i)));
				  	i ++;
				}
			    
			    
			    int vm_flavor = 0;
			    
			    switch(flavor){
			    	case "t2.small":
			    		vm_flavor = 0;
			    		break;
			    	case "t2.medium":
			    		vm_flavor = 1;
			    		break;
			    	case "t2.xlarge":
			    		vm_flavor = 2;
			    		break;
			    	case "t2.2xlarge":
			    		vm_flavor = 3;
			    		break;
			    	default:
				        throw new RuntimeException("[TensorflowConfigCostGenerator] Unknown flavor " + flavor);
			    }
			    
			    TensorflowConfigSize config = new TensorflowConfigSize(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor, size);
				
			    if(performance > timeOut) {
					//time.put(config, (performance * 0.85) / accuracy);	//seconds
			    	int total_nr_iteration = (int) Math.ceil((Math.log(1.0-0.9))/(1.0*batch_size*Math.log((real_size-1.0)/real_size)));
			    	time.put(config, (total_nr_iteration * performance) / nr_iterations);
			   	}else {
					time.put(config, performance);
				}
				this.accuracy.put(config, acc);
				this.price.put(config, costPerConfigPerMinute(config)*time.get(config)/60.0);
				this.intermediateValues.put(config, intermediateValues);
			}
			counter += 1;
		}
				
	}
	
	
	private void initAvgCost(){
		
		int counter = 0;
		for(Map.Entry<TensorflowConfigSize, Double>  d : this.accuracy.entrySet()){
			
			if(d.getValue() >= accThreshold){
				avgCost += this.price.get(d.getKey());
				counter ++;
			}
		}
		
		avgCost /= counter;
	}
	
	
	private void initTimes(){
		double max = 0;
		double min = Double.MAX_VALUE;
		
		for (Double  d : this.time.values()){
			if (d > max){
				max = d;
			} else if(d < min){
				min = d;
			}
		}
		
		maxTime = max/60.0;	// in min
		minTime = min/60.0;	// in min
	}


	/**
	 * @param config Configuration
	 * @return value of accuracy if the configuration is found, -1 if the configuration
	 * does not exist
	 */
	@Override
	public double getAccForConfig(TensorflowConfigSize config) {
		if(accuracy.get(config) == null)
			return 0.0;
		return accuracy.get(config);
	}
		
	
	@Override
	public double getAccForSpecificTime(double time, TensorflowConfigSize config) {
		if (this.accuracy == null || this.intermediateValues == null){
			return 0.0;
		} else {
			
			int position = -1;
				
			position = (int) Math.round((time*60) / 30.0) - 1;
			if (position >= this.intermediateValues.size()){
				position = this.intermediateValues.size() - 1;
			}
			
			if (position < 0){
				return 0.0;
			} else {
				return this.intermediateValues.get(config).get(position);
			}
		}
	}
	
	
	/** 
	 * Method for finding the max_time that corresponds to only having a given percentage
	 * of the search space feasible
	 * @input feasible percentage of the search space
	 * @return max_time constraint (in min)
	 */
	public double getTimeForSpecificConstraint(double perc) {
		double maxiTime = 300.0;	//seconds
		int numberOfFeasibleConfigs = 0;
		
		for(TensorflowConfigSize ci : this.accuracy.keySet()) {
			if (this.accuracy.get(ci) >= accConstrain && this.time.get(ci) <= maxiTime)
				numberOfFeasibleConfigs++;
		}
			
		System.out.println("NbFeasibleConfigs = " + numberOfFeasibleConfigs + " ; maxTime = " + maxiTime/60.0);
		
		return maxiTime/60.0; 	//in minutes
	}


	@Override
	public double[] getIntermediateValues(TensorflowConfigSize config, int amount, timeout timeoutType) {
		double[]	intermediateValues = null;	// time
		ArrayList<Double>	values = new ArrayList<Double> ();
		int i = 0;
		int valueIndex = 0;
		
		values = this.intermediateValues.get(config);
		
		if (timeoutType == timeout.LOG) {
			intermediateValues = new double[amount];
			i = valueIndex = 0;
		} else {
			intermediateValues = new double[amount + 1];
			intermediateValues[0] = 0;
			valueIndex = 0;
			i = 1;
		}
//		System.out.println("amount = " + amount + " ; intermediateValues.length = " + intermediateValues.length);
		while (i < intermediateValues.length) {
			intermediateValues[i] = values.get(valueIndex);
//			System.out.println("intermediateValues[i] = " + values.get(valueIndex) + " ; i = " + i);
			valueIndex++;
			i ++;
		}
		
		return intermediateValues;
	}
	
	
}
