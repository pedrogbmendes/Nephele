package weka.gauss;

import static weka.classifiers.functions.GaussianProcesses.FILTER_NONE;

import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

import lynceus.Lynceus;
import lynceus.Pair;
import weka.Matern32Kernel;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.GaussianProcesses;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.tuning.ModelParams;




/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 2019-02-12
 */
public class LynGaussianProcess{

   /*
   NOTE: with very few points, GDdoes not converge to the same
   as brute force
    */

   private final static boolean debug_lml = false;

   private Instances instances;
   private final static boolean normalize = false;
   private ModelParams params;
   private Instances testInstances;

   private final static double noise = Math.sqrt(1e-10);

   public ModelParams getParams() {
      return params;
   }

   public LynGaussianProcess(Instances instances, ModelParams params) {
      this.instances = instances;
      this.params = params;
   }
   
   
   public LynGaussianProcess(Instances instances, ModelParams params, Instances allInstances) {
	      this.instances = instances;
	      this.params = params;
	      this.testInstances = allInstances;
   }
   

   public LynGaussianProcess(Instances instances) {
      this.instances = instances;
      this.params = null;
   }

   private Pair<Double, Double> bruteForceThetaSigma() throws Exception {
      final double step = 0.05;
      final double logMax = 5;
      final double logMin = -5;

      double max_lml = -Double.MAX_VALUE;
      double opt_l = 1e-5;
      double opt_s = 1e-5;
      double lml;

      double l2, s2;
      for (double _l = logMin; _l <= logMax; _l += step) {
         for (double _s = logMin; _s <= logMax; _s += step) {
            l2 = Math.exp(_l);
            s2 = Math.exp(_s);


            Matern32Kernel m = new Matern32Kernel(l2, s2);
            GaussianProcesses gp = new CustomGP();
            gp.setKernel(m);
            if (!normalize) {
               gp.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
               gp.setNoise(noise);
            }
            lml = ((CustomGP) gp).LML(instances);

            if (lml > max_lml) {
               max_lml = lml;
               opt_l = l2;
               opt_s = s2;
               if (debug_lml) {
                  System.out.println("New opt <" + opt_l + ", " + opt_s + "> with lml " + max_lml);
               }
            }
         }
      }
      //System.out.println("Opt-theta (BF) " + opt_l);

      return new Pair<>(opt_l, opt_s);
   }


   private Pair<Double, Double> bruteForceTheta() throws Exception {
      final double step = 0.005;
      final double logMax = 10;
      final double logMin = -10;

      double max_lml = -Double.MAX_VALUE;
      double opt_l = 1e-5;
      double lml;

      double l2;
      for (double _l = logMin; _l <= logMax; _l += step) {
         l2 = Math.exp(_l);
         Matern32Kernel m = new Matern32Kernel(l2, 1.D);
         GaussianProcesses gp = new CustomGP();
         gp.setKernel(m);
         if (!normalize) {
            gp.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
            gp.setNoise(noise);
         }
         lml = ((CustomGP) gp).LML(instances);

         if (lml > max_lml) {
            max_lml = lml;
            opt_l = l2;
            if (debug_lml) {
               System.out.println("New opt " + opt_l + " with lml " + max_lml);
            }
         }
      }
      //System.out.println("Opt-theta (BF) " + opt_l);

      return new Pair<>(opt_l, 1.0D);
   }


   private static double lml(double theta, double sigmaSquared, Instances instances) throws Exception {
      Matern32Kernel mup = new Matern32Kernel(theta, sigmaSquared);
      GaussianProcesses gpup = new CustomGP();
      gpup.setKernel(mup);
      if (!normalize) {
         gpup.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
         gpup.setNoise(noise);
      }
      return ((CustomGP) gpup).LML(instances);

   }


   private Pair<Double, Double> optLogThetaStartingFromLog(double start_log_theta, double step, double upper_theta, double lower_theta) throws Exception {

      if (debug_lml) {
         System.out.println("Starting fom " + start_log_theta + " with lower " + lower_theta + " upper " + upper_theta);
      }
      double opt_lml;
      double opt_log_theta;

      double up_log_theta = start_log_theta + step;
      double down_log_theta = start_log_theta - step;

      double lml_curr = lml(Math.exp(start_log_theta), 1.D, instances);
      double lml_up = lml(Math.exp(up_log_theta), 1.D, instances);
      double lml_down = lml(Math.exp(down_log_theta), 1.D, instances);


      if (lml_curr > lml_up && lml_curr > lml_down) {
         if (debug_lml) {
            System.out.println("Starting point is local opt");
         }
         return new Pair<>(start_log_theta, lml_curr);
      }


      int gradient;
      if (lml_up > lml_down) {
         gradient = 1;
         opt_log_theta = up_log_theta;
         opt_lml = lml_up;
         if (debug_lml) {
            System.out.println("Up gradient: starting " + lml_curr + " up " + lml_up);
         }
      } else {
         gradient = -1;
         opt_log_theta = down_log_theta;
         opt_lml = lml_down;
         if (debug_lml) {
            System.out.println("Down gradient: starting " + lml_curr + " down " + lml_down);
         }
      }


      double next_log_theta, next_lml;
      while (true) {
         next_log_theta = opt_log_theta + gradient * step;
         //System.out.println("Opt (curr) "+Math.exp(opt_log_theta)+" next "+ Math.exp(next_log_theta));

         if (next_log_theta < lower_theta || next_log_theta > upper_theta) {
            if (debug_lml) {
               System.out.println("Stopping at " + Math.exp(opt_log_theta) + " b/c I've reached the border");
            }
            return new Pair<>(opt_log_theta, opt_lml);
         }

         next_lml = lml(Math.exp(next_log_theta), 1.D, instances);
         if (next_lml < opt_lml) {
            if (debug_lml) {
               System.out.println("Stopping at " + Math.exp(opt_log_theta) + " b/c I've reached the optimum. Cur " + opt_lml + " next " + next_lml);
            }
            return new Pair<>(opt_log_theta, opt_lml);
         }
         opt_lml = next_lml;
         opt_log_theta = next_log_theta;
      }
   }

   
   private Pair<Double, Double> GDTheta() throws Exception {
      final double step = 0.005;


      final double[] startingPoints = new double[]{-2.5, 0, 2.5};
      final double[][] limits = new double[3][2];
      limits[0][0] = -5;
      limits[0][1] = 0;
      limits[1][0] = -2.5;
      limits[1][1] = 2.5;
      limits[2][0] = 0;
      limits[2][1] = 5;

      double optTheta = -1, optLML = -Double.MAX_VALUE;
      double t, lml;
      Pair<Double, Double> res;
      int index = 0;
      double upper, lower;

      for (double s : startingPoints) {
         upper = limits[index][1];
         lower = limits[index++][0];
         res = optLogThetaStartingFromLog(s, step, upper, lower);
         lml = res.getSnd();
         if (debug_lml) {
            System.out.println("> Curr opt " + optLML + " new value " + lml);
         }
         if (lml > optLML) {
            optLML = lml;
            optTheta = res.getFst();
         }
      }
      optTheta = Math.exp(optTheta);
      //System.out.println("Opt-theta (GD) " + optTheta);

      return new Pair<>(optTheta, 1.0D);
   }


   private Pair<Double, Double> MarginalizationGP_MCMC(long seed) throws Exception {
		   
	   //optimize to hyperpatameters
	   int nIterations = 200;
	   //ArrayList<Pair<Double,Double>> chain = new ArrayList<Pair<Double,Double>>();
	  
	   Random random = new Random(seed);
	   
	   //initial state
	   NormalDistribution lengthCurr = new NormalDistribution(0.5,1);
	   NormalDistribution sigmaCurr = new NormalDistribution(1,1);
	      
	   Matern32Kernel kernelCurr = new Matern32Kernel(lengthCurr.getMean()*lengthCurr.getMean(), sigmaCurr.getMean()* sigmaCurr.getMean());
	   GaussianProcesses gpTestCurr = new CustomGP();
	   gpTestCurr.setKernel(kernelCurr);
	   gpTestCurr.buildClassifier(this.instances);
	   
	   double LikelihoodCurr = 1; //initiaze to 1 because the it will multiply
	   
	   for(Instance ii : this.testInstances) {
		   double mean_x_f = gpTestCurr.classifyInstance(ii);
		   double std_x_f = gpTestCurr.getStandardDeviation(ii);
		   NormalDistribution normal_x_f = new NormalDistribution(mean_x_f, std_x_f);
		   LikelihoodCurr *= normal_x_f.probability(mean_x_f);;			   
	   }
	   
	   double p_theta_curr = lengthCurr.probability(lengthCurr.getMean()) * sigmaCurr.probability(sigmaCurr.getMean());
	   double q_theta_curr = 0 ;
	   
	   
	   for(int i=0; i< nIterations; i++) {
		   //System.out.println("Lenght= " + lengthCurr.getMean() + "; sigma= " + sigmaCurr.getMean());
		   //new sample of the theta
		   NormalDistribution lengthNew = new NormalDistribution(lengthCurr.sample(),1);
		   NormalDistribution sigmaNew = new NormalDistribution(sigmaCurr.sample(),1);   
		   while(lengthNew.getMean() < 0) lengthNew = new NormalDistribution(lengthCurr.sample(),1);
		   while(sigmaNew.getMean() < 0 ) sigmaNew = new NormalDistribution(sigmaCurr.sample(),1);
		   
		   ////			arguments of kernel has to be squared???
		   Matern32Kernel kernelNew = new Matern32Kernel(lengthNew.getMean()*lengthNew.getMean(), sigmaNew.getMean()* sigmaNew.getMean());
		   GaussianProcesses gpTestNew = new CustomGP();
		   gpTestNew.setKernel(kernelNew);
		   gpTestNew.buildClassifier(this.instances);	   
		   
		   double LikelihoodNew = 1; //initialize to 1 because the it will multiply
		   
		   for(Instance ii : this.testInstances) {
			   double mean_x_f = gpTestNew.classifyInstance(ii);
			   double std_x_f = gpTestNew.getStandardDeviation(ii);
			   NormalDistribution normal_x_f = new NormalDistribution(mean_x_f, std_x_f);
			   LikelihoodNew *= normal_x_f.density(mean_x_f);;			   
		   }
		  
		   double p_theta_new = lengthNew.density(lengthNew.getMean()) * sigmaNew.density(sigmaNew.getMean());
		   
		   if(i==0) q_theta_curr = lengthCurr.density(lengthNew.getMean())* sigmaCurr.density(sigmaNew.getMean());
		   double q_theta_new = lengthNew.density(lengthCurr.getMean())*sigmaNew.density(sigmaCurr.getMean());
		   
		   
		   double r = (LikelihoodNew *p_theta_new*q_theta_new) /(LikelihoodCurr*p_theta_curr*q_theta_curr);
		   
		   double u = random.nextDouble();
		   
		   if(u<r) {
			   //update for the the state
			   lengthCurr = lengthNew;
			   sigmaCurr = sigmaNew;
			   
			   p_theta_curr = p_theta_new;
			   q_theta_curr = q_theta_new;
			   LikelihoodCurr = LikelihoodNew;
			   
		   }else {
			   //keep the currunt state 
		   }		   
	   	} 
	   	//System.out.println("Lenght= " + lengthCurr.getMean() + "; sigma= " + sigmaCurr.getMean());
		return new Pair<>(lengthCurr.getMean(), sigmaCurr.getMean());
   }
		   
		   
		   
		   
		   
//				   
//		   //Matrix k_y = ((CustomGP) gpTest).getCov_yMatrix(this.instances, this.allPossibleInstances);
//		   
//		   int nPoints = 50;
//		   //List<Matrix> listMatrix = new ArrayList<Matrix>(50);
//		   
//		   int n = this.instances.numInstances();
//		   int row = 0, column = 0;
//		   Matrix k = new DenseMatrix(n+nPoints, n+nPoints);
//		   Matrix k_train = new DenseMatrix(n, n);
//		   
//		   for(Instance ki : this.instances) {
//			   for(Instance kj : this.instances) {
//				   k.set(row, column, kernel.evaluate(ki, kj));
//				   k_train.set(row, column, kernel.evaluate(ki, kj));
//				   column ++;
//			   }
//			   row ++;
//			   column = 0;
//		   }
//		   
//		   for(int j=0; j<nPoints; j++) {
//			   Instance ii = this.allPossibleInstances.get(random.nextInt(this.allPossibleInstances.numInstances()));
//			   column = 0;
//			   row = 0;
//			   for(Instance ki : this.instances) {
//				   double kernelValue = kernel.evaluate(ki, ii);
//				   k.set(n+j, column, kernelValue);
//				   k.set(row, n+j, kernelValue);
//				   column++;
//				   row++;
//				  
//			   }
//			   k.set(n, n, kernel.evaluate(ii, ii));
//			   listMatrix.add(k);			   
//		   }
//		   
//		   
//		   
//		   
//		   double marginalLikelihood = ((CustomGP) gpTest).LML(this.instances);
//		   
//		   double u = random.nextDouble();
//		   
//		   //if u < 
//		   
//		   
//	   }
	      
  
   
   
   public Classifier build(long seed) throws Exception {


      if (params != null) {
         if (Lynceus.retrain_in_depth) {
            throw new RuntimeException("Params are not null but retrain in depth is true");
         }
         GaussianProcesses gp = new CustomGP();
         Matern32Kernel m = new Matern32Kernel(params.getTheta(), params.getSigma());
         gp.setKernel(m);
         if (!normalize) {
            gp.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
            gp.setNoise(Math.sqrt(1e-10));
         }
         gp.buildClassifier(this.instances);
         System.out.println("LynGP built with input param " + params);
         return gp;
      }

      //Params is null

      //We go from 1e-5 to 1e5 for all params
      //In log this is from -5 to 5
      //We sweep at a granularity of 0.1 in log scale
      //We need the variance and the scale
      
      /* Choose Theta */
      //Pair<Double, Double> opt = bruteForceTheta();
      //Pair<Double, Double> opt = GDTheta();

      Pair<Double, Double> opt = MarginalizationGP_MCMC(seed);
      
      //Pair<Double, Double> opt = bruteForceThetaSigma();

      final Matern32Kernel m32 = new Matern32Kernel(opt.getFst(), opt.getSnd());
      final GaussianProcesses gpp = new CustomGP();
      gpp.setKernel(m32);
      if (!normalize) {
         gpp.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
         gpp.setNoise(noise);
      }
      gpp.buildClassifier(this.instances);
      this.params = new ModelParams(opt.getFst(), opt.getSnd());

      /*if (WekaGaussianProcess.debug_gp) {
         cv(opt_l);
      }
      */
      System.out.println("LynGP built with found param " + opt);
      return gpp;
   }


	
	public Classifier build() throws Exception {
	
	
	   if (params != null) {
	      if (Lynceus.retrain_in_depth) {
	         throw new RuntimeException("Params are not null but retrain in depth is true");
	      }
	      GaussianProcesses gp = new CustomGP();
	      Matern32Kernel m = new Matern32Kernel(params.getTheta(), params.getSigma());
	      gp.setKernel(m);
	      if (!normalize) {
	         gp.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
	         gp.setNoise(Math.sqrt(1e-10));
	      }
	      gp.buildClassifier(this.instances);
	      System.out.println("LynGP built with input param " + params);
	      return gp;
	   }
	
	   //Params is null
	
	   //We go from 1e-5 to 1e5 for all params
	   //In log this is from -5 to 5
	   //We sweep at a granularity of 0.1 in log scale
	   //We need the variance and the scale
	   
	   /* Choose Theta */
	   //Pair<Double, Double> opt = bruteForceTheta();
	   //Pair<Double, Double> opt = GDTheta();
	
	   Pair<Double, Double> opt = MarginalizationGP_MCMC(1);
	   
	   //Pair<Double, Double> opt = bruteForceThetaSigma();
	
	   final Matern32Kernel m32 = new Matern32Kernel(opt.getFst(), opt.getSnd());
	   final GaussianProcesses gpp = new CustomGP();
	   gpp.setKernel(m32);
	   if (!normalize) {
	      gpp.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
	      gpp.setNoise(noise);
	   }
	   gpp.buildClassifier(this.instances);
	   this.params = new ModelParams(opt.getFst(), opt.getSnd());
	
	   /*if (WekaGaussianProcess.debug_gp) {
	      cv(opt_l);
	   }
	   */
	   System.out.println("LynGP built with found param " + opt);
	   return gpp;
	}
   
   private void cv(double opt_l) throws Exception {
      System.out.println("System params l = " + opt_l);
      Evaluation evaluation = new Evaluation(this.instances);
      final Matern32Kernel _m32 = new Matern32Kernel(opt_l);
      final GaussianProcesses _gpp = new CustomGP();
      if (!normalize) {
         _gpp.setFilterType(new SelectedTag(FILTER_NONE, GaussianProcesses.TAGS_FILTER));
         _gpp.setNoise(noise);
      }
      _gpp.setKernel(_m32);
      int folds = Math.min(instances.size(), 10);
      Random r = new Random(1987234);
      evaluation.crossValidateModel(_gpp, this.instances, folds, r);
      System.out.println(evaluation.toSummaryString());
   }



}

