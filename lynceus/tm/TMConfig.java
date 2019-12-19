package lynceus.tm;

import java.util.Objects;

import lynceus.Configuration;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 16.03.18
 */

public class TMConfig implements Configuration {

   /* class attributes */
   private final tm _tm;
   private final budget _budget;	// HTM Capacity Abort Policy
   private final int _threads;
   private final int init_budget;	// HTM Abort Budget
   
   public enum tm {
      HTM(0), TINYSTM(1), SWISSTM(2), NOREC(3), TL2(4);
      private int id;

      tm(int i) {
         id = i;
      }
   }
   
   public enum budget {
      DECREASE(0), HALF(1), ZERO(2);
      private int id;

      budget(int i) {
         id = i;
      }
   }

   
   /* class constructor */
   public TMConfig(tm _tm, budget _budget, int _threads, int init_budget) {
      this._tm = _tm;
      this._budget = _budget;
      this._threads = _threads;
      this.init_budget = init_budget;
   }
   
   
   /* class methods */
   @Override
   public int hashCode() {
      return Objects.hash(_tm, _budget, _threads, init_budget);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      //NB: I want to consider as equal different objects that refer to the same config, even across classes (i.e., config vs wekaconfig etc)
      //if (o == null || getClass() != o.getClass()) return false;
      TMConfig tmConfig = (TMConfig) o;
      if (_threads == tmConfig._threads &&
            init_budget == tmConfig.init_budget &&
            _tm.equals(tmConfig._tm) &&
            _budget.equals(tmConfig._budget)) return true;
      return false;
   }

   @Override
   public String toString() {
      return "TMConfig{" +
            "_tm=" + _tm +
            ", _budget=" + _budget +
            ", _threads=" + _threads +
            ", init_budget=" + init_budget +
            '}';
   }

   
   /* getters */
   public tm get_tm() {
      return _tm;
   }

   public budget get_budget() {
      return _budget;
   }

   public int get_threads() {
      return _threads;
   }

   public int getInit_budget() {
      return init_budget;
   }

   
   /* interface methods to be implemented */
   @Override
   public int numAttributes() {
      return 4;
   }
   
   @Override
   public Configuration clone() {
      return new TMConfig(_tm, _budget, _threads, init_budget);
   }

   @Override
   public Object at(int i) {
      switch (i) {
         case 0:
            return _tm.id;
         case 1:
            return _budget.id;
         case 2:
            return _threads;
         case 3:
            return init_budget;
         default:
            throw new RuntimeException("Requested attribute " + i + " but only " + numAttributes() + " available");
      }
   }

   
   /* other methods */
   public static TMConfig config(String s) {
      //System.out.println("Parsing " + s);
      return new TMConfig(tm(s), budget(s), threads(s), initBudget(s));
   }

   private static TMConfig.tm tm(String s) {
      if (s.contains("RETRY"))
         return TMConfig.tm.HTM;
      if (s.contains("tiny"))
         return TMConfig.tm.TINYSTM;
      if (s.contains("swiss"))
         return TMConfig.tm.SWISSTM;
      if (s.contains("tl2"))
         return TMConfig.tm.TL2;
      if (s.contains("norec"))
         return tm.NOREC;
      throw new RuntimeException("TM " + s + " not recognized");
   }

   private static TMConfig.budget budget(String s) {
      if (tm(s) != tm.HTM) return TMConfig.budget.ZERO;
      if (s.contains("NO_RETRY"))
         return TMConfig.budget.ZERO;
      if (s.contains("LINEAR"))
         return TMConfig.budget.DECREASE;
      if (s.contains("HALF"))
         return TMConfig.budget.HALF;
      throw new RuntimeException("Budget " + s + " not recognized");
   }

   private static int initBudget(String s) {
      //HALF_DECREASE=RETRY_BUDGET_20=4
      if (tm(s).equals(TMConfig.tm.HTM)) {
         String[] ss = s.split("_");
         String[] sss = ss[ss.length - 1].split("=");
         return Integer.parseInt(sss[0]);
      }
      return 0;
   }

   private static int threads(String s) {
      //HALF_DECREASE=RETRY_BUDGET_20=4
      String[] ss = s.split("=");
      if (tm(s).equals(tm.HTM)) {
         return Integer.parseInt(ss[ss.length - 1]);
      } else {
         String[] tt = ss[ss.length - 2].split("th");
         return Integer.parseInt(tt[0]);
      }
   }

}
