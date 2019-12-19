package weka.tm;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.tm.TMConfig;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 20.03.18
 */
public class WekaTMConfig extends TMConfig implements WekaConfiguration {
   @Override
   public WekaTMConfig clone() {
      return new WekaTMConfig(this, this.dataSet);   //NB: this is *not* copying the instances
   }

   private Instances dataSet; //this is only for creating instances

   public WekaTMConfig(Instance i) {
      super(tm.valueOf(i.stringValue(0)), budget.valueOf(i.stringValue(1)), (int) i.value(2), (int) i.value(3));
      this.dataSet = i.dataset();
      if (this.dataSet == null)
         throw new RuntimeException("Cannot be null");
   }


   public Instance toInstance() {
      final Instance r = new DenseInstance(dataSet.numAttributes());
      r.setDataset(this.dataSet); //First set the data set, then manipulate
      r.setValue(0, this.get_tm().toString());
      r.setValue(1, this.get_budget().toString());
      r.setValue(2, get_threads());
      r.setValue(3, getInit_budget());
      return r;
   }


   public WekaTMConfig(TMConfig c, Instances d) {
      super(c.get_tm(), c.get_budget(), c.get_threads(), c.getInit_budget());
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
