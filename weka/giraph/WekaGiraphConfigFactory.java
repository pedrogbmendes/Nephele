package weka.giraph;

import lynceus.giraph.GiraphConfig;

public class WekaGiraphConfigFactory {
	
	public static int wkld; // 0 = SSSP ; 1 = Page Rank ; 2 = Connected Components

	
	public static void setWkld(int wkld) {
		WekaGiraphConfigFactory.wkld = wkld;
	}


	public static GiraphConfigWekaTestSet buildInitTestSet(String arff){
		
		final GiraphConfigWekaTestSet testSet = new GiraphConfigWekaTestSet(arff);
		
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
		
		for (int cores : c4_total_worker_cores) {
			for (int size : c4_vm_size) {
				for (int graph : graphs){
					if (size == 0) {	// large
						nr_workers = cores / 2;
						GiraphConfig a = new GiraphConfig(0, 0, nr_workers, graph, wkld);
						testSet.addTestSample(a);
					} else if (size == 1) {	// xlarge
						nr_workers = cores / 4;
						GiraphConfig a = new GiraphConfig(0, 1, nr_workers, graph, wkld);
						testSet.addTestSample(a);
					} else if (size == 2) {	// 2xlarge
						nr_workers = cores / 8;
						GiraphConfig a = new GiraphConfig(0, 2, nr_workers, graph, wkld);
						testSet.addTestSample(a);
					} else {	// 4xlarge
						nr_workers = cores / 16;
						GiraphConfig a = new GiraphConfig(0, 3, nr_workers, graph, wkld);
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
						GiraphConfig a = new GiraphConfig(1, 1, nr_workers, graph, wkld);
						testSet.addTestSample(a);
					}
				} else if (size == 2) {	// 2xlarge
					for (int i = 1; i < r4_total_worker_cores.length; i++) {
						nr_workers = r4_total_worker_cores[i] / 8;
						GiraphConfig a = new GiraphConfig(1, 2, nr_workers, graph, wkld);
						testSet.addTestSample(a);
					}
				} else if (size == 3) {	// 4xlarge
					for (int i = 2; i < r4_total_worker_cores.length; i++) {
						nr_workers = r4_total_worker_cores[i] / 16;
						GiraphConfig a = new GiraphConfig(1, 3, nr_workers, graph, wkld);
						testSet.addTestSample(a);
					}
				} else {	// 8xlarge
					for (int i = 3; i < r4_total_worker_cores.length; i++) {
						nr_workers = r4_total_worker_cores[i] / 32;
						GiraphConfig a = new GiraphConfig(1, 4, nr_workers, graph, wkld);
						testSet.addTestSample(a);
					}
				}
			}
		}
		
			
//		/* flavor = c4.large ==> vm_type = 0 && vm_size = 0 */
//		if (wkld == 0 || wkld == 2) {	// SSSP or CC	---- PR ==> all OOM
//			total_worker_cores = new int[] {32, 48, 64, 80, 96, 112, 128};
//			for (int graph : graphs){
//				for (int cores : total_worker_cores){
//					nr_workers = cores/2;
//					GiraphConfig a = new GiraphConfig(0, 0, nr_workers, graph, wkld);
//					testSet.addTestSample(a);
//				}
//			}
//		}
//		
//		/* flavor = c4.xlarge ==> vm_type = 0 && vm_size = 1 */
//		if (wkld == 0) {		// SSSP
//			total_worker_cores = new int[] {16, 32, 48, 64, 80, 96, 112, 128};
//		} else if (wkld == 1) {	// PR
//			total_worker_cores = new int[] {80, 96, 112, 128};
//		} else {				// CC
//			total_worker_cores = new int[] {32, 48, 64, 80, 96, 112, 128};
//		}
//		for (int graph : graphs){
//			for (int cores : total_worker_cores){
//				nr_workers = cores/4;
//				GiraphConfig a = new GiraphConfig(0, 1, nr_workers, graph, wkld);
//				testSet.addTestSample(a);	
//			}
//		}
//		
//		/* flavor = c4.2xlarge ==> vm_type = 0 && vm_size = 2 */
//		if (wkld == 0) {		// SSSP
//			total_worker_cores = new int[] {16, 32, 48, 64, 80, 96, 112, 128};
//		} else if (wkld == 1) {	// PR
//			total_worker_cores = new int[] {80, 96, 112, 128};
//		} else {				// CC
//			total_worker_cores = new int[] {32, 48, 64, 80, 96, 112, 128};
//		}
//		for (int graph : graphs){
//			for (int cores : total_worker_cores){
//				nr_workers = cores/8;
//				GiraphConfig a = new GiraphConfig(0, 2, nr_workers, graph, wkld);
//				testSet.addTestSample(a);
//			}
//		}
//		
//		/* flavor = c4.4xlarge ==> vm_type = 0 && vm_size = 3 */
//		if (wkld == 0 || wkld == 2) {	// SSSP or CC
//			total_worker_cores = new int[] {16, 32, 48, 64, 80, 96, 112, 128};
//		} else {	// PR
//			total_worker_cores = new int[] {80, 96, 112, 128};
//		}
//		for (int graph : graphs){
//			for (int cores : total_worker_cores){
//				nr_workers = cores/16;
//				GiraphConfig a = new GiraphConfig(0, 3, nr_workers, graph, wkld);
//				testSet.addTestSample(a);
//			}
//		}
//		
//		/* flavor = r4.xlarge = 4 ==> vm_type = 1 && vm_size = 1 */
//		if( wkld == 1){
//			total_worker_cores = new int[] {16, 32};
//		} else {
//			total_worker_cores = new int[] {4, 8, 16, 32};
//		}
//		for (int graph : graphs){
//			for (int cores : total_worker_cores){
//				nr_workers = cores/4;
//				GiraphConfig a = new GiraphConfig(1, 1, nr_workers, graph, wkld);
//				testSet.addTestSample(a);
//			}
//		}
//		
//		/* flavor = r4.2xlarge = 5 ==> vm_type = 1 && vm_size = 2 */
//		total_worker_cores = new int[] {8, 16, 32};
//		for (int graph : graphs){
//			for (int cores : total_worker_cores){
//				nr_workers = cores/8;
//				GiraphConfig a = new GiraphConfig(1, 2, nr_workers, graph, wkld);
//				testSet.addTestSample(a);
//			}
//		}
//		
//		/* flavor = r4.4xlarge = 6 ==> vm_type = 1 && vm_size = 3 */
//		total_worker_cores = new int[] {16, 32};
//		for (int graph : graphs){
//			for (int cores : total_worker_cores){
//				nr_workers = cores/16;
//				GiraphConfig a = new GiraphConfig(1, 3, nr_workers, graph, wkld);
//				testSet.addTestSample(a);
//			}
//		}
//		
//		/* flavor = r4.8xlarge = 7 ==> vm_type = 1 && vm_size = 4 */
//		total_worker_cores = new int[] {32};
//		for (int graph : graphs){
//			for (int cores : total_worker_cores){
//				nr_workers = cores/32;
//				GiraphConfig a = new GiraphConfig(1, 4, nr_workers, graph, wkld);
//				testSet.addTestSample(a);
//			}
//		}

		
		return testSet;
	}
	
}
