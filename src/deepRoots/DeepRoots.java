package deepRoots;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A class to manage deep Roots, storing metadata about the links and information regarding concepts and roots for
 * each deep link specified.  Deep Roots is not meant to be directly associated with any particular semantic web
 * technology.
 */
public class DeepRoots {
    private HashMap<URI, String> data = new HashMap<URI, String>();
    private String shortName;
    private String description;
    private String guid;
    private String date;

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
     * @param conceptAlias defines the alias to narrow this,  a one-word reference denoting a BCID
     * @return returns the Bcid for this conceptAlias in this DeepRoots file
     */
    public String lookupPrefix(String conceptAlias) {
        Iterator it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            if (pairs.getKey().equals(conceptAlias)) {
                return (String) pairs.getValue();
            }
        }
        return null;
    }
}
