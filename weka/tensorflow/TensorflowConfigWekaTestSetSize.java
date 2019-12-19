package weka.tensorflow;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.tensorflow.TensorflowConfigSize;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class TensorflowConfigWekaTestSetSize extends AbstractConfigWekaTestSet<TensorflowConfigSize, WekaModelSample>{


	/* class constructors */
	public TensorflowConfigWekaTestSetSize(Instances set, String arff) {
		super(set, arff);
	}

	public TensorflowConfigWekaTestSetSize(String arff){
		super(arff);
	}
	
	/* superclass abstract methods to be implemented */
	@Override
	public TestSet<TensorflowConfigSize, WekaModelSample> clone() {
		return new TensorflowConfigWekaTestSetSize(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(TensorflowConfigSize c, Instances i) {
		return new WekaTensorflowConfigSize(c, i);
	}

	@Override
   public WekaTensorflowConfigSize buildFromInstance(Instance i) {
		return new WekaTensorflowConfigSize(i);
	}
	
}
