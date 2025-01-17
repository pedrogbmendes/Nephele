package lynceus;

/**
 * @author Diego Didona
 * @email diego.didona@epfl.ch
 * @since 11.03.18
 */
public class ConfigUtility<C extends Configuration> {
   private final C configuration;
   private final double utility;
   private final double cost;


   public ConfigUtility(C configuration, double utility, double cost) {
      this.configuration = configuration;
      this.utility = utility;
      this.cost = cost;
   }

   public double getCost() {
      return cost;
   }

   public C getConfiguration() {
      return configuration;
   }

   public double getUtility() {
      return utility;
   }

   @Override
   public String toString() {
      return "ConfigUtility{" +
            "configuration=" + configuration +
            ", utility=" + utility +
            ", cost=" + cost +
            '}';
   }
}
