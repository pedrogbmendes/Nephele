package weka.tm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import lynceus.tm.TMConfig;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 20.03.18
 */
public class WekaTMConfigFactory {

   private final static String data = "files/lowerIsBetterDiego.csv";


   public static TMConfigWekaTestSet buildInitTestSet(String arff) {

      try {

         final TMConfigWekaTestSet testSet = new TMConfigWekaTestSet(arff);

         BufferedReader br = new BufferedReader(new FileReader(new File(data)));


         int i = 0;
         /*Get header*/
         String header = br.readLine(); //header
         String[] header_token = header.split(",");
         for (i = 1; i < header_token.length; i++) {
            TMConfig c = TMConfig.config(header_token[i]);
            testSet.addTestSample(c);
         }
         br.close();
         return testSet;

      } catch (Exception e) {
         throw new RuntimeException(e);
      }


   }

}
