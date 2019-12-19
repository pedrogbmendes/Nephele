package weka.extendedGiraph;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.giraph.ExtendedGiraphConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class ExtendedGiraphConfigWekaTestSet extends AbstractConfigWekaTestSet<ExtendedGiraphConfig, WekaModelSample>{

	/* class constructors */
	public ExtendedGiraphConfigWekaTestSet(Instances set, String arff) {
		super(set, arff);
	}
	
	public ExtendedGiraphConfigWekaTestSet(String arff) {
		super(arff);
	}

	@Override
	public TestSet<ExtendedGiraphConfig, WekaModelSample> clone() {
		return new ExtendedGiraphConfigWekaTestSet(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(ExtendedGiraphConfig c, Instances i) {
		return new ExtendedWekaGiraphConfig(c, i);
	}

	@Override
	public ExtendedGiraphConfig buildFromInstance(Instance i) {
		return new ExtendedWekaGiraphConfig(i);
	}

}
