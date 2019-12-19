package lynceus.giraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lynceus.Configuration;
import lynceus.LHS;
import weka.giraph.WekaGiraphConfig;
import weka.giraph.WekaGiraphConfigFactory;

public class GiraphLHS<C extends Configuration> extends LHS{
	
	/* class attributes */
	private ArrayList<double[]> dims;
	
	/* class constructor */
	public GiraphLHS(int samples) {
		super(samples);
	}
	
	/* superclass abstract methods to be implemented */
	
	/**
	 * Initialise the dimension arrays
	 * @return a list with the initialised arrays
	 */
	@Override
	protected ArrayList initDims() {
		this.dims = new ArrayList<double[]>();
		
		double[] vm_type = {0, 1};
		double[] vm_size = {0, 1, 2, 3, 4};
		double[] nr_cores = {4, 8, 16, 32, 48, 64, 80, 96, 112, 128};
//		double[] graph = {0};
//		double[] wkld = {0, 1, 2};
		
		this.dims.add(nr_cores);
		this.dims.add(vm_size);
		this.dims.add(vm_type);
		
		return this.dims;
	}

	@Override
	protected Configuration newSample(int index) {
		
		double[] dim = new double[10];
		int auxIndex = -1;	// auxiliar to maintain the initial value of index when 
							// we need to do round-robin
		int vm_type = -1;
		int vm_size = 0;
		int workers = 0;
		int nr_cores = 0;
		int graph = 0;
		
		/* GIRAPH WKLDS
		 * PR: 
		 * 	   - C4.large 	==> nr_workers = {8, 16, 24, 32, 40, 48, 56, 64}	// 2  vCPUs
		 *     - C4.xlarge 	==> nr_workres = {4, 8, 12, 16, 20, 24, 28, 32}		// 4  vCPUs
		 *     - C4.2xlarge	==> nr_workers = {2, 4, 6, 8, 10, 12, 14, 16}		// 8  vCPUs
		 *     - C4.4xlarge ==> nr_workers = {1, 2, 3, 4, 5, 6, 7, 8}			// 16 vCPUs
		 *     - R4.xlarge 	==> nr_workers = {1, 2, 4, 8}						// 4  vCPUs
		 *     - R4.2xlarge	==> nr_workers = {1, 2, 4}							// 8  vCPUs
		 *     - R4.4xlarge ==> nr_workers = {1, 2}								// 16 vCPUs
		 *     - R4.8xlarge ==> nr_workers = {1}								// 32 vCPUs
		 * CC:
		 * 	   - C4.large 	==> nr_workers = {8, 16, 24, 32, 40, 48, 56, 64}
		 *     - C4.xlarge 	==> nr_workres = {4, 8, 12, 16, 20, 24, 28, 32}
		 *     - C4.2xlarge	==> nr_workers = {2, 4, 6, 8, 10, 12, 14, 16}
		 *     - C4.4xlarge ==> nr_workers = {1, 2, 3, 4, 5, 6, 7, 8}
		 *     - R4.xlarge 	==> nr_workers = {1, 2, 4, 8}
		 *     - R4.2xlarge	==> nr_workers = {1, 2, 4}
		 *     - R4.4xlarge ==> nr_workers = {1, 2}
		 *     - R4.8xlarge ==> nr_workers = {1}
		 * SSSP:
		 * 	   - C4.large 	==> nr_workers = {8, 16, 24, 32, 40, 48, 56, 64}
		 *     - C4.xlarge 	==> nr_workres = {4, 8, 12, 16, 20, 24, 28, 32}
		 *     - C4.2xlarge	==> nr_workers = {2, 4, 6, 8, 10, 12, 14, 16}
		 *     - C4.4xlarge ==> nr_workers = {1, 2, 3, 4, 5, 6, 7, 8}
		 *     - R4.xlarge 	==> nr_workers = {1, 2, 4, 8}
		 *     - R4.2xlarge	==> nr_workers = {1, 2, 4}
		 *     - R4.4xlarge ==> nr_workers = {1, 2}
		 *     - R4.8xlarge ==> nr_workers = {1}
		 */
		
		Random r = new Random();
		int low = 0;
		int high;
		
		/* for each dimension array of a configuration, pick the value
		 * corresponding to the given index. If the array is smaller, do it
		 * round-robin */
		int i = 0;
		while (i < this.dims.size()) {
			if (this.dims.get(i).length == 1) {
				auxIndex = 0;
			} else if (index >= this.dims.get(i).length) {
				auxIndex = index - this.dims.get(i).length;
				while (auxIndex >= this.dims.get(i).length) {
					auxIndex = auxIndex - this.dims.get(i).length;
				}
			} else {
				auxIndex = index;
			}
			
			dim = this.dims.get(i);
//			System.out.println(i);
			
			if (i == 0) {	// selecting nr_cores
				nr_cores = (int) dim[auxIndex];
			} else if (i == 1){ // selecting vm_size
				vm_size = (int) dim[auxIndex];
				
				if (nr_cores > 32) { // nr_cores > 32 can only be C4
					vm_type = 0;
					if (vm_size == 4) {	
						high = dim.length;
						while (vm_size == 4) { // take new vm_size randomly
							vm_size = (int) dim[(int) r.nextInt(high-low) + low];
						}
					}
				} else if (nr_cores < 16) { // nr_cores < 16 can only be R4
					vm_type = 1;
					if (nr_cores == 4) {
						vm_size = 1;
					} else if (nr_cores == 8) {
						if(Math.random() < 0.5) {
							vm_size = 1;
						} else {
							vm_size = 2;
						}
					}
				} else if (nr_cores == 16 && vm_size == 4) { // this combination doesn't exist
					high = dim.length;
					while (vm_size == 4) { // take new vm_size randomly
						vm_size = (int) dim[(int) r.nextInt(high-low) + low];
					}
				} else if (nr_cores == 32 && vm_size == 4) { // must be R4
					vm_size = 1;
				} else {
					i++;
					continue;
				}
			} else { 	// selecting vm_type
				if (vm_size == 0) {
					vm_type = 0;
				} else if (vm_size == 4) {
					vm_type = 1;
				} else if (vm_type == -1) {
					vm_type = (int) dim[auxIndex];
				} else {
					i++;
					continue;
				}
			}
			//System.out.println("dim: " + Arrays.toString(dim) + " ; auxIndex = " + auxIndex + " ; lenght = " + this.dims.get(i).length + " ; i = " + i);
			i++;
		}
		
//		System.out.println("cores=" + nr_cores + " ; size=" + vm_size + " ; type=" + vm_type);
		
		switch(vm_size) {
			case 0:	// large
				workers = nr_cores / 2;
				break;
			case 1:	// xlarge
				workers = nr_cores / 4;
				break;
			case 2: // 2xlarge
				workers = nr_cores / 8;
				break;
			case 3: // 4xlarge
				workers = nr_cores / 16;
				break;
			case 4: // 8xlarge
				workers = nr_cores / 32;
				break;
		}
		
//		System.out.println(nr_cores + " cores ==> " + workers + " workers");
		
		GiraphConfig newConfig = new GiraphConfig(vm_type, vm_size, workers, graph, WekaGiraphConfigFactory.wkld);
		
		return new WekaGiraphConfig(newConfig, super.dataset);
	}


	@Override
	protected void checkSamples(List samples, String type, int seed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ArrayList<Double> dimensions(Configuration config) {
		ArrayList<Double> dimensionsArray = new ArrayList<Double>();
		int nr_workers;
		int nr_cores = 0;
		
		if ((int) config.at(0) == 0) {	// type == C4
			dimensionsArray.add(1.0);
		} else {
			dimensionsArray.add(2.0);	// type == R4
		}
		
		nr_workers = (int) config.at(2);
		
		switch((int) config.at(1)) {
			case 0:	// size == large
				dimensionsArray.add(1.0);
				nr_cores = nr_workers * 2;
				break;
			case 1: // size == xlarge
				dimensionsArray.add(2.0);
				nr_cores = nr_workers * 4;
				break;
			case 2: // size == 2xlarge
				dimensionsArray.add(3.0);
				nr_cores = nr_workers * 8;
				break;
			case 3: // size == 4xlarge
				dimensionsArray.add(4.0);
				nr_cores = nr_workers * 16;
				break;
			case 4: // size == 8xlarge
				dimensionsArray.add(5.0);
				nr_cores = nr_workers * 32;
				break;
		}
		
		// {4, 8, 16, 32, 48, 64, 80, 96, 112, 128};
		switch(nr_cores) {
			case 4:
				dimensionsArray.add(1.0);
				break;
			case 8:
				dimensionsArray.add(2.0);
				break;
			case 16:
				dimensionsArray.add(3.0);
				break;
			case 32:
				dimensionsArray.add(4.0);
				break;
			case 48:
				dimensionsArray.add(5.0);
				break;
			case 64:
				dimensionsArray.add(6.0);
				break;
			case 80:
				dimensionsArray.add(7.0);
				break;
			case 96:
				dimensionsArray.add(8.0);
				break;
			case 112:
				dimensionsArray.add(9.0);
				break;
			case 128:
				dimensionsArray.add(10.0);
				break;
		}
	
		return dimensionsArray;
	}

}
