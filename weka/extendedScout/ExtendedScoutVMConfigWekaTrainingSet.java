package weka.extendedScout;

import lynceus.TrainingSet;
import lynceus.scout.ExtendedScoutVMConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class ExtendedScoutVMConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<ExtendedScoutVMConfig, WekaModelSample> {
   public ExtendedScoutVMConfigWekaTrainingSet(String arff) {
      super(arff);
   }


   private ExtendedScoutVMConfigWekaTrainingSet(Instances i) {
      super(i);
   }

   @Override
   public TrainingSet<ExtendedScoutVMConfig, WekaModelSample> clone() {
      return new ExtendedScoutVMConfigWekaTrainingSet(this.set);
   }


   @Override
   protected ExtendedScoutVMConfig buildConfigFromInstance(Instance i) {
      return new ExtendedWekaScoutVMConfig(i);
   }
}
