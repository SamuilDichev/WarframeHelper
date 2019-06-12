package models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Samuil Dichev
 */
@Entity(value = "relics", noClassnameStored = true)
public class RelicDB {

  @Id
  private ObjectId id;

  @Indexed
  private String tier;

  @Indexed
  private String name;
  private Status status;
  private List<ObjectId> drops = new ArrayList<>();
  private double minDropPlatinum;
  private double maxDropPlatinum;
  private double averageDropPlatinum;
  private double totalDropPlatinum;

  public ObjectId getId() {
    return id;
  }

  public void setId(String id) {
    this.id = new ObjectId(id);
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getTier() {
    return tier;
  }

  public void setTier(String tier) {
    this.tier = tier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public List<ObjectId> getDrops() {
    return drops;
  }

  public void setDrops(List<ObjectId> drops) {
    this.drops = drops;
  }

  public double getMinDropPlatinum() {
    return minDropPlatinum;
  }

  public void setMinDropPlatinum(double minDropPlatinum) {
    this.minDropPlatinum = minDropPlatinum;
  }

  public double getMaxDropPlatinum() {
    return maxDropPlatinum;
  }

  public void setMaxDropPlatinum(double maxDropPlatinum) {
    this.maxDropPlatinum = maxDropPlatinum;
  }

  public double getAverageDropPlatinum() {
    return averageDropPlatinum;
  }

  public void setAverageDropPlatinum(double averageDropPlatinum) {
    this.averageDropPlatinum = averageDropPlatinum;
  }

  public double getTotalDropPlatinum() {
    return totalDropPlatinum;
  }

  public void setTotalDropPlatinum(double totalDropPlatinum) {
    this.totalDropPlatinum = totalDropPlatinum;
  }
}
