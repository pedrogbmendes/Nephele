package lynceus.tm;

import java.util.ArrayList;
import java.util.List;

import lynceus.Configuration;
import lynceus.LHS;
import lynceus.tm.TMConfig.budget;
import lynceus.tm.TMConfig.tm;
import weka.tm.WekaTMConfig;

public class TMLHS <C extends Configuration> extends LHS{
	
	/* class attributes */
	private ArrayList<double[]> dims;
	
	/* class constructor */
	public TMLHS(int samples) {
		super(samples);
	}

	/* superclass abstract methods to be implemented */
	
	/**
	 * Initialise the dimension arrays
	 * @return a list with the initialised arrays
	 */
	@Override
	protected ArrayList<double[]> initDims() {
		this.dims = new ArrayList<double[]>();
		
		double[] tm = {0, 1, 2, 3, 4};
		double[] budget = {0, 1, 2};	// only for HTM
		double[] threadsA = {1, 2, 3, 4, 5, 6, 7, 8};		// machine A STMs and TSX
		double[] threadsB = {1, 2, 4, 6, 8, 16, 32, 48};	// machine B STMs
		double[] initBudget = {2, 4, 10, 16, 20};	// only for HTM
		
		this.dims.add(threadsA);
		this.dims.add(threadsB);
		this.dims.add(tm);
		this.dims.add(initBudget);
		this.dims.add(budget);

		//System.out.println("dims: " + dims);
		return this.dims;
	}

	@Override
	protected Configuration newSample(int index) {
		
		double[] dim = new double[8];
		int auxIndex = -1;	// auxiliar to maintain the initial value of index when 
							// we need to do round-robin
		
		tm _tm = null;
		budget _budget = TMConfig.budget.ZERO;	// HTM Capacity Abort Policy
		int _threads = 0;
		int init_budget = 0;	// HTM Abort Budget
		
		
		/* for each dimension array of a configuration, pick the value
		 * corresponding to the given index. If the array is smaller, do it
		 * round-robin */
		int i = 0;
		while (i < this.dims.size()) {
			if (this.dims.get(i).length == 1) {
				auxIndex = 0;
			} else if (index >= this.dims.get(i).length) {
				auxIndex = index - this.dims.get(i).length;
				while (auxIndex >= this.dims.get(i).length) {
					auxIndex = auxIndex - this.dims.get(i).length;
				}
			} else {
				auxIndex = index;
			}
			
			dim = this.dims.get(i);
			
			
			if (i == 0) {	// selecting threadsA
				_threads = (int) dim[auxIndex];
			} else if (i == 1) {	// selecting threadsB
			//	_threads = (int) dim[auxIndex];
			} else if (i == 2) {	// selecting tm
				if (dim[auxIndex] == 0) {
					_tm = TMConfig.tm.HTM;
				} else if (dim[auxIndex] == 1) {
					_tm = TMConfig.tm.TINYSTM;
				} else if (dim[auxIndex] == 2) {
					_tm = TMConfig.tm.SWISSTM;
				} else if (dim[auxIndex] == 3) {
					_tm = TMConfig.tm.NOREC;
				} else {
					_tm = TMConfig.tm.TL2;
				}
			} else if (i == 3) {	// selecting initBudget if tm = HTM
				if (_tm == TMConfig.tm.HTM) {
					init_budget = (int) dim[auxIndex];
				}
			} else {	// selecting budget if tm = HTM
				if (_tm == TMConfig.tm.HTM) {
					if (dim[auxIndex] == 0) {
						_budget = TMConfig.budget.DECREASE;
					} else if (dim[auxIndex] == 1) {
						_budget = TMConfig.budget.HALF;
					} else {
						_budget = TMConfig.budget.ZERO;
					}
				}
			}
			i++;
		}
		
		TMConfig newConfig = new TMConfig(_tm, _budget, _threads, init_budget);
		
		return new WekaTMConfig(newConfig, super.dataset);
	}


	@Override
	protected void checkSamples(List samples, String type, int seed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ArrayList<Double> dimensions(Configuration config) {
		ArrayList<Double> dimensionsArray = new ArrayList<Double>();
		
		int _tm;
	    int _budget;	// HTM Capacity Abort Policy
		int _threads;
		int init_budget;	// HTM Abort Budget
		
		_tm = (int) config.at(0);
		_budget = (int) config.at(1);
		_threads = (int) config.at(2);
		init_budget = (int) config.at(3);
		
		if (_tm == 0) {
			dimensionsArray.add(1.0);
		} else if (_tm == 1) {
			dimensionsArray.add(2.0);
		} else if (_tm == 2) {
			dimensionsArray.add(3.0);
		} else if (_tm == 3) {
			dimensionsArray.add(4.0);
		} else {
			dimensionsArray.add(5.0);
		}
		
		if (_budget == 0) {
			dimensionsArray.add(1.0);
		} else if (_budget == 1) {
			dimensionsArray.add(2.0);
		} else {
			dimensionsArray.add(3.0);
		}
		
		// {1, 2, 3, 4, 5, 6, 7, 8};
		if (_threads == 1) {
			dimensionsArray.add(1.0);
		} else if (_threads == 2) {
			dimensionsArray.add(2.0);
		} else if (_threads == 3) {
			dimensionsArray.add(3.0);
		} else if (_threads == 4) {
			dimensionsArray.add(4.0);
		} else if (_threads == 5) {
			dimensionsArray.add(5.0);
		} else if (_threads == 6) {
			dimensionsArray.add(6.0);
		} else if (_threads == 7) {
			dimensionsArray.add(7.0);
		} else {
			dimensionsArray.add(8.0);
		}
		
		// {2, 4, 10, 16, 20};
		if (init_budget == 0) {
			dimensionsArray.add(1.0);
		} else if (init_budget == 2) {
			dimensionsArray.add(2.0);
		} else if (init_budget == 4) {
			dimensionsArray.add(3.0);
		} else if (init_budget == 10) {
			dimensionsArray.add(4.0);
		} else if (init_budget == 16) {
			dimensionsArray.add(5.0);
		} else {
			dimensionsArray.add(6.0);
		}
		
		
		return dimensionsArray;
	}

}
