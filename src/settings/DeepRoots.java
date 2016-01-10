package settings;

import bcid.Bcid;
import bcid.BcidMinter;
import bcid.ExpeditionMinter;
import biocode.fims.SettingsManager;
import digester.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A class to manage deep Roots, storing metadata about the links and information regarding concepts and roots for
 * each deep link specified.  Deep Roots is not meant to be directly associated with any particular semantic web
 * technology.
 */
public class DeepRoots {
    //private HashMap<URI, String> data = new HashMap<URI, String>();
    private HashMap<String, String> data = new HashMap<String, String>();
    private String shortName;
    private String description;
    private String guid;
    private String date;
    private Integer projectId;
    private  String expeditionCode;
    private Integer userId;
    private static SettingsManager sm;

    static {
        sm = SettingsManager.getInstance();
    }

    public DeepRoots(Integer userId, Integer projectId, String expeditionCode) {
        this.projectId = projectId;
        this.expeditionCode = expeditionCode;
        this.userId = userId;
    }

    /**
     * stores the links between the concept (as URI) and identifier (as String)
     *
     * @return
     */
    public HashMap<String, String> getData() {
        return data;
    }

    /**
     * sets the links between the concept (as URI) and identifier (as String)
     *
     * @param data
     */
    public void setData(HashMap<String, String> data) {
        this.data = data;
    }

    /**
     * gets the short name describing this file
     *
     * @return
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * sets the short name describing this file
     *
     * @param shortName
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * gets the description for this file
     *
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * sets the description for this file
     *
     * @return
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Converts this object to a string representation for easy viewing
     *
     * @return
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("/**\n");
        sb.append("* name = " + shortName + "\n");
        sb.append("* description = " + description + "\n");
        sb.append("* guid = " + guid + "\n");
        sb.append("* date = " + date + "\n");
        sb.append("**/\n");
        Iterator it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            sb.append(pairs.getValue() + " a " + pairs.getKey() + " .\n");
        }
        return sb.toString();
    }

    /**
     * Find the appropriate identifier for a concept contained in this file
     *
     * @return returns the Bcid for this conceptAlias in this DeepRoots file
     */
    public String lookupPrefix(Entity entity) {
        // when viewing in graphviz.
        //String prefixRoot = "http://biscicol.org/id/metadata/";
        Iterator it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            //if (pairs.getKey().toString().trim().equals(entity.getConceptURI().trim())) {
            if (pairs.getKey().toString().trim().equals(entity.getConceptAlias().trim())) {
                String postfix =  (String) pairs.getValue();
                //return prefixRoot + postfix;
                return postfix;
            }
        }
        FimsPrinter.out.println("\tWarning: " + entity.getConceptAlias() + " cannot be mapped in Deep Roots, attempting to create mapping");
        // Create a mapping in the deeproots system for this URI
        FimsPrinter.out.println("\tCreating bcid root for " + entity.getConceptAlias() + " with resource type = " + entity.getConceptURI());
        // Mint the Bcid
        BcidMinter bcidMinter = new BcidMinter(Boolean.valueOf(sm.retrieveValue("ezidRequests")));

        String identifier = bcidMinter.createEntityBcid(new Bcid(userId, entity.getConceptAlias(), "", null, null, false, true));
        bcidMinter.close();
        // Associate this Bcid with this expedition
        ExpeditionMinter expedition = new ExpeditionMinter();
        expedition.attachReferenceToExpedition(expeditionCode, identifier, projectId);
        expedition.close();

        // Add this element to the data string so we don't keep trying to add it in the loop above
        //data.put(new URI(entity.getConceptURI()),entity.getConceptAlias());
        data.put(entity.getConceptAlias(), identifier);
        System.out.println("\tNew identifier = " + identifier);
        return identifier;
    }
}
