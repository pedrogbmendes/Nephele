package weka.giraphPlus;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.giraph.GiraphPlusConfig;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaGiraphPlusConfig extends GiraphPlusConfig implements WekaConfiguration{

	/* class attributes */
	private Instances dataset;

	/* class constructors */
	public WekaGiraphPlusConfig( int vm_type, int vm_size, int nr_workers, double configCostPerMin, int graph, int wkld, Instances d) {
		super(vm_type, vm_size, nr_workers, configCostPerMin, graph, wkld);
		dataset = d;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaGiraphPlusConfig] Cannot be null");
	}
	
	public WekaGiraphPlusConfig(Instance i){	// graph is always 0
		super((int) i.value(0), (int) i.value(1), (int) i.value(2), (double) i.value(3), 0, WekaGiraphPlusConfigFactory.wkld);
		this.dataset = i.dataset();
	    if (this.dataset == null)
	    	throw new RuntimeException("[WekaGiraphPlusConfig] Cannot be null");
	}

	public WekaGiraphPlusConfig(GiraphPlusConfig c, Instances dataset) {
		super(c.getVm_type(), c.getVm_size(), c.getNr_workers(), c.getVmCostPerMin(), c.getGraph(), c.getWkld());
		this.dataset = dataset;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaGiraphPlusConfig] Cannot be null");
	}

	
	/* interface methods to be implemented */
	@Override
	public Instance toInstance() {
		Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
	    rr.setDataset(this.dataset); //first set dataset, otherwise following statements fail with "no dataset associated" exception
	    rr.setValue(0, (double) this.getVm_type());
	    rr.setValue(1, (double) this.getVm_size());
	    rr.setValue(2, (double) this.getNr_workers());
	    rr.setValue(3, (double) this.getVmCostPerMin());
//	    rr.setValue(4, (double) this.getWkld());
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
