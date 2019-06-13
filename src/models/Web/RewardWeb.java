package models.Web;

/**
 * @author Samuil Dichev
 */
public class RewardWeb {
  private String itemName;
  private double chance;

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public double getChance() {
    return chance;
  }

  public void setChance(float chance) {
    this.chance = chance;
  }
}
