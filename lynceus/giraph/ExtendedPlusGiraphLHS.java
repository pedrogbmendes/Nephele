package lynceus.giraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lynceus.Configuration;
import lynceus.LHS;
import weka.extendedPlusGiraph.ExtendedPlusWekaGiraphConfig;
import weka.extendedPlusGiraph.ExtendedPlusWekaGiraphConfigFactory;

public class ExtendedPlusGiraphLHS<C extends Configuration> extends LHS {

	/* class attributes */
	private ArrayList<double[]> dims;
	
	public ExtendedPlusGiraphLHS(int samples) {
		super(samples);
	}

	/* superclass abstract methods to be implemented */
	@Override
	protected ArrayList<double[]> initDims() {
		this.dims = new ArrayList<double[]>();
		
		double[] vm_flavor = {0, 1, 2, 3, 4, 5, 6, 7}; // c4.large = 0 ; c4.xlarge = 1 ; c4.2xlarge = 2 ; c4.4xlarge = 3
													  // r4.xlarge = 4 ; r4.2xlarge = 5 ; r4.4xlarge = 6 ; r4.8xlarge = 7
		double[] totalCores = {4, 8, 16, 32, 48, 64, 80, 96, 112, 128};
		
		this.dims.add(totalCores);		
		this.dims.add(vm_flavor);
		
		return this.dims;
	}
	
	protected Configuration newSample(int index) {
		
		double[] dim = new double[10];
		int auxIndex = -1;	// auxiliar to maintain the initial value of index when 
							// we need to do round-robin
		int vm_flavor = -1;
		int workers = 0;
		double totalCores = 0;
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
				totalCores = (int) dim[auxIndex];
			} else { // selecting vm_flavor
				vm_flavor = (int) dim[auxIndex];
				
				if (totalCores > 32) { // nr_cores > 32 can only be C4
					if (vm_flavor == 4 || vm_flavor == 5 || vm_flavor == 6 || vm_flavor == 7) {
						high = dim.length;
						while (vm_flavor == 4 || vm_flavor == 5 || vm_flavor == 6 || vm_flavor == 7) { // take new vm_flavor randomly
							vm_flavor = (int) dim[(int) r.nextInt(high-low) + low];
						}	
					}
				} else if (totalCores < 16) { // nr_cores < 16 can only be R4
					if (totalCores == 4) {
						vm_flavor = 4;	// r4.xlarge
					} else if (totalCores == 8) {
						if(Math.random() < 0.5) {
							vm_flavor = 4;	// r4.xlarge
						} else {
							vm_flavor = 5;	// r4.2xlarge
						}
					}
				} else if (totalCores == 16 && vm_flavor == 7) { // this combination doesn't exist
					high = dim.length;
					while (vm_flavor == 7) { // take new vm_size randomly
						vm_flavor = (int) dim[(int) r.nextInt(high-low) + low];
					}
				} else {
					i ++;
					continue;
				}
			}
			//System.out.println("dim: " + Arrays.toString(dim) + " ; auxIndex = " + auxIndex + " ; lenght = " + this.dims.get(i).length + " ; i = " + i);
			i++;
		}
		
		GiraphDirectory.GiraphVMFlavor flavor;
		
		switch(vm_flavor) {
			case 0:
				flavor = GiraphDirectory.GiraphVMFlavor.C4_LARGE;
				workers = (int) totalCores / 2; 
				break;
			case 1:
				flavor = GiraphDirectory.GiraphVMFlavor.C4_XLARGE;
				workers = (int) totalCores / 4; 
				break;
			case 2:
				flavor = GiraphDirectory.GiraphVMFlavor.C4_2XLARGE;
				workers = (int) totalCores / 8; 
				break;
			case 3:
				flavor = GiraphDirectory.GiraphVMFlavor.C4_4XLARGE;
				workers = (int) totalCores / 16; 
				break;
			case 4:
				flavor = GiraphDirectory.GiraphVMFlavor.R4_XLARGE;
				workers = (int) totalCores / 4; 
				break;
			case 5:
				flavor = GiraphDirectory.GiraphVMFlavor.R4_2XLARGE;
				workers = (int) totalCores / 8; 
				break;
			case 6:
				flavor = GiraphDirectory.GiraphVMFlavor.R4_4XLARGE;
				workers = (int) totalCores / 16; 
				break;
			case 7:
				flavor = GiraphDirectory.GiraphVMFlavor.R4_8XLARGE;
				workers = (int) totalCores / 32; 
				break;
			default:
				throw new RuntimeException("[ExtendedGiraphLHS] Unknown flavor " + vm_flavor);	
		}
		
		double mem = GiraphDirectory.memFromAttributes(flavor, workers);
		
		ExtendedPlusGiraphConfig config = new ExtendedPlusGiraphConfig(flavor, workers, mem, totalCores, GiraphDirectory.vmCostPerMinFromAttributes(flavor, workers), graph, ExtendedPlusWekaGiraphConfigFactory.wkld);
		
		return new ExtendedPlusWekaGiraphConfig(config, super.dataset);
	}

	@Override
	protected ArrayList<Double> dimensions(Configuration config) {
		ArrayList<Double> dimensionsArray = new ArrayList<Double>();
		
		GiraphDirectory.GiraphVMFlavor flavor = (GiraphDirectory.GiraphVMFlavor) config.at(0);
		int nr_workers = (int) config.at(1);
		double mem = (double) config.at(2);
		double memDim;
		double totalCores = (double) config.at(3);
		double configCostPerTimeUnit = (double) config.at(4);
		double costDim;
		
		if (flavor == GiraphDirectory.GiraphVMFlavor.C4_LARGE) {
			dimensionsArray.add(1.0);
			if (mem == GiraphDirectory.memFromAttributes(flavor, 8)) {
				memDim = 1.0;
				costDim = 1.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 16)) {
				memDim = 2.0;
				costDim = 2.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 24)) {
				memDim = 3.0;
				costDim = 3.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 32)) {
				memDim = 4.0;
				costDim = 4.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 40)) {
				memDim = 5.0;
				costDim = 5.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 48)) {
				memDim = 6.0;
				costDim = 6.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 56)) {
				memDim = 7.0;
				costDim = 7.0;
			} else {//(mem == ExtendedGiraphConfig.memFromAttributes(flavor, 64)) {
				memDim = 8.0;
				costDim = 8.0;
			}
		} else if (flavor == GiraphDirectory.GiraphVMFlavor.C4_XLARGE) {
			dimensionsArray.add(2.0);
			if (mem == GiraphDirectory.memFromAttributes(flavor, 4)) {
				memDim = 9.0;
				costDim = 1.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 8)) {
				memDim = 10.0;
				costDim = 2.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 12)) {
				memDim = 11.0;
				costDim = 3.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 16)) {
				memDim = 12.0;
				costDim = 4.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 20)) {
				memDim = 13.0;
				costDim = 5.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 24)) {
				memDim = 14.0;
				costDim = 6.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 28)) {
				memDim = 15.0;
				costDim = 7.0;
			} else {//(mem == ExtendedGiraphConfig.memFromAttributes(flavor, 32)) {
				memDim = 16.0;
				costDim = 8.0;
			}
		} else if (flavor == GiraphDirectory.GiraphVMFlavor.C4_2XLARGE) {
			dimensionsArray.add(3.0);
			if (mem == GiraphDirectory.memFromAttributes(flavor, 2)) {
				memDim = 17.0;
				costDim = 1.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 4)) {
				memDim = 18.0;
				costDim = 2.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 6)) {
				memDim = 19.0;
				costDim = 3.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 8)) {
				memDim = 20.0;
				costDim = 4.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 10)) {
				memDim = 21.0;
				costDim = 5.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 12)) {
				memDim = 22.0;
				costDim = 6.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 14)) {
				memDim = 23.0;
				costDim = 7.0;
			} else {//(mem == ExtendedGiraphConfig.memFromAttributes(flavor, 16)) {
				memDim = 24.0;
				costDim = 8.0;
			}
		} else if (flavor == GiraphDirectory.GiraphVMFlavor.C4_4XLARGE) {
			dimensionsArray.add(4.0);
			if (mem == GiraphDirectory.memFromAttributes(flavor, 1)) {
				memDim = 17.0;
				costDim = 1.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 2)) {
				memDim = 18.0;
				costDim = 2.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 3)) {
				memDim = 19.0;
				costDim = 3.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 4)) {
				memDim = 20.0;
				costDim = 4.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 5)) {
				memDim = 21.0;
				costDim = 5.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 6)) {
				memDim = 22.0;
				costDim = 6.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 7)) {
				memDim = 23.0;
				costDim = 7.0;
			} else {//(mem == ExtendedGiraphConfig.memFromAttributes(flavor, 8)) {
				memDim = 24.0;
				costDim = 8.0;
			}
		} else if (flavor == GiraphDirectory.GiraphVMFlavor.R4_XLARGE) {
			dimensionsArray.add(5.0);
			if (mem == GiraphDirectory.memFromAttributes(flavor, 1)) {
				memDim = 25.0;
				costDim = 9.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 2)) {
				memDim = 26.0;
				costDim = 10.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 4)) {
				memDim = 27.0;
				costDim = 11.0;
			} else { //if (mem == ExtendedGiraphConfig.memFromAttributes(flavor, 8)) {
				memDim = 28.0;
				costDim = 12.0;
			}
		} else if (flavor == GiraphDirectory.GiraphVMFlavor.R4_2XLARGE) {
			dimensionsArray.add(6.0);
			if (mem == GiraphDirectory.memFromAttributes(flavor, 1)) {
				memDim = 29.0;
				costDim = 10.0;
			} else if (mem == GiraphDirectory.memFromAttributes(flavor, 2)) {
				memDim = 30.0;
				costDim = 11.0;
			} else { //if (mem == ExtendedGiraphConfig.memFromAttributes(flavor, 4)) {
				memDim = 31.0;
				costDim = 12.0;
			}
		} else if (flavor == GiraphDirectory.GiraphVMFlavor.R4_4XLARGE) {
			dimensionsArray.add(7.0);
			if (mem == GiraphDirectory.memFromAttributes(flavor, 1)) {
				memDim = 32.0;
				costDim = 11.0;
			} else {//if (mem == ExtendedGiraphConfig.memFromAttributes(flavor, 2)) {
				memDim = 33.0;
				costDim = 12.0;
			}
		} else {
			dimensionsArray.add(8.0);
			memDim = 34.0;
			costDim = 12.0;
		}
		
		// nr_workers
		switch (nr_workers) {
			case 1:
				dimensionsArray.add(1.0);
				break;
			case 2:
				dimensionsArray.add(2.0);
				break;
			case 3:
				dimensionsArray.add(3.0);
				break;
			case 4:
				dimensionsArray.add(4.0);
				break;
			case 5:
				dimensionsArray.add(5.0);
				break;
			case 6:
				dimensionsArray.add(6.0);
				break;
			case 7:
				dimensionsArray.add(7.0);
				break;
			case 8:
				dimensionsArray.add(8.0);
				break;
			case 10:
				dimensionsArray.add(9.0);
				break;
			case 12:
				dimensionsArray.add(10.0);
				break;
			case 14:
				dimensionsArray.add(11.0);
				break;
			case 16:
				dimensionsArray.add(12.0);
				break;
			case 20:
				dimensionsArray.add(13.0);
				break;
			case 24:
				dimensionsArray.add(14.0);
				break;
			case 28:
				dimensionsArray.add(15.0);
				break;
			case 32:
				dimensionsArray.add(16.0);
				break;
			case 40:
				dimensionsArray.add(17.0);
				break;
			case 48:
				dimensionsArray.add(18.0);
				break;
			case 56:
				dimensionsArray.add(19.0);
				break;
			case 64:
				dimensionsArray.add(20.0);
				break;
		}
		
		// total mem
		dimensionsArray.add(memDim);
		
		// total cores
		//{4, 8, 16, 32, 48, 64, 80, 96, 112, 128};
		switch ((int)totalCores) {
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
		
		dimensionsArray.add(costDim);
		
		return dimensionsArray;
	}

	@Override
	protected void checkSamples(List samples, String type, int seed) {
		// TODO Auto-generated method stub
		
	}

	
}
