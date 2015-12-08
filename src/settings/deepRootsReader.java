package settings;

import fimsExceptions.FIMSRuntimeException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
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
public class deepRootsReader {

    private static Logger logger = LoggerFactory.getLogger(deepRootsReader.class);

    public deepRoots createRootData(Integer user_id, Integer project_id, String expedition_code) {
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        String deeproots_uri = sm.retrieveValue("deeproots_uri");

//        String url = "http://biscicol.org:8080/id/expeditionService/deepRoots/" + project_id + "/" + expedition_code;
        String url = deeproots_uri + project_id + "/" + expedition_code;

        try {
            // Read file into String variable
            String json = readFile(new URL(url));
            // Create the deepLinks.rootData Class
            deepRoots rootData = new deepRoots(user_id, project_id, expedition_code);
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
                        String prefix = (String) dataObject.get("prefix");
                        //data.put(concept, prefix);
                        data.put(alias, prefix);
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
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        } catch (URISyntaxException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

    /**
     * Read the file and return as a String representation
     *
     * @param url
     * @return
     */
    protected String readFile(URL url) {
        String everything;
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(is));
        try {

            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }
            everything = sb.toString();
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.warn("IOException", e);
            }
        }
        return everything;
    }

    /**
     * Main method used for local testing
     *
     * @param args
     */
    public static void main(String[] args) {
        deepRootsReader reader = new deepRootsReader();
        // Some path name to the file
        String filePath = "file:///Users/jdeck/IdeaExpeditions/bcid/src/deepRoots/encodeURIcomponent.json";
        // Creating the object
        deepRoots rootData = reader.createRootData(null, 1, filePath);
        // Output for testing
        System.out.println(rootData.toString());
    }

}
