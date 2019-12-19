package lynceus.giraph;

public class GiraphDirectory {

	public enum GiraphVMFlavor {
		C4_LARGE, C4_XLARGE, C4_2XLARGE, C4_4XLARGE, R4_XLARGE, R4_2XLARGE, R4_4XLARGE, R4_8XLARGE
	}

	public static GiraphVMFlavor vmFlavorFromInt (int flavor) {
		switch(flavor) {
			case 0:
				return GiraphVMFlavor.C4_LARGE;
			case 1:
				return GiraphVMFlavor.C4_XLARGE;
			case 2:
				return GiraphVMFlavor.C4_2XLARGE;
			case 3:
				return GiraphVMFlavor.C4_4XLARGE;
			case 4:
				return GiraphVMFlavor.R4_XLARGE;
			case 5:
				return GiraphVMFlavor.R4_2XLARGE;
			case 6:
				return GiraphVMFlavor.R4_4XLARGE;
			case 7:
				return GiraphVMFlavor.R4_8XLARGE;
			default:
				throw new RuntimeException("[GiraphDirectory] unknow flavor " + flavor);
		}
	}
	
	public static int vmFlavorToInt (GiraphVMFlavor flavor) {
		switch (flavor) {
		case C4_LARGE:
			return 0 ;
		case C4_XLARGE: 
			return 1;
		case C4_2XLARGE:
			return 2;
		case C4_4XLARGE:
			return 3;
		case R4_XLARGE:
			return 4;
		case R4_2XLARGE:
			return 5;
		case R4_4XLARGE:
			return 6;
		case R4_8XLARGE:
			return 7;
		default:
			throw new RuntimeException("[GiraphDirectory] unknow flavor " + flavor);
		}
	}
	
	public static double memFromAttributes (GiraphVMFlavor flavor, int workers) {
		switch (flavor) {
			case C4_LARGE:
				return (double) Math.round(workers * 1.792 * 100) / 100D;
			case C4_XLARGE: 
				return (double) Math.round(workers * 5.632 * 100) / 100D;
			case C4_2XLARGE:
				return (double) Math.round(workers * 11.520 * 100) / 100D;
			case C4_4XLARGE:
				return (double) Math.round(workers * 23.040 * 100) / 100D;
			case R4_XLARGE:
				return (double) Math.round(workers * 23.424 * 100) / 100D;
			case R4_2XLARGE:
				return (double) Math.round(workers * 54.272 * 100) / 100D;
			case R4_4XLARGE:
				return (double) Math.round(workers * 116.736 * 100) / 100D;
			case R4_8XLARGE:
				return (double) Math.round(workers * 241.664 * 100) / 100D;
			default:
				throw new RuntimeException("[GiraphDirectory] unknow flavor " + flavor);
		}
	}

	public static GiraphVMFlavor vmFlavorFromString(String flavor) {
		switch(flavor) {
			case "c4.large":
				return GiraphVMFlavor.C4_LARGE;
			case "c4.xlarge":
				return GiraphVMFlavor.C4_XLARGE;
			case "c4.2xlarge":
				return GiraphVMFlavor.C4_2XLARGE;
			case "c4.4xlarge":
				return GiraphVMFlavor.C4_4XLARGE;
			case "r4.xlarge":
				return GiraphVMFlavor.R4_XLARGE;
			case "r4.2xlarge":
				return GiraphVMFlavor.R4_2XLARGE;
			case "r4.4xlarge":
				return GiraphVMFlavor.R4_4XLARGE;
			case "r4.8xlarge":
				return GiraphVMFlavor.R4_8XLARGE;
			default:
				throw new RuntimeException("[GiraphDirectory] VM flavor " + flavor + " inexistent in dataset");
		}
	}
	
	public static double vmCostPerMinFromAttributes(GiraphVMFlavor flavor, int num_instances) {
		num_instances = num_instances + 1;
		switch(flavor) {
			case C4_LARGE:
				return num_instances * (0.119 + 0.027)/60.0;
			case C4_XLARGE:
				return num_instances * (0.237 + 0.053)/60.0;
			case C4_2XLARGE:
				return num_instances * (0.476 + 0.105)/60.0;
			case C4_4XLARGE:
				return num_instances * (0.95 + 0.210)/60.0;
			case R4_XLARGE:
				return num_instances * (0.312 + 0.067)/60.0;
			case R4_2XLARGE:
				return num_instances * (0.624 + 0.133)/60.0;
			case R4_4XLARGE:
				return num_instances * (1.248 + 0.266)/60.0;
			case R4_8XLARGE:
				return num_instances * (2.496 + 0.270)/60.0;
			default:
				throw new RuntimeException("[GiraphDirectory] Inexistent vm flavor " + flavor);
		}
	}
	
}
