package weka.extendedPlusGiraph;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.giraph.ExtendedPlusGiraphConfig;
import lynceus.giraph.GiraphDirectory;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class ExtendedPlusWekaGiraphConfig extends ExtendedPlusGiraphConfig implements WekaConfiguration {

	/* class attributes */
	private Instances dataset;

	/* class constructors */
	public ExtendedPlusWekaGiraphConfig(GiraphDirectory.GiraphVMFlavor flavor, int nr_workers, double mem, double cores, double configCostPerMin, int graph, int wkld, Instances d) {
		super(flavor, nr_workers ,mem, cores, configCostPerMin, graph, wkld);
		dataset = d;
		if (this.dataset == null)
	    	throw new RuntimeException("[ExtendedPlusWekaGiraphConfig] Cannot be null");
	}
	
	public ExtendedPlusWekaGiraphConfig(Instance i){	// graph is always 0
		super(GiraphDirectory.vmFlavorFromInt((int)i.value(0)), (int) i.value(1), (double) i.value(2), (double) i.value(3), (double) i.value(4), 0, (int) ExtendedPlusWekaGiraphConfigFactory.wkld);
		this.dataset = i.dataset();
	    if (this.dataset == null)
	    	throw new RuntimeException("[ExtendedPlusWekaGiraphConfig] Cannot be null");
	}

	public ExtendedPlusWekaGiraphConfig(ExtendedPlusGiraphConfig c, Instances dataset) {
		super(c.getVm_flavor(), c.getNr_workers(), c.getTotalMem(), c.getTotalCores(), c.getConfigCostPerTimeUnit(), c.getGraph(), c.getWkld());
		this.dataset = dataset;
		if (this.dataset == null)
	    	throw new RuntimeException("[ExtendedPlusWekaGiraphConfig] Cannot be null");
	}

	/* interface methods to be implemented */
	@Override
	public ExtendedPlusWekaGiraphConfig clone() {
	   return new ExtendedPlusWekaGiraphConfig(this,this.dataset);   //NB: this is *not* copying the instances
	}
	
	@Override
	public Instance toInstance() {
		Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
	    rr.setDataset(this.dataset); //first set dataset, otherwise following statements fail with "no dataset associated" exception
	    rr.setValue(0, (double) GiraphDirectory.vmFlavorToInt(this.getVm_flavor()));
	    rr.setValue(1, (double) this.getNr_workers());
	    rr.setValue(2, (double) this.getTotalMem());
	    rr.setValue(3, (double) this.getTotalCores());
	    rr.setValue(4, (double) this.getConfigCostPerTimeUnit());
	    return rr;
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