package weka;

import lynceus.CostGenerator;
import lynceus.vm.VMConfig;
import weka.tuning.ModelParams;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 16.03.18
 */
public class OmniscentWekaGaussianProcess extends WekaGaussianProcess<VMConfig> {
   private CostGenerator<VMConfig> costGenerator;

   public OmniscentWekaGaussianProcess(WekaSet set, CostGenerator<VMConfig> c,ModelParams params) {
      super(set,params);
      costGenerator = c;
   }

   @Override
   public double evaluate(VMConfig config) {
      return costGenerator.deploymentCost(null, config);
   }

   public double stdv(VMConfig config) {
      return 1.0; //very little variance.
   }
}
