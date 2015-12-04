package bcid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.SettingsManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * The bcid class encapsulates all of the information we know about a BCID.
 * This includes data such as the
 * status of EZID creation, associated dataset calls, and any metadata.
 * It can include a data element or a data group.
 * There are several ways to construct an element, including creating it from scratch, or instantiating by looking
 * up an existing identifier from the database.
 */
public class bcid extends GenericIdentifier {
    protected URI webAddress = null;        // URI for the webAddress, EZID calls this _target (e.g. http://biocode.berkeley.edu/specimens/MBIO56)
    protected String sourceID = null;       // Source or local identifier (e.g. MBIO056)
    protected String what = null;           // erc.what
    protected String when = null;           // erc.when
    protected String who = null;            // erc.who
    protected String title = null;            // erc.who\
    public String projectCode = null;
    protected Boolean datasetsEzidMade;
    protected Boolean datasetsEzidRequest;
    protected String datasetsPrefix;
    protected String datasetsTs;
    protected Boolean identifiersEzidRequest;
    protected Boolean identifiersEzidMade;
    protected Boolean datasetsSuffixPassthrough;
    protected String identifiersTs;
    //protected String ark;
    protected String doi;
    protected Integer dataset_id;
    protected String graph = null;

    protected String level;
    final static String UNREGISTERED_ELEMENT = "Unregistered Element";
    final static String ELEMENT = "BCID Data Element";
    final static String GROUP = "BCID Data Group";

    // HashMap to store metadata values
    private HashMap<String, String> map = new HashMap<String, String>();


    static SettingsManager sm;

    private static Logger logger = LoggerFactory.getLogger(bcid.class);

    static {
        sm = SettingsManager.getInstance();
        sm.loadProperties();
    }

    /**
     * Create data group
     *
     * @param datasets_id
     */
    public bcid(Integer datasets_id) {
        dataGroupMinter dataset = setDatasets_id(datasets_id);
        projectCode = dataset.getProject(dataset_id);

        dataset.close();
    }


    /**
     * Create an element given a source identifier, and a resource type identifier
     *
     * @param sourceID
     * @param dataset_id
     */
    public bcid(String sourceID, Integer dataset_id) {
        dataGroupMinter dataset = setDatasets_id(dataset_id);
        try {
            if (sourceID != null && !sourceID.equals("")) {
                identifier = new URI(dataset.identifier + sm.retrieveValue("divider") + sourceID);
            } else {
                identifier = dataset.identifier;
            }
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }
        projectCode = dataset.getProject(dataset_id);
        dataset.close();
    }

    /**
     * Create an element given a source identifier, web address for resolution, and a dataset_id
     * This method is meant for CREATING bcids.
     *
     * @param sourceID
     * @param webAddress
     * @param dataset_id
     */
    public bcid(String sourceID, URI webAddress, Integer dataset_id) {
        dataGroupMinter dataset = setDatasets_id(dataset_id);
        this.webAddress = webAddress;

        try {
            if (sourceID != null && !sourceID.equals("")) {
                identifier = new URI(dataset.identifier + sm.retrieveValue("divider") + sourceID);
            } else {
                identifier = dataset.identifier;
            }
            projectCode = dataset.getProject(dataset_id);
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException for uri: {}", dataset.identifier + sm.retrieveValue("divider") + sourceID, e);
        }
        // Reformat webAddress in this constructor if there is a sourceID
        if (sourceID != null && webAddress != null && !sourceID.toString().trim().equals("") && !webAddress.toString().trim().equals("")) {
            //System.out.println("HERE" + webAddress);
            try {
                this.webAddress = new URI(webAddress + sourceID);
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException for uri: {}", webAddress + sourceID, e);
            }
        }
        dataset.close();
    }


    /**
     * Create an element by passing in an BigInteger for the specific slot in the database and a string representation of this
     * ARK.
     * This class should probably only be instantiated by the resolver class, after it figures out what BigInteger
     * a particular ARK belongs to.
     *
     * @param identifiers_id indicating the integer of this identifier in the BCID system
     * @param ark            is the Full identifier
     */
    /* public bcid(BigInteger identifiers_id, String ark) {
       try {
           database db = new database();
           Statement stmt = db.conn.createStatement();
           String datasets = "SELECT " +
                   "   d.ezidMade," +
                   "   d.ezidRequest," +
                   "   d.prefix,d.ts," +
                   "   d.title," +
                   "   i.ezidMade," +
                   "   i.ezidRequest," +
                   "   d.suffixPassthrough," +
                   "   i.localid," +
                   "   i.webaddress," +
                   "   d.resourceType," +
                   "   i.ts," +
                   //"   concat_ws('',u.fullname,' &lt;',u.email,'&gt;') as username " +
                   "   CONCAT_WS(' ',u.firstName, u.lastName) " +
                   " FROM datasets d, identifiers i, users u " +
                   " WHERE d.datasets_id = i.datasets_id && " +
                   " d.users_id = u.user_id && " +
                   " i.identifiers_id = " + identifiers_id.toString();
           ResultSet rs = stmt.executeQuery(datasets);
           rs.next();
           int count = 1;
           datasetsEzidMade = rs.getBoolean(count++);
           datasetsEzidRequest = rs.getBoolean(count++);
           datasetsPrefix = rs.getString(count++);
           datasetsTs = rs.getString(count++);
           title = rs.getString(count++);
           identifiersEzidMade = rs.getBoolean(count++);
           identifiersEzidRequest = rs.getBoolean(count++);
           datasetsSuffixPassthrough = rs.getBoolean(count++);
           sourceID = rs.getString(count++);
           String webaddress = rs.getString(count++);
           if (webaddress != null) {
               webAddress = new URI(webaddress);
           }
           what = rs.getString(count++);
           when = rs.getString(count++);
           who = rs.getString(count++);
           this.identifier = new URI(ark);
           this.level = this.ELEMENT;

       } catch (Exception e) {
           e.printStackTrace();
       }
   } */


    /**
     * Internal function for setting the source ID (local identifier that has been passed in)
     *
     * @param sourceID
     */
    private void setSourceID(String sourceID, dataGroupMinter dataset) {
        try {
            if (sourceID != null && !sourceID.equals("")) {
                identifier = new URI(dataset.identifier + sm.retrieveValue("divider") + sourceID);
            } else {
                identifier = dataset.identifier;
            }
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }

        // Reformat webAddress in this constructor if there is a sourceID
        if (sourceID != null && webAddress != null && !sourceID.toString().trim().equals("") && !webAddress.toString().trim().equals("")) {
            //System.out.println("HERE" + webAddress);
            try {
                this.webAddress = new URI(webAddress + sourceID);
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException thrown", e);
            }
        }
    }

    /**
     * Internal functional for setting the datasets_id that has been passed in
     *
     * @param pDatasets_id
     */
    private dataGroupMinter setDatasets_id(Integer pDatasets_id) {
        /*  try {
          database db = new database();
          Statement stmt = db.conn.createStatement();
          String datasets = "SELECT d.ezidMade," +
                  "   d.ezidRequest," +
                  "   d.prefix," +
                  "   d.ts," +
                  "   d.title," +
                  "   d.resourceType," +
                  "   d.suffixPassthrough," +
                  "   d.doi," +
                  "   d.webAddress," +
                  //"   concat_ws('',u.fullname,' &lt;',u.email,'&gt;') as username " +
                  "   CONCAT_WS(' ',u.firstName, u.lastName) " +
                  " FROM datasets d, users u " +
                  " WHERE " +
                  " d.datasets_id = " + datasets_id + " && " +
                  " d.users_id = u.user_id";

          ResultSet rs = stmt.executeQuery(datasets);
          rs.next();
          int count = 1;
          datasetsEzidMade = rs.getBoolean(count++);
          datasetsEzidRequest = rs.getBoolean(count++);
          datasetsPrefix = rs.getString(count++);
          datasetsTs = rs.getString(count++);
          title = rs.getString(count++);
          what = rs.getString(count++);
          datasetsSuffixPassthrough = rs.getBoolean(count++);
          doi = rs.getString(count++);
          try {
              webAddress = new URI(rs.getString(count++));
          } catch (NullPointerException e) {
              webAddress = null;
          }
          who = rs.getString(count++);
          identifier = new URI(datasetsPrefix);
          //what = new ResourceTypes().get(ResourceTypes.DATASET).uri;
          when = datasetsTs;
          level = this.GROUP;

      } catch (Exception e) {
          e.printStackTrace();
      }  */


        // Create a dataset representation based on the dataset_id
        dataGroupMinter dataset = new dataGroupMinter(pDatasets_id);
        //when =  new dates().now();
        when = dataset.ts;

        this.graph = dataset.getGraph();
        this.webAddress = dataset.getWebAddress();
        this.dataset_id = pDatasets_id;
        this.what = dataset.getResourceType();
        this.title = dataset.title;
        this.projectCode = dataset.projectCode;
        this.datasetsTs = dataset.ts;
        this.datasetsPrefix = dataset.getPrefix();
        this.doi = dataset.doi;
        this.level = this.UNREGISTERED_ELEMENT;
        this.who = dataset.who;
        identifiersEzidRequest = false;
        identifiersEzidMade = false;
        datasetsEzidMade = dataset.ezidMade;
        datasetsEzidRequest = dataset.ezidRequest;
        datasetsSuffixPassthrough = dataset.getSuffixPassThrough();

        return dataset;

    }

    /**
     * Convert the class variables to a HashMap of metadata.
     *
     * @return
     */
    public HashMap<String, String> getMetadata() {
        put("ark", identifier);
        put("who", who);
        put("when", when);
        put("what", what);
        put("webaddress", webAddress);
        put("level", level);
        put("title", title);
        put("projectCode", projectCode);
        put("sourceID", sourceID);
        put("doi", doi);
        put("datasetsEzidMade", datasetsEzidMade);
        put("datasetsSuffixPassThrough", datasetsSuffixPassthrough);
        put("datasetsEzidRequest", datasetsEzidRequest);
        put("datasetsPrefix", datasetsPrefix);
        put("datasetsTs", datasetsTs);
        put("identifiersEzidMade", identifiersEzidMade);
        put("identifiersTs", identifiersTs);
        put("rights", rights);
        return map;
    }

    public URI getResolutionTarget() {
        return webAddress;
    }

    public URI getMetadataTarget() throws URISyntaxException {
        // if (sourceID != null)
        //     return new URI(resolverMetadataPrefix + identifier + sourceID);
        // else
        return new URI(resolverMetadataPrefix + identifier);

    }

    private void put(String key, String val) {
        if (val != null)
            map.put(key, val);
    }

    private void put(String key, Boolean val) {
        if (val != null)
            map.put(key, val.toString());
    }

    private void put(String key, URI val) {
        if (val != null) {
            map.put(key, val.toString());
        }
    }

    public String getGraph() {
        return graph;
    }
    public Boolean getDatasetsSuffixPassthrough() {
        return datasetsSuffixPassthrough;
    }
}

