package models;

/**
 * @author Samuil Dichev
 */
public class Deal implements Comparable<Deal> {
  private String item_name;
  private int platinum;
  private int ducats;
  private double ducatsPerPlatinum;
  private String ingame_name;
  private String status;

  public Deal() {

  }

  public Deal(Item item, Order order) {
    this.item_name = item.getItem_name();
    this.platinum = order.getPlatinum();
    this.ducats = item.getDucats();
    this.ducatsPerPlatinum = item.getDucats() / (double) order.getPlatinum();
    this.ingame_name = order.getUser().getIngame_name();
    this.status = order.getUser().getStatus();
  }

  public int compareTo(Deal deal) {
    return this.getIngame_name().compareTo(deal.getIngame_name());
  }

  public String getItem_name() {
    return item_name;
  }

  public void setItem_name(String item_name) {
    this.item_name = item_name;
  }

  public int getPlatinum() {
    return platinum;
  }

  public void setPlatinum(int platinum) {
    this.platinum = platinum;
  }

  public int getDucats() {
    return ducats;
  }

  public void setDucats(int ducats) {
    this.ducats = ducats;
  }

  public double getDucatsPerPlatinum() {
    return ducatsPerPlatinum;
  }

  public void setDucatsPerPlatinum(double ducatsPerPlatinum) {
    this.ducatsPerPlatinum = ducatsPerPlatinum;
  }

  public String getIngame_name() {
    return ingame_name;
  }

  public void setIngame_name(String ingame_name) {
    this.ingame_name = ingame_name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
