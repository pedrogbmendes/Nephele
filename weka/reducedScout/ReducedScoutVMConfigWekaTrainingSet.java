package weka.reducedScout;

import lynceus.TrainingSet;
import lynceus.scout.ReducedScoutVMConfig;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;


public class ReducedScoutVMConfigWekaTrainingSet extends AbstractConfigWekaTrainingSet<ReducedScoutVMConfig, WekaModelSample> {

	/* class constructor */
	protected ReducedScoutVMConfigWekaTrainingSet(Instances i) {
		super(i);
	}
	
	public ReducedScoutVMConfigWekaTrainingSet(String arff) {
	    super(arff);
	}

	/* abstract methods to be implemented */
	@Override
	public TrainingSet<ReducedScoutVMConfig, WekaModelSample> clone() {
		return new ReducedScoutVMConfigWekaTrainingSet(this.set);
	}

	@Override
	protected ReducedScoutVMConfig buildConfigFromInstance(Instance i) {
		return new ReducedWekaScoutVMConfig(i);
	}

}
