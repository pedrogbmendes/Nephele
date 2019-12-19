package lynceus.giraph;

import java.util.ArrayList;

import lynceus.Configuration;

public class GiraphDatapoint extends GiraphConfig implements Configuration{

	/* class attributes */
	private double 	performance;	// in seconds
	private double 	price;
		
	/* class constructors */
	protected GiraphDatapoint(){
		
	}
	
	public GiraphDatapoint(int vm_type, int vm_size, int nr_workers, int graph, int wkld, double performance){
		super(vm_type, vm_size, nr_workers, graph, wkld);
		this.performance = performance;
		this.price = this.performance * vm_price_per_sec(vm_type, vm_size) * (1 + nr_workers);
	}
	
	public GiraphDatapoint(GiraphConfig c, double performance){
		super(c.getVm_type(), c.getVm_size(), c.getNr_workers(), c.getGraph(), c.getWkld());
		this.performance = performance;
		this.price = this.performance * vm_price_per_sec(vm_type, vm_size) * (1 + nr_workers);
	}
	
	/* Interface methods to be implemented */
	@Override
	public int numAttributes() {
		return 6;
	}
	
	@Override
	public Object at(int i) {
		switch (i) {
			case 0:
				return vm_type;
	 		case 1:
	 			return vm_size;
	        case 2:
	           return nr_workers;
	        case 3:
	           return graph;
	        case 4:
	           return wkld;
	        case 5:
	        	return performance;
	        case 6:
	        	return price;
	        default:
	           throw new RuntimeException("[GiraphConfig] Requested attribute " + i + " but only " + numAttributes() + " available");
		}
	}
	@Override
	public Configuration clone() {
		return new GiraphDatapoint(this.vm_type, this.vm_size, this.nr_workers, this.graph, this.wkld, this.performance);
	}
	
	
	/* getters */
	public double getPerformance() {
		return performance;
	}

	public double getPrice() {
		return price;
	}
	
	
	/* other methods */
	
	/** method to get the price per second for a specific instance flavor
	 ** price_per_hour/seconds_in_an_hour for London availability zone
	 ** @input: vm flavor whose price we want to know **/
	private double vm_price_per_sec(int vm_type, int vm_size){
		switch (vm_type) {
			case 0:
				switch(vm_size) {
					case 0:		// c4.large
			           return (0.119 + 0.027)/3600.0;
			        case 1: 	// c4.xlarge
			           return (0.237 + 0.053)/3600.0;
			        case 2: 	// c4.2xlarge
			           return (0.476 + 0.105)/3600.0;
			        case 3: 	// c4.4xlarge
			           return (0.95 + 0.210)/3600.0;
			        default:
				       throw new RuntimeException("[GiraphDatapoint] Inexistent vm flavor " + vm_type + "." + vm_size);
				}
			case 1:
				switch(vm_size) {
				case 1:		// r4.xlarge
		        	return (0.312 + 0.067)/3600.0;
		        case 2: 	// r4.2xlarge
		        	return (0.624 + 0.133)/3600.0;
		        case 3: 	// r4.4xlarge
		        	return (1.248 + 0.266)/3600.0;
		        case 4: 	// r4.8xlarge
		        	return (2.496 + 0.270)/3600.0;
		        default:
			        throw new RuntimeException("[GiraphDatapoint] Inexistent vm flavor " + vm_type + "." + vm_size);
				}
	        default:
	           throw new RuntimeException("[GiraphDatapoint] Inexistent vm flavor " + vm_type + "." + vm_size);
        }
	}
	
	
	/** return a config of a datapoint *
	 * @input: the datapoint
	 * @return: a config extracted from the datapoint **/
	public GiraphConfig toGiraphConfig(){
		return new GiraphConfig(this.getVm_type(), this.getVm_size(), this.getNr_workers(), this.getGraph(), this.getWkld());
	}
	
	/** search for the datapoint with the configuration config and return it 
	 * @input: the configuration which we want to find, the dataset of all datapoints
	 * @return: the datapoint with the configuration **/
	protected static GiraphDatapoint findDatapoint(GiraphConfig config, ArrayList<GiraphDatapoint> dataset){
		GiraphConfig c;
		for(GiraphDatapoint d : dataset){
			c = d.toGiraphConfig();
			if(c.equals(config)){
				return d;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return "GiraphDatapoint [performance=" + performance + ", price=" + price + ", vm_type=" + vm_type
				+ ", vm_size=" + vm_size + ", nr_workers=" + nr_workers + ", graph=" + graph + ", wkld=" + wkld + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		long temp;
		temp = Double.doubleToLongBits(performance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(price);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		//if (getClass() != obj.getClass())
		//	return false;
		GiraphDatapoint other = (GiraphDatapoint) obj;
		if (Double.doubleToLongBits(performance) != Double.doubleToLongBits(other.performance))
			return false;
		if (Double.doubleToLongBits(price) != Double.doubleToLongBits(other.price))
			return false;
		return true;
	}
	
}
