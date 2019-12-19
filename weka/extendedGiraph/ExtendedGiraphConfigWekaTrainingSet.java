package weka.extendedGiraph;

import lynceus.TrainingSet;
import lynceus.giraph.ExtendedGiraphConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;


public class ExtendedGiraphConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<ExtendedGiraphConfig, WekaModelSample> {
	
	/* class constructors */
	ExtendedGiraphConfigWekaTrainingSet(String arff){
		super(arff);
	}
	
	protected ExtendedGiraphConfigWekaTrainingSet(Instances i) {
		super(i);
	}

	@Override
	public TrainingSet<ExtendedGiraphConfig, WekaModelSample> clone() {
		return new ExtendedGiraphConfigWekaTrainingSet(this.set);
	}

	@Override
	protected ExtendedGiraphConfig buildConfigFromInstance(Instance i) {
		return new ExtendedWekaGiraphConfig(i);
	}

}
