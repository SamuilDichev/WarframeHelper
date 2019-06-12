package util;

import com.mongodb.MongoClient;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

/**
 * @author Samuil Dichev
 */
public class MongoHelper {

  private static final String DB_NAME = "WFMarket";
  private static Morphia MORPHIA;
  private static Datastore DATASTORE;
  private static final MongoHelper INSTANCE = new MongoHelper();

  private MongoHelper() {
    MORPHIA = new Morphia();
    DATASTORE = MORPHIA.createDatastore(new MongoClient(), DB_NAME);
    DATASTORE.ensureIndexes();
  }

  public static MongoHelper getInstance() {
    return INSTANCE;
  }

  public Morphia getMorphia() {
    return MORPHIA;
  }

  public Datastore getDatastore() {
    return DATASTORE;
  }
}
