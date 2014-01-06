package settings;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public deepRoots createRootData(String url) throws IOException, URISyntaxException {
        // Read file into String variable
        String json = readFile(new URL(url));
        // Create the deepLinks.rootData Class
        deepRoots rootData = new deepRoots();
        // Create the Hashmap to store in the deepLinks.rootData class
        HashMap<java.net.URI, String> data = new HashMap<java.net.URI, String>();
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
                    String prefix = (String) dataObject.get("prefix");
                    data.put(concept, prefix);
                }
            } else if (outputObject.containsKey("metadata")) {
                JSONObject metadataObject = (JSONObject) outputObject.values().toArray()[0];
                rootData.setGuid((String) metadataObject.get("guid"));
                rootData.setDescription((String) metadataObject.get("description"));
                rootData.setDate((String) metadataObject.get("date"));
                rootData.setShortName((String) metadataObject.get("name"));
            }
        }

        // Assign the actual data to the deepLinks.rootData element
        rootData.setData(data);

        return rootData;
    }

    /**
     * Read the file and return as a String representation
     *
     * @param url
     * @return
     * @throws java.io.IOException
     */
    protected String readFile(URL url) throws IOException {
        String everything;
        BufferedReader br = new BufferedReader(
                new InputStreamReader(url.openStream()));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }
            everything = sb.toString();
        } finally {
            br.close();
        }
        return everything;
    }

    /**
     * Main method used for local testing
     *
     * @param args
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     */
    public static void main(String[] args) throws IOException, URISyntaxException {
        deepRootsReader reader = new deepRootsReader();
        // Some path name to the file
        String filePath = "file:///Users/jdeck/IdeaProjects/bcid/src/deepRoots/test.json";
        // Creating the object
        deepRoots rootData = reader.createRootData(filePath);
        // Output for testing
        System.out.println(rootData.toString());
    }

}
