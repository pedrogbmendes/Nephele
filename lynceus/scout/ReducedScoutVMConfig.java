package lynceus.scout;

import static lynceus.aws.AWSDirectory.cpusFor;

import java.util.Objects;

import lynceus.Configuration;
import lynceus.aws.AWSDirectory;
import lynceus.aws.AWSDirectory.AWSInstanceSize;
import lynceus.aws.AWSDirectory.AWSInstanceType;
import weka.WekaGaussianProcess;

public class ReducedScoutVMConfig implements Configuration {

   /* class attributes */
   private AWSDirectory.AWSInstanceType vm_type;
   private AWSDirectory.AWSInstanceSize vm_size; /*Size is not used as an attribute. We just use it for identifying the cost  */
   private double vcpus;
   private int num_instances;

   /* class constructor */
   public ReducedScoutVMConfig(int num_instances, AWSInstanceType vm_type, AWSInstanceSize vm_size, double vcpus) {
      this.num_instances = num_instances;
      this.vm_type = vm_type;
      this.vm_size = vm_size;
      this.vcpus = vcpus;
   }


   /* interface methods to be implemented */
   @Override
   public int numAttributes() {
      if (WekaGaussianProcess.useNominalAttributes)
         return 3;
      return 5;
   }

	 @Override
	 /*Size is not used as an attribute. We just use it for identifying the cost  */
	 public Object at(int i) {
	    switch (i) {
	       case 0:
	          return num_instances;
	       case 1:
	          return vm_type;
	       case 2: 
	    	   return vcpus;
	       default:
	          throw new RuntimeException("Attribute " + i + " not defined for " + this.getClass());
	    }
	 }

   @Override
   public Configuration clone() {
      return new ReducedScoutVMConfig(num_instances, vm_type, vm_size, vcpus);
   }

   /* getters */
   public int getNum_instances() {
      return num_instances;
   }

   public AWSInstanceType getVm_type() {
      return vm_type;
   }

   public AWSInstanceSize getVm_size() {
      return vm_size;
   }

   public double getVcpus() {
      return vcpus;
   }


   /* other methods */
   @Override
   public String toString() {
      return "ReducedScoutVMConfig [num_instances=" + num_instances + ", vm_type=" + vm_type + ", vm_size=" + vm_size
            + ", vcpus=" + vcpus + "]";
   }


   @Override
   public int hashCode() {
//		 final int prime = 31;
//		 int result = 1;
//		 result = prime * result + num_instances;
//		 long temp;
//		 temp = Double.doubleToLongBits(vcpus);
//		 result = prime * result + (int) (temp ^ (temp >>> 32));
//		 result = prime * result + ((vm_size == null) ? 0 : vm_size.hashCode());
//		 result = prime * result + ((vm_type == null) ? 0 : vm_type.hashCode());
//		 return result;
      return Objects.hash(vm_type, vm_size, num_instances, vcpus);
   }


   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
//		if (getClass() != obj.getClass())
//			return false;
      ReducedScoutVMConfig other = (ReducedScoutVMConfig) obj;
      if (num_instances != other.num_instances)
         return false;
      if (Double.doubleToLongBits(vcpus) != Double.doubleToLongBits(other.vcpus))
         return false;
      if (vm_size != other.vm_size)
         return false;
      if (vm_type != other.vm_type)
         return false;
      return true;
   }


   public static ReducedScoutVMConfig parseName(String name) {
      String[] split = name.split("_");
      int numInstances = Integer.parseInt(split[0]);
      AWSDirectory.AWSInstanceSize size;
      AWSDirectory.AWSInstanceType type;
      String type_size = split[1];
      if (type_size.contains("c4")) {
         type = AWSDirectory.AWSInstanceType.C4;
      } else if (type_size.contains("r4")) {
         type = AWSDirectory.AWSInstanceType.R4;
      } else if (type_size.contains("m4")) {
         type = AWSDirectory.AWSInstanceType.M4;
      } else {
         throw new RuntimeException(type_size + " has unrecognized type");
      }

      //note that "2xlarge" and "xlarge" contain "large" so we have  to check large as last
      //Note that the AWS name is "2xlarge" and not "x2large" as in our enum
      if (type_size.contains("2xlarge")) {
         size = AWSDirectory.AWSInstanceSize.x2large;
      } else if (type_size.contains("xlarge")) {
         size = AWSDirectory.AWSInstanceSize.xlarge;
      } else if (type_size.contains("large")) {
         size = AWSDirectory.AWSInstanceSize.large;
      } else {
         throw new RuntimeException(type_size + " has unrecognized size");
      }

      double cpus = cpusFor(size, type);

      return new ReducedScoutVMConfig(numInstances, type, size, cpus);
   }

}
