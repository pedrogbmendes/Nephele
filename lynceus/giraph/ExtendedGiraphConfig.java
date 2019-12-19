package lynceus.giraph;

import lynceus.Configuration;

public class ExtendedGiraphConfig implements Configuration {

	/* class attributes */
	protected GiraphDirectory.GiraphVMFlavor vm_flavor;
	protected int nr_workers;
	protected double totalMem;
	protected double totalCores;
	protected int graph;		// 0 = orkut
	protected int wkld;			// 0 = SSSP ; 1 = Page Rank ; 2 = Connected Components 
	
	/* class constructors */
	public ExtendedGiraphConfig(){
		
	}
	
	public ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor flavor, int workers, double mem, double cores, int graph, int wkld){
		vm_flavor = flavor;
		nr_workers = workers;
		totalMem = mem;
		totalCores = cores;
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

	public int getGraph() {
		return graph;
	}

	public int getWkld() {
		return wkld;
	}

	
	/* Interface methods to be implemented */
	@Override
	public int numAttributes() {
		return 4;
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
	        default:
	           throw new RuntimeException("[ExtendedGiraphConfig] Requested attribute " + i + " but only " + numAttributes() + " available");
		}
	}
	@Override
	public Configuration clone() {
		return new ExtendedGiraphConfig(this.vm_flavor, this.nr_workers, this.totalMem, this.totalCores, this.graph, this.wkld);
	}

	/* other methods */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + graph;
		result = prime * result + nr_workers;
		long temp;
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
		ExtendedGiraphConfig other = (ExtendedGiraphConfig) obj;
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
		return "ExtendedGiraphConfig [vm_flavor=" + vm_flavor + ", nr_workers=" + nr_workers + ", totalMem=" + totalMem
				+ ", totalCores=" + totalCores + ", graph=" + graph + ", wkld=" + wkld + "]";
	}
		
}
