package models;


import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.List;

/**
 * @author Samuil Dichev
 */
@Entity(value = "items", noClassnameStored = true)
public class Item {

  @Id
  private ObjectId id;

  @Indexed
  private String item_name;
  private String url_name;
  private double platinum;

  @Indexed
  private int ducats;

  private List<String> drops;

  public ObjectId getId() {
    return id;
  }

  public void setId(String id) {
    this.id = new ObjectId(id);
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getItem_name() {
    return item_name;
  }

  public void setItem_name(String item_name) {
    this.item_name = item_name;
  }

  public String getUrl_name() {
    return url_name;
  }

  public void setUrl_name(String url_name) {
    this.url_name = url_name;
  }

  public double getPlatinum() {
    return platinum;
  }

  public void setPlatinum(double platinum) {
    this.platinum = platinum;
  }

  public int getDucats() {
    return ducats;
  }

  public void setDucats(int ducats) {
    this.ducats = ducats;
  }

  public List<String> getDrops() {
    return drops;
  }

  public void setDrops(List<String> drops) {
    this.drops = drops;
  }
}
