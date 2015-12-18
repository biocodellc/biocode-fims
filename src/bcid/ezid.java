package bcid;

import java.util.HashMap;

/**
 * Represent an ezid bcid in our system
 */
public class ezid extends bcid {

    private HashMap<String,String> map;
    public ezid(HashMap<String,String> map) {
        this.map = map;
    }

    public HashMap<String, String> getMetadata() {
        // TODO: sort using treemap but convert back to Hashmap??
        //TreeMap<String, String> treeMap = new TreeMap<String, String>(map);
        return map;
    }
}
