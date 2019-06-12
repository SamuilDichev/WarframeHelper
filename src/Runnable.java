import models.Deal;
import models.Item;
import models.RelicDB;
import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.MongoHelper;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Samuil Dichev
 */
public class Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(Runnable.class);
  private static final String DEALS_STRING = "%.1f dpp (%dp/%dd) - %s (%s) - %s %dp";

  public static void main(String[] args) throws Exception {
    Scanner in = new Scanner(System.in);
    System.out.println("Choose an option:\n" +
            "1. Find Ducat deals\n" +
            "2. Find Relic deals\n" +
            "3. Update In-game Items\n" +
            "4. Update Relics");
    System.out.print("Your choice: ");

    int option = in.nextInt();
    switch (option) {
      case 1:
        findDppDeals();
        break;
      case 2:
        findRelicDeals();
        break;
      case 3:
        updateItemDB();
        break;
      case 4:
        updateRelics();
        break;
      default:
        findDppDeals();
        break;
    }
  }

  private static void findDppDeals() throws Exception {
    ItemScraper scraper = new ItemScraper();

    Map<String, List<Deal>> deals = scraper.findDucatDeals();
    for (String ingame_name : deals.keySet()) {
      System.out.println("\n=== " + ingame_name + " ===");
      for (Deal deal : deals.get(ingame_name)) {
        System.out.println(createDealsString(deal));
      }

      System.out.println(createWhisperString(ingame_name, deals.get(ingame_name)));
    }
  }

  private static String createDealsString(Deal deal) {
    return String.format(DEALS_STRING, deal.getDucatsPerPlatinum(), deal.getPlatinum(),
            deal.getDucats(), deal.getIngame_name(), deal.getStatus(),
            normalizeItemName(deal.getItem_name()), deal.getPlatinum());
  }

  private static String createWhisperString(String ingame_name, List<Deal> deals) {
    String whisperString = "/w " + ingame_name + " Hi, WTB " + deals.size() + " items: ";
    for (int i = 0; i < deals.size(); i++) {
      if (i == 0 || !deals.get(i).getItem_name().equals(deals.get(i - 1).getItem_name())) {
        whisperString += normalizeItemName(deals.get(i).getItem_name()) + " " + deals.get(i).getPlatinum() + "p, ";
      }
    }

    whisperString = whisperString.substring(0, whisperString.length() - 2) + "! (warframe.market)";

    return whisperString;
  }

  private static String normalizeItemName(String name) {
    if (name.endsWith("Blueprint")) {
      name = name.replace(" Blueprint", "");
    }

    return "[" + name + "]";
  }

  private static void findRelicDeals() {
    Datastore ds = MongoHelper.getInstance().getDatastore();
    List<RelicDB> relics = ds.find(RelicDB.class).asList();
    for (RelicDB relic : relics) {
      List<Item> items = ds.find(Item.class).field("_id").hasAnyOf(relic.getDrops()).asList();
      Comparator<Item> comparator = Comparator.comparing(Item::getPlatinum);
      items.sort(comparator);
      relic.setMinDropPlatinum(items.get(0).getPlatinum());
      relic.setMaxDropPlatinum(items.get(items.size() - 1).getPlatinum());
      relic.setTotalDropPlatinum(items.stream().mapToDouble(Item::getPlatinum).sum());
      ds.save(relic);
    }

    List<RelicDB> updatedRelics = ds.find(RelicDB.class).order("tier,-maxDropPlatinum,name").asList();
    for (RelicDB relic : updatedRelics) {
      if (relic.getMaxDropPlatinum() < 20) {
        continue;
      }

      System.out.println(relic.getTier() + " " + relic.getName() + "(" + relic.getStatus() +
              ") - Min: " + relic.getMinDropPlatinum() + ", max: " + relic.getMaxDropPlatinum() +
              ", total: " + relic.getTotalDropPlatinum());
    }
  }

  private static void updateRelics() throws Exception {
    ItemScraper scraper = new ItemScraper();
    scraper.updateRelics();
  }

  private static void updateItemDB() {
    ItemScraper scraper = new ItemScraper();
    scraper.updateItemDB();
  }
}
