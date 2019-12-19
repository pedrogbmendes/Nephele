package weka.giraphPlus;

import lynceus.TrainingSet;
import lynceus.giraph.GiraphPlusConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class GiraphPlusConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<GiraphPlusConfig, WekaModelSample>{

	/* class constructors */
	GiraphPlusConfigWekaTrainingSet(String arff){
		super(arff);
	}
	
	private GiraphPlusConfigWekaTrainingSet(Instances i) {
		super(i);
	}

	/* superclass abstract methods to be implemented */
	@Override
	public TrainingSet<GiraphPlusConfig, WekaModelSample> clone() {
		return new GiraphPlusConfigWekaTrainingSet(this.set);
	}

	@Override
	protected GiraphPlusConfig buildConfigFromInstance(Instance i) {
		return new WekaGiraphPlusConfig(i);
	}

}
