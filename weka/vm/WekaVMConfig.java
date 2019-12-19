package weka.vm;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.vm.VMConfig;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 12.03.18
 */
public class WekaVMConfig extends VMConfig implements WekaConfiguration {

   @Override
   public WekaVMConfig clone() {
      return  new WekaVMConfig(this,this.dataSet);   //NB: this is *not* copying the instances
   }

   private Instances dataSet; //this is only for creating instances

   public WekaVMConfig(Instance i) {
      super((int) i.value(0), (int) i.value(1), i.value(2), (int) i.value(3), (int) i.value(4));
      this.dataSet = i.dataset();
      if (this.dataSet == null)
         throw new RuntimeException("Cannot be null");
   }



   public Instance toInstance() {
      double ret[] = new double[dataSet.numAttributes()];   //leave room for the target attribute as well
      ret[0] = type;
      ret[1] = vcpus;
      ret[2] = ecus;
      ret[3] = ram;
      ret[4] = num_instances;
      Instance rr = new DenseInstance(1.0, ret);
      rr.setDataset(this.dataSet);
      return rr;
   }

   public WekaVMConfig(VMConfig c, Instances d) {
      super(c.getType(), c.getVcpus(), c.getEcus(), c.getRam(), c.getNum_instances());
      this.dataSet = d;
      if (this.dataSet == null)
         throw new RuntimeException("Cannot be null");
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
}
