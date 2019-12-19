package weka.extendedPlusGiraph;

import lynceus.TrainingSet;
import lynceus.giraph.ExtendedPlusGiraphConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class ExtendedPlusGiraphConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<ExtendedPlusGiraphConfig, WekaModelSample> {
	
	/* class constructors */
	ExtendedPlusGiraphConfigWekaTrainingSet(String arff){
		super(arff);
	}
	
	protected ExtendedPlusGiraphConfigWekaTrainingSet(Instances i) {
		super(i);
	}

	@Override
	public TrainingSet<ExtendedPlusGiraphConfig, WekaModelSample> clone() {
		return new ExtendedPlusGiraphConfigWekaTrainingSet(this.set);
	}

	@Override
	protected ExtendedPlusGiraphConfig buildConfigFromInstance(Instance i) {
		return new ExtendedPlusWekaGiraphConfig(i);
	}
}
