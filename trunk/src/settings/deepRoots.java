package settings;

import digester.Entity;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A class to manage deep Roots, storing metadata about the links and information regarding concepts and roots for
 * each deep link specified.  Deep Roots is not meant to be directly associated with any particular semantic web
 * technology.
 */
public class deepRoots {
    private HashMap<URI, String> data = new HashMap<URI, String>();
    private String shortName;
    private String description;
    private String guid;
    private String date;
    private Integer expedition_id;
    private  String project_code;
    private bcidConnector bcidConnector;
    public deepRoots(bcidConnector bcidConnector, Integer expedition_id, String project_code) {
        this.expedition_id = expedition_id;
        this.project_code = project_code;
        this.bcidConnector = bcidConnector;
    }

    /**
     * stores the links between the concept (as URI) and prefix (as String)
     *
     * @return
     */
    public HashMap<URI, String> getData() {
        return data;
    }

    /**
     * sets the links between the concept (as URI) and prefix (as String)
     *
     * @param data
     */
    public void setData(HashMap<URI, String> data) {
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
     * Find the appropriate prefix for a concept contained in this file
     *
     * @return returns the identifier for this conceptAlias in this DeepRoots file
     */
    public String lookupPrefix(Entity entity) throws Exception {
        // when viewing in graphviz.
        //String prefixRoot = "http://biscicol.org/id/metadata/";
        Iterator it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (pairs.getKey().toString().trim().equals(entity.getConceptURI().trim())) {
                String postfix =  (String) pairs.getValue();
                //return prefixRoot + postfix;
                return postfix;
            }
        }
        fimsPrinter.out.println("\tWarning: " + entity.getConceptURI() + " cannot be mapped in Deep Roots, attempting to create mapping");

        // Create a mapping in the deeproots system for this URI
        try {
            fimsPrinter.out.println("\tCreating identifier root for " + entity.getConceptAlias() + " with resource type = " + entity.getConceptURI());
            // Create the entity BCID
            String bcid = bcidConnector.createEntityBCID("", entity.getConceptAlias(), entity.getConceptURI());
            // Associate this identifier with this project
            bcidConnector.associateBCID(expedition_id, project_code, bcid);

            // Add this element to the data string so we don't keep trying to add it in the loop above
            data.put(new URI(entity.getConceptURI()),entity.getConceptAlias());
        } catch (Exception e) {
            //e.printStackTrace();
            fimsPrinter.out.println("\tUnable to map  " + entity.getConceptURI() + " -- using default namespace!");
            return null;
        } finally {
            return "something";
        }

    }
}
