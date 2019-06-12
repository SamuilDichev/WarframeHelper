import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import models.*;
import models.Web.RelicWeb;
import models.Web.RewardWeb;
import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.JsonHelper;
import util.MongoHelper;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Samuil Dichev
 */
public class ItemScraper {
  private static final Logger LOGGER = LoggerFactory.getLogger(ItemScraper.class);
  private static final String ITEMS_URL = "https://api.warframe.market/v1/items";
  private static final String RELICS_URL = "https://drops.warframestat.us/data/relics.json";
  private static final double MIN_RATIO = 15.0;
  private AsyncHttpClient client;

  public ItemScraper() {
    client = new AsyncHttpClient();
  }

  public Map<String, List<Deal>> findDucatDeals() throws Exception {
    Datastore ds = MongoHelper.getInstance().getDatastore();
    List<Item> ducatItems = ds.find(Item.class).field("ducats").greaterThan(0).asList();
    Map<String, List<Deal>> userDeals = new HashMap<>();

    for (Item item : ducatItems) {
      Future<Response> whenResponse = client.prepareGet(ITEMS_URL + "/" + item.getUrl_name() + "/orders").execute();
      Response response = whenResponse.get();
      ObjectNode jsonBodyNode = new ObjectMapper().readValue(response.getResponseBody(), ObjectNode.class);

      String orderText = jsonBodyNode.path("payload").path("orders").toString();
      List<Order> unfilteredOrders = JsonHelper.fromJson(orderText, new TypeReference<List<Order>>() {

      });

      List<Order> orders = unfilteredOrders.stream().filter(o -> "sell".equals(o.getOrder_type()))
              .filter(o -> !"offline".equals(o.getUser().getStatus())).collect(Collectors.toList());

      for (Order order : orders) {
        if ("sell".equals(order.getOrder_type()) && !"offline".equals(order.getUser().getStatus())) {
          double ratio = item.getDucats() / order.getPlatinum();

          if (ratio >= MIN_RATIO) {
            Deal deal = new Deal(item, order);
            List<Deal> userDeal = userDeals.getOrDefault(deal.getIngame_name(), new ArrayList<>());
            userDeal.add(deal);
            userDeals.put(deal.getIngame_name(), userDeal);
          }
        }
      }

      item.setPlatinum(getAveragePlatinum(orders));
      ds.save(item);
    }

    return userDeals;
  }

  private double getAveragePlatinum(List<Order> orders) {
    Comparator<Order> comparator = Comparator.comparing(Order::getPlatinum);
    orders.sort(comparator);
    int iMax = Math.min(orders.size(), 5);
    int ordersTotalPlatinum = 0;
    for (int i = 0; i < iMax; i++) {
      ordersTotalPlatinum += orders.get(i).getPlatinum();
    }

    return (double) Math.round((double) ordersTotalPlatinum / iMax * 100) / 100;
  }

  /**
   * Scrapes the relic URL for all possible relics. If their drops already exist in the DB as items,
   * their DB IDs are added to the relic's drop list before saving the relic to the database.
   *
   * @throws Exception
   */
  public void updateVaultedRelics() throws Exception {
    Future<Response> whenResponse = client.prepareGet(RELICS_URL).execute();
    Response response = whenResponse.get();
    ObjectNode jsonBodyNode = new ObjectMapper().readValue(response.getResponseBody(), ObjectNode.class);

    // Scrape all relics from the URL
    String relicsText = jsonBodyNode.path("relics").toString();
    List<RelicWeb> unfilteredRelics = JsonHelper.fromJson(relicsText, new TypeReference<List<RelicWeb>>() {

    });

    Datastore ds = MongoHelper.getInstance().getDatastore();

    // Filter out all relics except "Intact" quality
    for (int i = 0; i < unfilteredRelics.size(); i++) {
      RelicWeb unfilteredRelic = unfilteredRelics.get(i);
      if (!"Intact".equals(unfilteredRelic.getState())) {
        continue;
      }

      RelicDB relic = ds.find(RelicDB.class)
              .field("tier").equal(unfilteredRelic.getTier())
              .field("name").equal(unfilteredRelic.getRelicName()).get();

      if (relic == null) {
        relic = new RelicDB();
        relic.setTier(unfilteredRelic.getTier());
        relic.setName(unfilteredRelic.getRelicName());
        relic.setStatus(Status.UNVAULTED);
      }

      // Search for Relic's drops in the Item DB and add them to its drop list if they exist already, otherwise skip
      for (RewardWeb reward : unfilteredRelic.getRewards()) {
        Item dbitem = ds.find(Item.class).field("item_name").equal(reward.getItemName()).get();
        if (dbitem != null && !relic.getDrops().contains(dbitem.getId())) {
          relic.getDrops().add(dbitem.getId());
        }
      }

      ds.save(relic);

      // Log every X lines
      if (i % 10 == 0 && i > 0) {
        LOGGER.info("Saved {}/{} relics.", i, unfilteredRelics.size());
        System.out.println("Saved " + i + "/" + unfilteredRelics.size() + " relics.");
      }
    }
  }

  public void updateRelicInfo() {
    Datastore ds = MongoHelper.getInstance().getDatastore();
    List<Item> items = ds.find(Item.class).field("ducats").greaterThan(0).asList();

    for (int i = 0; i < items.size(); i++) {
      Item item = items.get(i);
      List<String> drops = item.getDrops();

      if (drops == null) {
        continue;
      }

      for (String drop : drops) {
        if (drop.contains("Lith") || drop.contains("Meso") || drop.contains("Neo") || drop.contains("Axi")) {
          String[] dropSplit = drop.split("\\s+");
          String tier = dropSplit[0];
          String name = dropSplit[1];
          RelicDB relic = ds.find(RelicDB.class).field("tier").equal(tier).field("name").equal(name).get();

          if (relic == null) {
            relic = new RelicDB();
            relic.setTier(tier);
            relic.setName(name);
            relic.setStatus(Status.UNVAULTED);
          }

          if (!relic.getDrops().contains(item.getId())) {
            relic.getDrops().add(item.getId());
          }

          ds.save(relic);

          if (i % 100 == 0 && i > 0) {
            LOGGER.info("Saved {}/{} items.", i, items.size());
            System.out.println("Saved " + i + "/" + items.size() + " items.");
          }
        }
      }
    }
  }

  public void updateItemDB() {
    Future<Response> whenResponse = client.prepareGet(ITEMS_URL).execute();

    try {
      Response response = whenResponse.get();
      String jsonBody = response.getResponseBody();

      ObjectNode node = new ObjectMapper().readValue(jsonBody, ObjectNode.class);
      String itemList = node.path("payload").path("items").get("en").toString();
      List<Item> items = JsonHelper.fromJson(itemList, new TypeReference<List<Item>>() {

      });

      for (int i = 0; i < items.size(); i++) {
        Item item = addDetails(items.get(i));
        MongoHelper.getInstance().getDatastore().save(item);

        if (i % 100 == 0 && i > 0) {
          LOGGER.info("Saved {}/{} items.", i, items.size());
          System.out.println("Saved " + i + "/" + items.size() + " items.");
        }
      }

    } catch (Exception e) {
      LOGGER.error("Ops", e);
      e.printStackTrace();
    }
  }

  private Item addDetails(Item item) throws Exception {
    Future<Response> whenResponse = client.prepareGet(ITEMS_URL + "/" + item.getUrl_name()).execute();

    Response response = whenResponse.get();
    String jsonBody = response.getResponseBody();

    ObjectNode jsonBodyNode = new ObjectMapper().readValue(jsonBody, ObjectNode.class);
    JsonNode itemSet = jsonBodyNode.path("payload").path("item").get("items_in_set");

    Iterator<JsonNode> itemSetIt = itemSet.elements();
    while (itemSetIt.hasNext()) {
      JsonNode jsonItem = itemSetIt.next();

      if (jsonItem.get("id").asText().equals(item.getId().toString())) {
        if (jsonItem.has("ducats")) {
          item.setDucats(Integer.valueOf(jsonItem.get("ducats").toString()));
        } else {
          item.setDucats(0);
        }

        JsonNode dropList = jsonItem.path("en").get("drop");
        Iterator<JsonNode> dropListIt = dropList.elements();
        List<String> drops = new ArrayList<>();

        while (dropListIt.hasNext()) {
          JsonNode drop = dropListIt.next();
          drops.add(drop.get("name").asText());
        }

        item.setDrops(drops);

        break;
      }
    }

    return item;
  }
}
