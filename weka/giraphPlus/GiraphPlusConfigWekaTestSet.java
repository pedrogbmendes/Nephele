package weka.giraphPlus;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.giraph.GiraphPlusConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class GiraphPlusConfigWekaTestSet extends AbstractConfigWekaTestSet<GiraphPlusConfig, WekaModelSample>{

	/* class constructors */
	public GiraphPlusConfigWekaTestSet(Instances set, String arff) {
		super(set, arff);
	}

	public GiraphPlusConfigWekaTestSet(String arff){
		super(arff);
	}

	/* superclass abstract methods to be implemented */
	@Override
	public TestSet<GiraphPlusConfig, WekaModelSample> clone() {
		return new GiraphPlusConfigWekaTestSet(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(GiraphPlusConfig c, Instances i) {
		return new WekaGiraphPlusConfig(c, i);
	}

	@Override
	public GiraphPlusConfig buildFromInstance(Instance i) {
		return new WekaGiraphPlusConfig(i);
	}
}
