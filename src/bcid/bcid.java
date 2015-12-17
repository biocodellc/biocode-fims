package bcid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

/**
 * The bcid class encapsulates all of the information we know about a BCID.
 * This includes data such as the
 * status of EZID creation, associated bcid calls, and any metadata.
 * It can include a data element or a data group.
 * There are several ways to construct an element, including creating it from scratch, or instantiating by looking
 * up an existing identifier from the database.
 */
public class bcid extends GenericIdentifier {
    protected URI webAddress = null;        // URI for the webAddress, EZID calls this _target (e.g. http://biocode.berkeley.edu/specimens/MBIO56)
    protected String suffix = null;       // Source or local identifier (e.g. MBIO056)
    protected String what = null;           // erc.what
    protected String when = null;           // erc.when
    protected String who = null;            // erc.who
    protected String title = null;            // erc.who\
    public String projectCode = null;
    protected Boolean bcidsEzidMade;
    protected Boolean bcidsEzidRequest;
    protected String bcidsPrefix;
    protected String bcidsTs;
    protected Boolean identifiersEzidRequest;
    protected Boolean identifiersEzidMade;
    protected Boolean bcidsSuffixPassthrough;
    protected String identifiersTs;
    //protected String ark;
    protected String doi;
    protected Integer bcidsId;
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
     * @param bcidsId
     */
    public bcid(Integer bcidsId) {
        bcidMinter bcidMinter = setBcidsId(bcidsId);

        bcidMinter.close();
    }


    /**
     * Create an element given a source identifier, and a resource type identifier
     *
     * @param suffix
     * @param bcidsId
     */
    public bcid(String suffix, Integer bcidsId) {
        this.suffix = suffix;
        bcidMinter bcidMinter = setBcidsId(bcidsId);
        projectCode = bcidMinter.getProject(bcidsId);
        bcidMinter.close();
    }

    /**
     * Create an element given a source identifier, web address for resolution, and a bcidsId
     * This method is meant for CREATING bcids.
     *
     * @param suffix
     * @param webAddress
     * @param bcidsId
     */
    public bcid(String suffix, URI webAddress, Integer bcidsId) {
        this.suffix = suffix;
        bcidMinter bcidMinter = setBcidsId(bcidsId);
        this.webAddress = webAddress;

        // Reformat webAddress in this constructor if there is a suffix
        if (suffix != null && webAddress != null && !suffix.toString().trim().equals("") && !webAddress.toString().trim().equals("")) {
            //System.out.println("HERE" + webAddress);
            try {
                this.webAddress = new URI(webAddress + suffix);
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException for uri: {}", webAddress + suffix, e);
            }
        }
        bcidMinter.close();
    }


    /**
     * Internal function for setting the source ID (local identifier that has been passed in)
     *
     * @param suffix
     */
    private void setSuffix(String suffix, bcidMinter bcidMinter) {
        try {
            if (suffix != null && !suffix.equals("")) {
                identifier = new URI(bcidMinter.identifier + sm.retrieveValue("divider") + suffix);
            } else {
                identifier = bcidMinter.identifier;
            }
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException thrown", e);
        }

        // Reformat webAddress in this constructor if there is a suffix
        if (suffix != null && webAddress != null && !suffix.toString().trim().equals("") && !webAddress.toString().trim().equals("")) {
            //System.out.println("HERE" + webAddress);
            try {
                this.webAddress = new URI(webAddress + suffix);
            } catch (URISyntaxException e) {
                //TODO should we silence this exception?
                logger.warn("URISyntaxException thrown", e);
            }
        }
    }

    /**
     * Internal functional for setting the bcids_id that has been passed in
     *
     * @param pBcidsId
     */
    private bcidMinter setBcidsId (Integer pBcidsId) {

        // Create a bcid representation based on the bcidsId
        bcidMinter bcidMinter = new bcidMinter(pBcidsId);
        //when =  new dates().now();
        when = bcidMinter.ts;

        this.graph = bcidMinter.getGraph();
        this.webAddress = bcidMinter.getWebAddress();
        this.bcidsId = pBcidsId;
        this.what = bcidMinter.getResourceType();
        this.title = bcidMinter.title;
        this.projectCode = bcidMinter.projectCode;
        this.bcidsTs = bcidMinter.ts;
        this.bcidsPrefix = bcidMinter.getPrefix();
        this.doi = bcidMinter.doi;
        this.level = this.UNREGISTERED_ELEMENT;
        this.who = bcidMinter.who;
        identifiersEzidRequest = false;
        identifiersEzidMade = false;
        bcidsEzidMade = bcidMinter.ezidMade;
        bcidsEzidRequest = bcidMinter.ezidRequest;
        bcidsSuffixPassthrough = bcidMinter.getSuffixPassThrough();

        try {
            if (suffix != null && !suffix.equals("")) {
                identifier = new URI(bcidMinter.identifier + sm.retrieveValue("divider") + suffix);
            } else {
                identifier = bcidMinter.identifier;
            }
            projectCode = bcidMinter.getProject(bcidsId);
        } catch (URISyntaxException e) {
            //TODO should we silence this exception?
            logger.warn("URISyntaxException for uri: {}", bcidMinter.identifier + sm.retrieveValue("divider") + suffix, e);
        }

        return bcidMinter;

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
        put("suffix", suffix);
        put("doi", doi);
        put("bcidsEzidMade", bcidsEzidMade);
        put("bcidsSuffixPassThrough", bcidsSuffixPassthrough);
        put("bcidsEzidRequest", bcidsEzidRequest);
        put("bcidsPrefix", bcidsPrefix);
        put("bcidsTs", bcidsTs);
        put("identifiersEzidMade", identifiersEzidMade);
        put("identifiersTs", identifiersTs);
        put("rights", rights);
        return map;
    }

    public URI getWebAddress() {
        return webAddress;
    }

    public URI getMetadataTarget() throws URISyntaxException {
        // if (suffix != null)
        //     return new URI(resolverMetadataPrefix + identifier + suffix);
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
    public Boolean getBcidsSuffixPassthrough() {
        return bcidsSuffixPassthrough;
    }
}

