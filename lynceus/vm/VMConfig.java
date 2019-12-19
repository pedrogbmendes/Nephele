package lynceus.vm;

import lynceus.Configuration;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */

public class VMConfig implements Configuration {
   public final static int m5_large = 0;
   public final static int M4 = 1;
   public final static int C4 = 2;
   public final static int R4 = 3;
   public final static int m5_xlarge = 4;
   public final static int m5_2xlarge = 5;
   public final static int m5_4xlarge = 6;
   public final static int m5_12xlarge = 7;
   public final static int m5_24xlarge = 8;

   protected int type;
   protected int vcpus;
   protected double ecus;
   protected int ram;
   protected int num_instances;

   public VMConfig clone() {
      return new VMConfig(this.type, this.vcpus, this.ecus, this.ram, this.num_instances);
   }

   @Override
   public String toString() {
      return "VMConfig{" +
            "type=" + type +
            ", vcpus=" + vcpus +
            ", ecus=" + ecus +
            ", ram=" + ram +
            ", num_instances=" + num_instances +
            '}';
   }

   public int numAttributes() {
      return 5;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this)
         return true;
      if (!(obj instanceof VMConfig)) {
         return false;
      }
      VMConfig that = (VMConfig) obj;
      for (int i = 0; i < this.numAttributes(); i++) {
         if (!that.at(i).equals(this.at(i))) {
            return false;
         }
      }
      return true;
   }

   public Object at(int i) {
      switch (i) {
         case 0:
            return type;
         case 1:
            return (double) vcpus;
         case 2:
            return ecus;
         case 3:
            return (double) ram;
         case 4:
            return (double) num_instances;
         default:
            throw new RuntimeException("VMConfig only has " + numAttributes() + " attributes. You asked for " + (i + 1));
      }
   }

   protected VMConfig() {
   }

   public int getType() {
      return type;
   }

   public int getVcpus() {
      return vcpus;
   }

   public double getEcus() {
      return ecus;
   }

   public int getRam() {
      return ram;
   }

   public int getNum_instances() {
      return num_instances;
   }

   public VMConfig(int type, int vcpus, double ecus, int ram, int num_instances) {
      this.type = type;
      this.vcpus = vcpus;
      this.ecus = ecus;
      this.ram = ram;
      this.num_instances = num_instances;
   }

}
