package weka.vm;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.vm.VMConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public class VMConfigWekaTestSet extends AbstractConfigWekaTestSet<VMConfig, WekaModelSample> {


   public VMConfigWekaTestSet(Instances set, String arff) {
      super(set, arff);
   }

   public VMConfigWekaTestSet(String arff) {
      super(arff);
   }

   @Override
   public TestSet<VMConfig, WekaModelSample> clone() {
      return new VMConfigWekaTestSet(this.set, this.arff);
   }

   @Override
   public VMConfig buildFromInstance(Instance i) {
      return new WekaVMConfig(i);
   }


   @Override
   protected WekaConfiguration buildFromConfigAndSet(VMConfig c, Instances set) {
      return new WekaVMConfig(c, set);
   }


   /*
   private VMConfigWekaTestSet(String arff, Instances instances) {
      this.arff = arff;
      this.set = new Instances(instances);      //This copies all instances and header references
   }
   */


}
