package lynceus.scout;

import static lynceus.aws.AWSDirectory.cpusFor;
import static lynceus.aws.AWSDirectory.ecusFor;
import static lynceus.aws.AWSDirectory.ramFor;

import java.util.Objects;

import lynceus.Configuration;
import lynceus.aws.AWSDirectory;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 29.03.18
 */
public class ScoutVMConfig implements Configuration {

   /* class attributes */
   private AWSDirectory.AWSInstanceType type;
   private AWSDirectory.AWSInstanceSize size;
   private double vcpus;
   private double ecus;
   private double ram;
   private double num_instances;

   /* class constructors */
   public ScoutVMConfig(AWSDirectory.AWSInstanceType type, AWSDirectory.AWSInstanceSize size, double vcpus, double ecus, double ram, double num_instances) {
	  this.type = type;
	  this.size = size;
	  this.vcpus = vcpus;
	  this.ecus = ecus;
	  this.ram = ram;
	  this.num_instances = num_instances;
   }
   
   /* interface methods to be implemented */
   @Override
   public int numAttributes() {
      return 5;
   }

   @Override
   /*Size is not used as an attribute. We just use it for identifying the cost  */
   public Object at(int i) {
      switch (i) {
         case 0:
            return String.valueOf(type);
         case 1:
            return vcpus;
         case 2:
            return ecus;
         case 3:
            return ram;
         case 4:
            return num_instances;
         default:
            throw new RuntimeException("Attribute " + i + " not defined for " + this.getClass());
      }
   }

   @Override
   public Configuration clone() {
      return new ScoutVMConfig(type, size, vcpus, ecus, ram, num_instances);
   }
   
   
   /* getters */
   public AWSDirectory.AWSInstanceType getType() {
      return type;
   }

   public double getVcpus() {
      return vcpus;
   }

   public double getEcus() {
      return ecus;
   }

   public double getRam() {
      return ram;
   }

   public double getNum_instances() {
      return num_instances;
   }

   public AWSDirectory.AWSInstanceSize getSize() {
      return size;
   }


   /* other methods */
   @Override
   public String toString() {
      return "ScoutVMConfig{" +
            "type=" + type +
            ", size=" + size +
            ", vcpus=" + vcpus +
            ", ecus=" + ecus +
            ", ram=" + ram +
            ", num_instances=" + num_instances +
            '}';
   }

   public static ScoutVMConfig parseName(String name) {
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

           double ram = ramFor(size, type);
           double cpus = cpusFor(size, type);
           double ecus = ecusFor(size, type);

           return new ScoutVMConfig(type, size, cpus, ecus, ram, numInstances);

        }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      //NB: I want to consider as equal different objects that refer to the same config, even across classes (i.e., config vs wekaconfig etc)
           //if (o == null || getClass() != o.getClass()) return false;
      ScoutVMConfig that = (ScoutVMConfig) o;
      return Double.compare(that.vcpus, vcpus) == 0 &&
            Double.compare(that.ecus, ecus) == 0 &&
            Double.compare(that.ram, ram) == 0 &&
            Double.compare(that.num_instances, num_instances) == 0 &&
            type == that.type &&
            size == that.size;
   }

   @Override
   public int hashCode() {
      return Objects.hash(type, size, vcpus, ecus, ram, num_instances);
   }

}
