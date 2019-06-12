package models.Web;

/**
 * @author Samuil Dichev
 */
public class RewardWeb {
  private String itemName;

  public String getItemName() {
    if (itemName.endsWith("Blueprint")) {
      itemName = itemName.replace(" Blueprint", "");
    }

    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }
}
