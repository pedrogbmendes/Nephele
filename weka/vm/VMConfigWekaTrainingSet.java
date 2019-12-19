package weka.vm;

import lynceus.TrainingSet;
import lynceus.vm.VMConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public class VMConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<VMConfig, WekaModelSample> {

   VMConfigWekaTrainingSet(String arff) {
      super(arff);
   }


   private VMConfigWekaTrainingSet(Instances i) {
      super(i);
   }

   @Override
   public TrainingSet<VMConfig, WekaModelSample> clone() {
      return new VMConfigWekaTrainingSet(this.set);
   }


   @Override
   protected VMConfig buildConfigFromInstance(Instance i) {
      return new WekaVMConfig(i);
   }
}
