package weka.tensorflow;

import lynceus.tensorflow.TensorflowConfig;

public class WekaTensorflowConfigFactory {
	
	private static boolean pruned;

	public static void setPruned(boolean pruned) {
		WekaTensorflowConfigFactory.pruned = pruned;
	}



	public static TensorflowConfigWekaTestSet buildInitTestSet(String arff){
		
		final TensorflowConfigWekaTestSet testSet = new TensorflowConfigWekaTestSet(arff);
		
		int nr_workers;
		int[] 	 synchronism		= new int[]{0, 1};
		int[]	 total_worker_cores;
		int[] 	 batch_size_list 	= new int[]{16, 256};
		double[] learning_rate_list = new double[]{0.001, 0.0001, 0.00001};
		int[] 	 vm_flavors			= new int[]{0, 1, 2, 3};
		if(pruned){
			total_worker_cores	= new int[]{32, 48, 64, 80};//, 128};
		}else{
			total_worker_cores	= new int[]{8, 16, 32, 48, 64, 80};//, 128};
		}
//		/* t2.small ==> flavor = 0 */
//		for(int cores : total_worker_cores){
//			for (int bs : batch_size_list){				
//				for(double lr : learning_rate_list){
//					for(int sync : synchronism){
//						TensorflowConfig a = new TensorflowConfig(1, cores, lr, bs, sync, 0);
//						testSet.addTestSample(a);
//					}
//				}
//			}
//		}
//		
//		/* t2.medium ==> flavor = 1 */
//		for(int cores : total_worker_cores){
//			for (int bs : batch_size_list){				
//				for(double lr : learning_rate_list){
//					for(int sync : synchronism){
//						TensorflowConfig a = new TensorflowConfig(1, cores/2, lr, bs, sync, 1);
//						testSet.addTestSample(a);
//					}
//				}
//			}
//		}
//		
//		/* t2.2xlarge ==> flavor = 3 */
//		for(int cores : total_worker_cores){
//			for (int bs : batch_size_list){				
//				for(double lr : learning_rate_list){
//					for(int sync : synchronism){
//						TensorflowConfig a = new TensorflowConfig(1, cores/8, lr, bs, sync, 3);
//						testSet.addTestSample(a);
//					}
//				}
//			}
//		}
//		
//		
//		/* t2.xlarge ==> flavor = 2 */
//		total_worker_cores	= new int[]{8, 16, 32, 48, 64, 80};
//		for(int cores : total_worker_cores){
//			for (int bs : batch_size_list){				
//				for(double lr : learning_rate_list){
//					for(int sync : synchronism){
//						TensorflowConfig a = new TensorflowConfig(1, cores/4, lr, bs, sync, 2);
//						testSet.addTestSample(a);
//					}
//				}
//			}
//		}
				
		
		for (int flavor : vm_flavors){
			for(int cores : total_worker_cores){
				for (int bs : batch_size_list){				
					for(double lr : learning_rate_list){
						for(int sync : synchronism){
							switch(flavor){
								case 0: 	// t2.small
									nr_workers = cores;
									TensorflowConfig a = new TensorflowConfig(1, cores, lr, bs, sync, flavor);
									testSet.addTestSample(a);
									break;
								case 1:		// t2.medium
									nr_workers = cores/2;
									TensorflowConfig b = new TensorflowConfig(1, nr_workers, lr, bs, sync, flavor); 
									testSet.addTestSample(b);
									break;
								case 2:		// t2.xlarge
									nr_workers = cores/4;
									TensorflowConfig c = new TensorflowConfig(1, nr_workers, lr, bs, sync, flavor); 
									testSet.addTestSample(c);
									break;
								case 3:		// t2.2xlarge
									nr_workers = cores/8;
									TensorflowConfig d = new TensorflowConfig(1, nr_workers, lr, bs, sync, flavor); 
									testSet.addTestSample(d);
									break;
								default:
									System.out.println("[WekaConfigFactory] Inexistent vm flavor " + flavor);
									break;
							}
						}	
					}
				}
			}
		}
		return testSet;
	}
	
	
}
