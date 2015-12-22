package settings;

import bcid.ExpeditionMinter;
import fimsExceptions.FimsRuntimeException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A sample for reading a deepLinks data file in JSON.  The following libraries are required:
 * commons-beanutils-1.8.3.jar
 * commons-collections-3.2.1.jar
 * commons-lang-2.6.jar
 * commons-logging-1.1.jar
 * ezmorph-1.0.6.jar
 * json-lib-2.4-jdk15.jar
 */
public class DeepRootsReader {

    private static Logger logger = LoggerFactory.getLogger(DeepRootsReader.class);

    public DeepRoots createRootData(Integer userId, Integer projectId, String expeditionCode) {
        try {
            // Get deepLinks json object
            ExpeditionMinter expeditionMinter = new ExpeditionMinter();
            String json = expeditionMinter.getDeepRoots(expeditionCode, projectId);
            expeditionMinter.close();
            // Create the deepLinks.rootData Class
            DeepRoots rootData = new DeepRoots(userId, projectId, expeditionCode);
            // Create the Hashmap to store in the deepLinks.rootData class
            //HashMap<java.net.URI, String> data = new HashMap<java.net.URI, String>();
            HashMap<String, String> data = new HashMap<String, String>();
            // write json String into array
            JSONArray jsonOutputArray = (JSONArray) JSONSerializer.toJSON(JSONArray.fromObject(json));
            // Loop the Output
            Iterator it = jsonOutputArray.iterator();
            while (it.hasNext()) {
                // Create an outputObject for each top-level element
                JSONObject outputObject = (JSONObject) it.next();
                // Get the section dealing with "data"
                if (outputObject.containsKey("data")) {
                    JSONArray dataArray = (JSONArray) outputObject.values().toArray()[0];
                    Iterator dataIt = dataArray.iterator();
                    // Loop the data elements
                    while (dataIt.hasNext()) {
                        JSONObject dataObject = (JSONObject) dataIt.next();
                        java.net.URI concept = new java.net.URI((String) dataObject.get("concept"));
                        String alias = (String) dataObject.get("alias");
                        String identifier = (String) dataObject.get("identifier");
                        //data.put(concept, identifier);
                        data.put(alias, identifier);
                    }
                } else if (outputObject.containsKey("metadata")) {
                    JSONObject metadataObject = (JSONObject) outputObject.values().toArray()[0];
                    rootData.setGuid((String) metadataObject.get("guid"));
                    rootData.setDescription((String) metadataObject.get("description"));
                    rootData.setDate((String) metadataObject.get("date"));
                    rootData.setShortName((String) metadataObject.get("name"));
                }
            }
            rootData.setData(data);
            // Assign the actual data to the deepLinks.rootData element

            return rootData;
        } catch (URISyntaxException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    /**
     * Main method used for local testing
     *
     * @param args
     */
    public static void main(String[] args) {
        DeepRootsReader reader = new DeepRootsReader();
        // Some path name to the file
        String filePath = "file:///Users/jdeck/IdeaExpeditions/bcid/src/deepRoots/encodeURIcomponent.json";
        // Creating the object
        DeepRoots rootData = reader.createRootData(null, 1, filePath);
        // Output for testing
        System.out.println(rootData.toString());
    }

}
