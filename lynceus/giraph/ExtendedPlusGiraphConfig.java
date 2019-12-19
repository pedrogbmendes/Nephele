package lynceus.giraph;

import lynceus.Configuration;


public class ExtendedPlusGiraphConfig implements Configuration {

	/* class attributes */
	protected GiraphDirectory.GiraphVMFlavor vm_flavor;
	protected int nr_workers;
	protected double totalMem;
	protected double totalCores;
	protected double configCostPerTimeUnit;
	protected int graph;		// 0 = orkut
	protected int wkld;			// 0 = SSSP ; 1 = Page Rank ; 2 = Connected Components 


	/* class constructors */
	public ExtendedPlusGiraphConfig(){
		
	}
	
	public ExtendedPlusGiraphConfig(GiraphDirectory.GiraphVMFlavor flavor, int workers, double mem, double cores, double configCostPerTimeUnit, int graph, int wkld){
		vm_flavor = flavor;
		nr_workers = workers;
		totalMem = mem;
		totalCores = cores;
		this.configCostPerTimeUnit = configCostPerTimeUnit;
		this.graph = graph;
		this.wkld = wkld;
	}

	/* getters */
	public GiraphDirectory.GiraphVMFlavor getVm_flavor() {
		return vm_flavor;
	}

	public int getNr_workers() {
		return nr_workers;
	}

	public double getTotalMem() {
		return totalMem;
	}

	public double getTotalCores() {
		return totalCores;
	}

	public double getConfigCostPerTimeUnit() {
		return configCostPerTimeUnit;
	}

	public int getGraph() {
		return graph;
	}

	public int getWkld() {
		return wkld;
	}
	
	/* Interface methods to be implemented */
	@Override
	public int numAttributes() {
		return 5;
	}
	
	@Override
	public Object at(int i) {
		switch (i) {
			case 0:
				return vm_flavor;
     		case 1:
     			return nr_workers;
	        case 2:
	           return totalMem;
	        case 3:
	        	return totalCores;
	        case 4:
	        	return configCostPerTimeUnit;
	        default:
	           throw new RuntimeException("[ExtendedPlusGiraphConfig] Requested attribute " + i + " but only " + numAttributes() + " available");
		}
	}
	
	@Override
	public Configuration clone() {
		return new ExtendedPlusGiraphConfig(this.vm_flavor, this.nr_workers, this.totalMem, this.totalCores, this.configCostPerTimeUnit, this.graph, this.wkld);
	}

	/* other methods */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(configCostPerTimeUnit);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + graph;
		result = prime * result + nr_workers;
		temp = Double.doubleToLongBits(totalCores);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(totalMem);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((vm_flavor == null) ? 0 : vm_flavor.hashCode());
		result = prime * result + wkld;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
//		if (getClass() != obj.getClass())
//			return false;
		ExtendedPlusGiraphConfig other = (ExtendedPlusGiraphConfig) obj;
		if (Double.doubleToLongBits(configCostPerTimeUnit) != Double.doubleToLongBits(other.configCostPerTimeUnit))
			return false;
		if (graph != other.graph)
			return false;
		if (nr_workers != other.nr_workers)
			return false;
		if (Double.doubleToLongBits(totalCores) != Double.doubleToLongBits(other.totalCores))
			return false;
		if (Double.doubleToLongBits(totalMem) != Double.doubleToLongBits(other.totalMem))
			return false;
		if (vm_flavor != other.vm_flavor)
			return false;
		if (wkld != other.wkld)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ExtendedPlusGiraphConfig [vm_flavor=" + vm_flavor + ", nr_workers=" + nr_workers + ", totalMem="
				+ totalMem + ", totalCores=" + totalCores + ", configCostPerTimeUnit=" + configCostPerTimeUnit
				+ ", graph=" + graph + ", wkld=" + wkld + "]";
	}
	
	
}
