package models.Web;

import java.util.List;

/**
 * @author Samuil Dichev
 */
public class RelicWeb {

  private String tier;
  private String relicName;
  private String state;
  private List<RewardWeb> rewards;

  public String getTier() {
    return tier;
  }

  public void setTier(String tier) {
    this.tier = tier;
  }

  public String getRelicName() {
    return relicName;
  }

  public void setRelicName(String relicName) {
    this.relicName = relicName;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public List<RewardWeb> getRewards() {
    return rewards;
  }

  public void setRewards(List<RewardWeb> rewards) {
    this.rewards = rewards;
  }
}
