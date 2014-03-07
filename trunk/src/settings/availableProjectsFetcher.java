package settings;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import net.sf.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * Fetch availableProjects from the BCID system
 */
public class availableProjectsFetcher {
    String projectServiceURL = "http://biscicol.org/id/projectService/list";
    ArrayList<availableProject> availableProjects = new ArrayList<availableProject>();

    /**
     * Constructor parses the projectServiceURL and builds an array of availableProjects
     */
    public availableProjectsFetcher() {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(readJsonFromUrl(projectServiceURL));
            JSONObject jsonObject = (JSONObject) obj;

            // loop array
            JSONArray msg = (JSONArray) jsonObject.get("projects");
            Iterator<JSONObject> iterator = msg.iterator();
            while (iterator.hasNext()) {
                availableProjects.add(new availableProject(iterator.next()));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get an arraylist of availableProjects and their associated data
     * @return
     */
    public ArrayList<availableProject> getAvailableProjects() {
        return availableProjects;
    }

    public static String readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String json = org.apache.commons.io.IOUtils.toString(br);

        is.close();
        return json;
    }
    public static void main (String[] args) {
        availableProjectsFetcher fetcher = new availableProjectsFetcher();
        Iterator it = fetcher.getAvailableProjects().iterator();
        while (it.hasNext()) {
            availableProject a = (availableProject) it.next();
            System.out.println(a.getProject_code());
        }
    }

}
