package weka.extendedScout;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.scout.ExtendedScoutVMConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 09.04.18
 */
public class ExtendedScoutVMConfigWekaTestSet extends AbstractConfigWekaTestSet<ExtendedScoutVMConfig, WekaModelSample> {
   public ExtendedScoutVMConfigWekaTestSet(Instances set, String arff) {
      super(set, arff);
   }

   public ExtendedScoutVMConfigWekaTestSet(String arff) {
      super(arff);
   }

   @Override
   public TestSet<ExtendedScoutVMConfig, WekaModelSample> clone() {
      return new ExtendedScoutVMConfigWekaTestSet(new Instances(this.set), this.arff);
   }

   @Override
   public ExtendedScoutVMConfig buildFromInstance(Instance i) {
      return new ExtendedWekaScoutVMConfig(i);
   }


   @Override
   protected WekaConfiguration buildFromConfigAndSet(ExtendedScoutVMConfig c, Instances set) {
      return new ExtendedWekaScoutVMConfig(c, set);
   }

}
