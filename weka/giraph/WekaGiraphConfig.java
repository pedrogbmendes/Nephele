package weka.giraph;

import java.util.ArrayList;

import lynceus.Pair;
import lynceus.WekaConfiguration;
import lynceus.giraph.GiraphConfig;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class WekaGiraphConfig  extends GiraphConfig implements WekaConfiguration{

	/* class attributes */
	private Instances dataset;
	
	
	/* class constructors */
	public WekaGiraphConfig( int vm_type, int vm_size, int nr_workers, int graph, int wkld, Instances d) {
		super(vm_type, vm_size, nr_workers, graph, wkld);
		dataset = d;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaGiraphConfig] Cannot be null");
	}
	
	public WekaGiraphConfig(Instance i){	// graph is always 0
		super((int) i.value(0), (int) i.value(1), (int) i.value(2), 0, WekaGiraphConfigFactory.wkld);
		this.dataset = i.dataset();
	    if (this.dataset == null)
	    	throw new RuntimeException("[WekaGiraphConfig] Cannot be null");
	}

	public WekaGiraphConfig(GiraphConfig c, Instances dataset) {
		super(c.getVm_type(), c.getVm_size(), c.getNr_workers(), c.getGraph(), c.getWkld());
		this.dataset = dataset;
		if (this.dataset == null)
	    	throw new RuntimeException("[WekaGiraphConfig] Cannot be null");
	}

	
	/* interface methods to be implemented */
	@Override
	public Instance toInstance() {
//        double ret[] = new double[dataset.numAttributes()];   //leave room for the target attribute as well
// 	   	ret[0] = (double) this.getVm_flavor();
// 	   	ret[1] = this.getNr_workers();
// 	   	ret[2] = this.getGraph();
// 	   	ret[3] = this.getWkld();
// 	   	Instance rr = new DenseInstance(1.0, ret);
// 	   	rr.setDataset(this.dataset);
// 	   	return rr;
		
		Instance rr = new DenseInstance(numAttributes() + 1);  // + 1 for the target attribute
	    rr.setDataset(this.dataset); //first set dataset, otherwise following statements fail with "no dataset associated" exception
	    rr.setValue(0, (double) this.getVm_type());
	    rr.setValue(1, (double) this.getVm_size());
	    rr.setValue(2, (double) this.getNr_workers());
//	    rr.setValue(3, (double) this.getGraph());
//	    rr.setValue(4, (double) this.getWkld());
	    return rr;
		
	}
	
	@Override
	public WekaGiraphConfig clone() {
	   return  new WekaGiraphConfig(this,this.dataset);   //NB: this is *not* copying the instances
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
