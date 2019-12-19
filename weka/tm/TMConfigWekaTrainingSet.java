package weka.tm;

import lynceus.TrainingSet;
import lynceus.tm.TMConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 21.03.18
 */
public class TMConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<TMConfig, WekaModelSample> {
   TMConfigWekaTrainingSet(String arff) {
      super(arff);
   }


   private TMConfigWekaTrainingSet(Instances i) {
      super(i);
   }


   @Override
   public TrainingSet<TMConfig, WekaModelSample> clone() {
      return new TMConfigWekaTrainingSet(this.set);
   }

   @Override
   protected TMConfig buildConfigFromInstance(Instance i) {
      return new WekaTMConfig(i);
   }
}
