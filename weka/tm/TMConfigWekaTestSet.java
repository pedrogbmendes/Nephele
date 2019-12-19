package weka.tm;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.tm.TMConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 20.03.18
 */
public class TMConfigWekaTestSet extends AbstractConfigWekaTestSet<TMConfig, WekaModelSample> {
   public TMConfigWekaTestSet(Instances set, String arff) {
      super(set, arff);
   }

   public TMConfigWekaTestSet(String arff) {
      super(arff);
   }

   @Override
   public TMConfig buildFromInstance(Instance i) {
      return new WekaTMConfig(i);
   }

   @Override
   protected WekaConfiguration buildFromConfigAndSet(TMConfig tmConfig, Instances i) {
      return new WekaTMConfig(tmConfig, i);
   }

   @Override
   public TestSet<TMConfig, WekaModelSample> clone() {
      return new TMConfigWekaTestSet(new Instances(this.set), this.arff);
   }

}
