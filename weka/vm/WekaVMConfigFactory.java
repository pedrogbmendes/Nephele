package weka.vm;

import java.util.Random;

import lynceus.vm.VMConfig;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public class WekaVMConfigFactory {

   public static VMConfigWekaTestSet buildInitTestSet(String arff) {
      final VMConfigWekaTestSet testSet = new VMConfigWekaTestSet(arff);

      int[] m5_large_instances = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
      int[] m5_xlarge_instances = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
      int[] m5_2xlarge_instances = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
      int[] m5_4xlarge_instances = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};


      for (int i = 1; i <= m5_large_instances.length; i++) {
         VMConfig a = new VMConfig(VMConfig.m5_large, 2, 10, 8, i);
         testSet.addTestSample(a);
      }

      for (int i = 1; i <= m5_xlarge_instances.length; i++) {
         VMConfig a = new VMConfig(VMConfig.m5_xlarge, 4, 15, 16, i);
         testSet.addTestSample(a);
      }

      for (int i = 1; i <= m5_2xlarge_instances.length; i++) {
         VMConfig a = new VMConfig(VMConfig.m5_2xlarge, 8, 31, 32, i);
         testSet.addTestSample(a);
      }

      for (int i = 1; i <= m5_4xlarge_instances.length; i++) {
         VMConfig a = new VMConfig(VMConfig.m5_4xlarge, 16, 61, 64, i);
         testSet.addTestSample(a);
      }

      for (int i = 1; i <= m5_4xlarge_instances.length; i++) {
         VMConfig a = new VMConfig(VMConfig.m5_12xlarge, 48, 173, 192, i);
         testSet.addTestSample(a);
      }
      for (int i = 1; i <= m5_4xlarge_instances.length; i++) {
         VMConfig a = new VMConfig(VMConfig.m5_24xlarge, 96, 345, 384, i);
         testSet.addTestSample(a);
      }

      return testSet;
   }

   public static VMConfig randomFromTestSet(String arff, long seed) {
      VMConfigWekaTestSet s = buildInitTestSet(arff);
      Random r = new Random(seed);
      return s.removeAndReturn(r.nextInt(s.size()));
   }

}
