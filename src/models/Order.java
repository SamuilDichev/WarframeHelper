package models;

import org.bson.types.ObjectId;

/**
 * @author Samuil Dichev
 */
public class Order {
  private ObjectId id;
  private boolean visible;
  private int platinum;
  private User user;
  private int quantity;
  private String order_type;
  private String creation_date;
  private String last_update;
  private String platform;
  private String region;

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public void setId(String id) {
    this.id = new ObjectId(id);
  }

  public boolean isVisible() {
    return visible;
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
  }

  public int getPlatinum() {
    return platinum;
  }

  public void setPlatinum(int platinum) {
    this.platinum = platinum;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getOrder_type() {
    return order_type;
  }

  public void setOrder_type(String order_type) {
    this.order_type = order_type;
  }

  public String getCreation_date() {
    return creation_date;
  }

  public void setCreation_date(String creation_date) {
    this.creation_date = creation_date;
  }

  public String getLast_update() {
    return last_update;
  }

  public void setLast_update(String last_update) {
    this.last_update = last_update;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
