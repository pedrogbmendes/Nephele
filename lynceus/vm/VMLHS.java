package lynceus.vm;

import java.util.ArrayList;
import java.util.List;

import lynceus.Configuration;
import lynceus.LHS;

public class VMLHS <C extends Configuration> extends LHS{

	/* class constructor */
	public VMLHS(int samples) {
		super(samples);
	}

	/* superclass abstract methods to be implemented */
	@Override
	protected ArrayList initDims() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Configuration newSample(int index) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	protected void checkSamples(List samples, String type, int seed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ArrayList<Double> dimensions(Configuration config) {
		// TODO Auto-generated method stub
		return null;
	}

}
