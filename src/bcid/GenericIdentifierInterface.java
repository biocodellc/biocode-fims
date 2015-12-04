package bcid;

import java.util.HashMap;

/**
 * All identifiers need to have a getMetadata function
 */
public interface GenericIdentifierInterface {

    /**
     * getMetadata returns a HashMap of any class variables we want to expose to rendering classes
     * @return
     */
    public HashMap<String, String> getMetadata();
}
