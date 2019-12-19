package weka.giraph;

import lynceus.TrainingSet;
import lynceus.giraph.GiraphConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class GiraphConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<GiraphConfig, WekaModelSample>{

	/* class constructors */
	GiraphConfigWekaTrainingSet(String arff){
		super(arff);
	}
	
	private GiraphConfigWekaTrainingSet(Instances i) {
		super(i);
	}

	/* superclass abstract methods to be implemented */
	@Override
	public TrainingSet<GiraphConfig, WekaModelSample> clone() {
		return new GiraphConfigWekaTrainingSet(this.set);
	}

	@Override
	protected GiraphConfig buildConfigFromInstance(Instance i) {
		return new WekaGiraphConfig(i);
	}

}
