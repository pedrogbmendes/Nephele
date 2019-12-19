package lynceus.giraph;

import lynceus.Configuration;

public class GiraphConfig implements Configuration{
	
	/* class attributes */
	protected int vm_type; 		// 0 = c4 ; 1 = r4
	protected int vm_size;		// 0 = large ; 1 = xlarge ; 2 = 2xlarge ; 3 = 4xlarge ; 4 = 8xlarge
								// 0 = c4.large ; 1 = c4.xlarge ; 2 = c4.2xlarge ; 3 = c4.4xlarge ; 4 = r4.xlarge ; 5 = r4.2xlarge ; 6 = r4.4xlarge ; 7 = r4.8xlarge
	protected int nr_workers;
	protected int graph;		// 0 = orkut
	protected int wkld;			// 0 = SSSP ; 1 = Page Rank ; 2 = Connected Components 
	
	
	/* class constructors */
	public GiraphConfig(){
		
	}
	
	public GiraphConfig(int type, int size, int workers, int graph, int wkld){
		vm_type = type;
		vm_size = size;
		nr_workers = workers;
		this.graph = graph;
		this.wkld = wkld;
	}
	
	
	/* getters */
	public int getVm_type() {
		return vm_type;
	}

	public int getVm_size() {
		return vm_size;
	}

	public int getNr_workers() {
		return nr_workers;
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
		return 3;
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
	        default:
	           throw new RuntimeException("[GiraphConfig] Requested attribute " + i + " but only " + numAttributes() + " available");
		}
	}
	@Override
	public Configuration clone() {
		return new GiraphConfig(this.vm_type, this.vm_size, this.nr_workers, this.graph, this.wkld);
	}
	
	
	/* other methods */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + graph;
		result = prime * result + nr_workers;
		result = prime * result + vm_size;
		result = prime * result + vm_type;
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
		GiraphConfig other = (GiraphConfig) obj;
		if (graph != other.graph)
			return false;
		if (nr_workers != other.nr_workers)
			return false;
		if (vm_size != other.vm_size)
			return false;
		if (vm_type != other.vm_type)
			return false;
		if (wkld != other.wkld)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GiraphConfig [vm_type=" + vm_type + ", vm_size=" + vm_size + ", nr_workers=" + nr_workers + ", graph="
				+ graph + ", wkld=" + wkld + "]";
	}
	
}
