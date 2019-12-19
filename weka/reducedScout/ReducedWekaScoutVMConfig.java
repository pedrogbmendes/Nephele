package weka.reducedScout;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.aws.AWSDirectory;
import lynceus.scout.ReducedScoutVMConfig;
import weka.WekaGaussianProcess;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ReducedWekaScoutVMConfig extends ReducedScoutVMConfig implements WekaConfiguration {

   /* class attributes */
   private Instances dataSet; //this is only for creating instances

   /* class constructor */
   public ReducedWekaScoutVMConfig(ReducedScoutVMConfig c, Instances d) {
      super(c.getNum_instances(), c.getVm_type(), c.getVm_size(), c.getVcpus());
      this.dataSet = d;
      if (this.dataSet == null)
         throw new RuntimeException("Cannot be null");
   }

   private static AWSDirectory.AWSInstanceType typeFromInstance(Instance i) {
      if (WekaGaussianProcess.useNominalAttributes)
         return AWSDirectory.AWSInstanceType.valueOf(i.stringValue(1));
      else {
         if (i.value(1) == 1)
            return AWSDirectory.AWSInstanceType.C4;
         if (i.value(2) == 1)
            return AWSDirectory.AWSInstanceType.M4;
         return AWSDirectory.AWSInstanceType.R4;
      }
   }

   private static int vcpusIndex() {
      if (WekaGaussianProcess.useNominalAttributes)
         return 2;
      return 4;
   }

   private static AWSDirectory.AWSInstanceSize sizeFromInstance(Instance i) {
      return AWSDirectory.sizeFromAttributes(typeFromInstance(i), i.value(vcpusIndex()), 0.0, 0.0);
   }


   public ReducedWekaScoutVMConfig(Instance i) {
      super((int) i.value(0), typeFromInstance(i), sizeFromInstance(i), i.value(vcpusIndex()));   // we don't care about the values for ram and ecus
      this.dataSet = i.dataset();
      if (this.dataSet == null)
         throw new RuntimeException("Cannot be null");
   }

   /* abstract methods to be implemented */
   @Override
   public ReducedWekaScoutVMConfig clone() {
      return new ReducedWekaScoutVMConfig(this, this.dataSet);   //NB: this is *not* copying the instances
   }

   @Override
   public Instance toInstance() {
      if (WekaGaussianProcess.useNominalAttributes) {
         Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
         rr.setDataset(this.dataSet); //first set dataset, otherwise following statements fail with "no dataset associated" exception
         rr.setValue(0, this.getNum_instances());
         rr.setValue(1, this.getVm_type().toString());
         rr.setValue(2, this.getVcpus());
         return rr;
      } else {
         Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
         rr.setDataset(this.dataSet); //first set dataset, otherwise following statements fail with "no dataset associated" exception
         rr.setValue(0, this.getNum_instances());
         rr.setValue(1, indexType(this.getVm_type(), 1));
         rr.setValue(2, indexType(this.getVm_type(), 2));
         rr.setValue(3, indexType(this.getVm_type(), 3));
         rr.setValue(4, this.getVcpus());
         return rr;
      }
   }

   private int indexType(AWSDirectory.AWSInstanceType type, int index) {
      switch (type) {
         case C4:
            return index == 1 ? 1 : 0;
         case M4:
            return index == 2 ? 1 : 0;
         case R4:
            return index == 3 ? 1 : 0;
         default:
            throw new RuntimeException();
      }
   }

   @Override
   public ArrayList<WekaConfiguration> neighbourhood() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public boolean findPair(ArrayList<Pair> searchSpace, Pair pair) {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String toString() {
      //return "ReducedWekaScoutVMConfig [dataSet=" + dataSet + "]";
      return super.toString();
   }

}
