package weka.extendedGiraph;

import lynceus.giraph.ExtendedGiraphConfig;
import lynceus.giraph.GiraphDirectory;

public class ExtendedWekaGiraphConfigFactory {
	
	public static int wkld; // 0 = SSSP ; 1 = Page Rank ; 2 = Connected Components

	
	public static void setWkld(int wkld) {
		ExtendedWekaGiraphConfigFactory.wkld = wkld;
	}
	
	public static ExtendedGiraphConfigWekaTestSet buildInitTestSet(String arff){
		
		final ExtendedGiraphConfigWekaTestSet testSet = new ExtendedGiraphConfigWekaTestSet(arff);
				
		/****************************************************************************************
		 * 																						*
		 * 	   - C4.large 	==> nr_workers = {8, 16, 24, 32, 40, 48, 56, 64}	// 2  vCPUs		*
		 *     - C4.xlarge 	==> nr_workres = {4, 8, 12, 16, 20, 24, 28, 32}		// 4  vCPUs		*
		 *     - C4.2xlarge	==> nr_workers = {2, 4, 6, 8, 10, 12, 14, 16}		// 8  vCPUs		*
		 *     - C4.4xlarge ==> nr_workers = {1, 2, 3, 4, 5, 6, 7, 8}			// 16 vCPUs		*
		 *     - R4.xlarge 	==> nr_workers = {1, 2, 4, 8}						// 4  vCPUs		*
		 *     - R4.2xlarge	==> nr_workers = {1, 2, 4}							// 8  vCPUs		*
		 *     - R4.4xlarge ==> nr_workers = {1, 2}								// 16 vCPUs		*
		 *     - R4.8xlarge ==> nr_workers = {1}								// 32 vCPUs		*
		 *     																					*
		 ****************************************************************************************/
		
		int   nr_workers;
		int[] c4_total_worker_cores = new int[] {16, 32, 48, 64, 80, 96, 112, 128};
		int[] r4_total_worker_cores = new int[] {4, 8, 16, 32};
		int[] graphs = new int[] {0};				// 0 = orkut
		int[] c4_vm_size = new int [] {0, 1, 2, 3};
		int[] r4_vm_size = new int [] {1, 2, 3, 4};
		double mem;
				
		for (int cores : c4_total_worker_cores) {
			for (int size : c4_vm_size) {
				for (int graph : graphs){
					if (size == 0) {	// large
						nr_workers = cores / 2;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.C4_LARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.C4_LARGE, nr_workers, mem, cores, graph, wkld);
						testSet.addTestSample(a);
					} else if (size == 1) {	// xlarge
						nr_workers = cores / 4;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.C4_XLARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.C4_XLARGE, nr_workers, mem, cores, graph, wkld);
						testSet.addTestSample(a);
					} else if (size == 2) {	// 2xlarge
						nr_workers = cores / 8;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.C4_2XLARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.C4_2XLARGE, nr_workers, mem, cores, graph, wkld);
						testSet.addTestSample(a);
					} else {	// 4xlarge
						nr_workers = cores / 16;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.C4_4XLARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.C4_4XLARGE, nr_workers, mem, cores, graph, wkld);
						testSet.addTestSample(a);
					}
				}
			}
		}
		
		for (int size : r4_vm_size) {
			for (int graph : graphs){
				if (size == 1) {	// xlarge
					for (int i = 0; i < r4_total_worker_cores.length; i++) {
						nr_workers = r4_total_worker_cores[i] / 4;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.R4_XLARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.R4_XLARGE, nr_workers, mem, r4_total_worker_cores[i], graph, wkld);
						testSet.addTestSample(a);
					}
				} else if (size == 2) {	// 2xlarge
					for (int i = 1; i < r4_total_worker_cores.length; i++) {
						nr_workers = r4_total_worker_cores[i] / 8;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.R4_2XLARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.R4_2XLARGE, nr_workers, mem, r4_total_worker_cores[i], graph, wkld);
						testSet.addTestSample(a);
					}
				} else if (size == 3) {	// 4xlarge
					for (int i = 2; i < r4_total_worker_cores.length; i++) {
						nr_workers = r4_total_worker_cores[i] / 16;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.R4_4XLARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.R4_4XLARGE, nr_workers, mem, r4_total_worker_cores[i], graph, wkld);
						testSet.addTestSample(a);
					}
				} else {	// 8xlarge
					for (int i = 3; i < r4_total_worker_cores.length; i++) {
						nr_workers = r4_total_worker_cores[i] / 32;
						mem = GiraphDirectory.memFromAttributes(GiraphDirectory.GiraphVMFlavor.R4_8XLARGE, nr_workers);
						ExtendedGiraphConfig a = new ExtendedGiraphConfig(GiraphDirectory.GiraphVMFlavor.R4_8XLARGE, nr_workers, mem, r4_total_worker_cores[i], graph, wkld);
						testSet.addTestSample(a);
					}
				}
			}
		}
		
		return testSet;
	}
	
}
