package weka.giraph;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.giraph.GiraphConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class GiraphConfigWekaTestSet extends AbstractConfigWekaTestSet<GiraphConfig, WekaModelSample>{

	/* class constructors */
	public GiraphConfigWekaTestSet(Instances set, String arff) {
		super(set, arff);
	}

	public GiraphConfigWekaTestSet(String arff){
		super(arff);
	}
	
	/* superclass abstract methods to be implemented */
	@Override
	public TestSet<GiraphConfig, WekaModelSample> clone() {
		return new GiraphConfigWekaTestSet(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(GiraphConfig c, Instances i) {
		return new WekaGiraphConfig(c, i);
	}

	@Override
   public WekaGiraphConfig buildFromInstance(Instance i) {
		return new WekaGiraphConfig(i);
	}
}
