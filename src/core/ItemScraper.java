package core;

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
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
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

  // TODO Change relics to be taken directly from warframe's drop tables. These guys don't update relics for days.
  private static final String RELICS_URL = "https://drops.warframestat.us/data/relics.json";
  private static final double MIN_RATIO = 15.0;
  private AsyncHttpClient client;

  public ItemScraper() {
    client = new AsyncHttpClient();
  }

  public Map<String, List<Deal>> findDucatDeals() throws Exception {
    Datastore ds = MongoHelper.getInstance().getDatastore();
    List<Item> ducatItems = ds.find(Item.class).field("ducats").greaterThan(0).asList();
    Map<String, List<models.Deal>> userDeals = new HashMap<>();

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
  public void updateRelics() throws Exception {
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

      RelicDB relic = getOrCreateRelic(unfilteredRelic.getTier(), unfilteredRelic.getRelicName());

      // Search for Relic's drops in the Item DB and add them to its drop list if they exist already, otherwise skip
      for (RewardWeb reward : unfilteredRelic.getRewards()) {
        Query<Item> itemQuery = ds.find(Item.class);
        itemQuery.or(
                itemQuery.criteria("item_name").equal(reward.getItemName()),
                itemQuery.criteria("item_name").equal(reward.getItemName().replace(" Blueprint", ""))
        );

        Item dbItem = itemQuery.get();
        if (dbItem != null) {
          relic.getDrops().compute(dbItem.getId(), (k, v) -> (v == null) ? reward.getChance() : v + reward.getChance());
        } else if ("Forma Blueprint".equals(reward.getItemName())) {
          Item forma = new Item();
          forma.setItem_name("Forma Blueprint");
          forma.setDucats(0);
          forma.setPlatinum(0);
          ds.save(forma);

          relic.getDrops().compute(forma.getId(), (k, v) -> (v == null) ? reward.getChance() : v + reward.getChance());
        }
      }

      ds.save(relic);

      // Log every X lines
      if (i % 100 == 0 && i > 0) {
        LOGGER.info("Saved {}/{} relics.", i, unfilteredRelics.size());
        System.out.println("Saved " + i + "/" + unfilteredRelics.size() + " relics.");
      }
    }
  }

  private RelicDB getOrCreateRelic(String tier, String name) {
    RelicDB relic = MongoHelper.getInstance().getDatastore().find(RelicDB.class)
            .field("tier").equal(tier)
            .field("name").equal(name).get();

    if (relic == null) {
      relic = new RelicDB();
      relic.setTier(tier);
      relic.setName(name);
      relic.setStatus(Status.UNVAULTED);
    }

    relic.setDropsMap(new HashMap<>());

    return relic;
  }

  /**
   * Scrapes the item URL for all items and saves them to DB while reusing the ObjectIds from the URL data.
   */
  public void updateItemDB() {
    Future<Response> whenResponse = client.prepareGet(ITEMS_URL).execute();

    try {
      Response response = whenResponse.get();
      String jsonBody = response.getResponseBody();

      ObjectNode node = new ObjectMapper().readValue(jsonBody, ObjectNode.class);
      String itemList = node.path("payload").get("items").toString();
      List<Item> items = JsonHelper.fromJson(itemList, new TypeReference<List<Item>>() {

      });

      Datastore ds = MongoHelper.getInstance().getDatastore();
      for (int i = 0; i < items.size(); i++) {
        Item item = addDetails(items.get(i));
        ds.save(item);

        if (i % 100 == 0 && i > 0) {
          LOGGER.info("Saved {}/{} items.", i, items.size());
          System.out.println("Saved " + i + "/" + items.size() + " items.");
        }
      }

      updateMismatchingNames();
    } catch (Exception e) {
      LOGGER.error("Ops", e);
      e.printStackTrace();
    }
  }

  private void updateMismatchingNames() {
    Datastore ds = MongoHelper.getInstance().getDatastore();

    Query<Item> query = ds.createQuery(Item.class).field("item_name").equal("Kavasa Prime Collar Blueprint");
    UpdateOperations<Item> ops = ds.createUpdateOperations(Item.class).set("item_name", "Kavasa Prime Kubrow Collar Blueprint");
    ds.update(query, ops);

    query = ds.createQuery(Item.class).field("item_name").equal("Kavasa Prime Collar Buckle");
    ops = ds.createUpdateOperations(Item.class).set("item_name", "Kavasa Prime Buckle");
    ds.update(query, ops);

    query = ds.createQuery(Item.class).field("item_name").equal("Kavasa Prime Collar Band");
    ops = ds.createUpdateOperations(Item.class).set("item_name", "Kavasa Prime Band");
    ds.update(query, ops);
  }

  /**
   * Scrapes specific item's URL for ducats and relic drop locations. Add them to the model and saves to DB.
   *
   * @param item
   * @return
   * @throws Exception
   */
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
