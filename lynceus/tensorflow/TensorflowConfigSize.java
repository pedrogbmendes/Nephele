package lynceus.tensorflow;

import java.util.Objects;

import lynceus.Configuration;

public class TensorflowConfigSize implements Configuration{

	/* class attributes */
	protected int		nr_ps;	// 1
	protected int 		nr_workers;
	protected double 	learning_rate;
	protected int 		batch_size;	 // 16; 256
	protected int		synchronism; // 0 = async ; 1 = sync
	protected int 		vm_flavor;	 // 0 = t2.small ; 1 = t2.medium ; 2 = t2.xlarge ; 3 = t2.2xlarge
	protected double 	s;	//interval between 0 and 1 corresponding to size_of_data_set/size of large dataset
	
	
	/* class constructor */
	protected TensorflowConfigSize(){
		
	}
	
	public TensorflowConfigSize(int nr_ps, int nr_workers, double lr, int bs, int synchronism, int flavor, double s){
		this.nr_ps = nr_ps;
		this.nr_workers = nr_workers;
		learning_rate = lr;
		batch_size = bs;
		this.synchronism = synchronism;
		vm_flavor = flavor;
		this.s = s;
	}
	
	
	/* getters */
	public int getNr_ps(){
		return nr_ps;
	}
	   
	public int getNr_workers() {
	   return nr_workers;
	}
		
	public double getLearning_rate() {
	   return learning_rate;
	}
		
	public int getBatch_size() {
	   return batch_size;
	}
		
	public int getSynchronism(){
	   return synchronism;
	}
	   
	public int getVm_flavor() {
	   return vm_flavor;
	}
	
	public double getDataset_size() {
		   return s;
	}
   
	/* implement interface methods */
	@Override
   public int numAttributes() {
      return 7;
   }
	
   @Override
   public Object at(int i) {
      switch (i) {
      	 case 0:
      		 return nr_ps;
         case 1:
            return nr_workers;
         case 2:
            return learning_rate;
         case 3:
            return batch_size;
         case 4:
        	 return synchronism;
         case 5:
            return vm_flavor;
         case 6:
             return s;
         default:
            throw new RuntimeException("[TensorflowConfigSize] Requested attribute " + i + " but only " + numAttributes() + " available");
      }
   }
   
   @Override
   public Configuration clone() {
      return new TensorflowConfigSize(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor, s);
   }


   
   /* other methods */
   @Override
   public String toString() {
	   return "Config [nr_ps=" + nr_ps + ", nr_workers=" + nr_workers + ", learning_rate=" + learning_rate + ", batch_size=" + batch_size
			   + ", synchronism=" + synchronism + ", vm_flavor=" + vm_flavor + ", dataset_size=" + s + "]";
   }

   	@Override
	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + batch_size;
//		long temp;
//		temp = Double.doubleToLongBits(learning_rate);
//		result = prime * result + (int) (temp ^ (temp >>> 32));
//		result = prime * result + nr_ps;
//		result = prime * result + nr_workers;
//		result = prime * result + synchronism;
//		result = prime * result + vm_flavor;
//		return result;
   		return Objects.hash(nr_ps, nr_workers, learning_rate, batch_size, synchronism, vm_flavor,s);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
//		if (getClass() != obj.getClass())
//			return false;
		TensorflowConfigSize other = (TensorflowConfigSize) obj;
		if (batch_size != other.batch_size)
			return false;
		if (Double.doubleToLongBits(learning_rate) != Double.doubleToLongBits(other.learning_rate))
			return false;
		if (nr_ps != other.nr_ps)
			return false;
		if (nr_workers != other.nr_workers)
			return false;
		if (synchronism != other.synchronism)
			return false;
		if (vm_flavor != other.vm_flavor)
			return false;
		if (s != other.s)
			return false;
		return true;
	}

}
