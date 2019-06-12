package models;

import org.bson.types.ObjectId;

/**
 * @author Samuil Dichev
 */
public class User {
  private ObjectId id;
  private String ingame_name;
  private String status;
  private String last_seen;
  private int reputation;
  private String avatar;
  private int reputation_bonus;
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

  public String getLast_seen() {
    return last_seen;
  }

  public void setLast_seen(String last_seen) {
    this.last_seen = last_seen;
  }

  public int getReputation() {
    return reputation;
  }

  public void setReputation(int reputation) {
    this.reputation = reputation;
  }

  public String getAvatar() {
    return avatar;
  }

  public void setAvatar(String avatar) {
    this.avatar = avatar;
  }

  public int getReputation_bonus() {
    return reputation_bonus;
  }

  public void setReputation_bonus(int reputation_bonus) {
    this.reputation_bonus = reputation_bonus;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
