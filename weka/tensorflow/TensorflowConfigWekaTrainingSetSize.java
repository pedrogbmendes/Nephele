package weka.tensorflow;

import lynceus.TrainingSet;
import lynceus.tensorflow.TensorflowConfigSize;
import weka.AbstractConfigWekaTrainingSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;



public class TensorflowConfigWekaTrainingSetSize extends AbstractConfigWekaTrainingSet<TensorflowConfigSize, WekaModelSample>{

	/* class constructors */
	TensorflowConfigWekaTrainingSetSize(String arff){
		super(arff);
	}
	
	private TensorflowConfigWekaTrainingSetSize(Instances i) {
		super(i);
	}

	/* superclass abstract methods to be implemented */
	@Override
	public TrainingSet<TensorflowConfigSize, WekaModelSample> clone() {
		return new TensorflowConfigWekaTrainingSetSize(this.set);
	}

	@Override
	protected TensorflowConfigSize buildConfigFromInstance(Instance i) {
		return new WekaTensorflowConfigSize(i);
	}

}
