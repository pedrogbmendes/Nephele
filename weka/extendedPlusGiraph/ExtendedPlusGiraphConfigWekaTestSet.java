package weka.extendedPlusGiraph;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.giraph.ExtendedPlusGiraphConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;


public class ExtendedPlusGiraphConfigWekaTestSet extends AbstractConfigWekaTestSet<ExtendedPlusGiraphConfig, WekaModelSample>{

	/* class constructors */
	public ExtendedPlusGiraphConfigWekaTestSet(Instances set, String arff) {
		super(set, arff);
	}
	
	public ExtendedPlusGiraphConfigWekaTestSet(String arff) {
		super(arff);
	}

	@Override
	public TestSet<ExtendedPlusGiraphConfig, WekaModelSample> clone() {
		return new ExtendedPlusGiraphConfigWekaTestSet(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(ExtendedPlusGiraphConfig c, Instances i) {
		return new ExtendedPlusWekaGiraphConfig(c, i);
	}

	@Override
	public ExtendedPlusGiraphConfig buildFromInstance(Instance i) {
		return new ExtendedPlusWekaGiraphConfig(i);
	}
}