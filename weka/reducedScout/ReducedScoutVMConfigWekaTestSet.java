package weka.reducedScout;

import lynceus.TestSet;
import lynceus.WekaConfiguration;
import lynceus.scout.ReducedScoutVMConfig;
import weka.AbstractConfigWekaTestSet;
import weka.WekaModelSample;
import weka.core.Instance;
import weka.core.Instances;

public class ReducedScoutVMConfigWekaTestSet extends AbstractConfigWekaTestSet<ReducedScoutVMConfig, WekaModelSample> {

	/* class constructor */
	public ReducedScoutVMConfigWekaTestSet(Instances set, String arff) {
		super(set, arff);
	}
	
	public ReducedScoutVMConfigWekaTestSet(String arff) {
        super(arff);
     }

	/* abstract methods to be implemented */
	@Override
	public TestSet<ReducedScoutVMConfig, WekaModelSample> clone() {
		return new ReducedScoutVMConfigWekaTestSet(new Instances(this.set), this.arff);
	}

	@Override
	protected WekaConfiguration buildFromConfigAndSet(ReducedScoutVMConfig c, Instances i) {
		return new ReducedWekaScoutVMConfig(c, i);
	}

	@Override
	public ReducedScoutVMConfig buildFromInstance(Instance i) {
		return new ReducedWekaScoutVMConfig(i);
	}

}
