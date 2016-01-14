package utils;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import biocode.fims.settings.SettingsManager;


/**
 * Fetch publicly availableProjects from the BCID system
 */
public class AvailableProjectsFetcher {
//    String projectServiceURL = "http://biscicol.org/id/projectService/list";
    ArrayList<AvailableProject> availableProjects = new ArrayList<AvailableProject>();

    /**
     * Constructor parses the projectServiceURL and builds an array of availableProjects
     */
    public AvailableProjectsFetcher() {
        JSONParser parser = new JSONParser();
        try {
            SettingsManager sm = SettingsManager.getInstance();
            String project_service_uri = sm.retrieveValue("project_service_uri");

            Object obj = parser.parse(readJsonFromUrl(project_service_uri));
            JSONObject jsonObject = (JSONObject) obj;

            // loop array
            JSONArray msg = (JSONArray) jsonObject.get("projects");
            Iterator<JSONObject> iterator = msg.iterator();
            while (iterator.hasNext()) {
                availableProjects.add(new AvailableProject(iterator.next()));
            }

        } catch (ParseException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    public AvailableProject getProject(Integer projectId) {
       Iterator it = availableProjects.iterator();
        while (it.hasNext())  {
            AvailableProject availableProject = (AvailableProject)it.next();
            if ( Integer.parseInt(availableProject.getProjectId()) == projectId)
                return availableProject;
        }
        return null;
    }
    /**
     * Get an arraylist of availableProjects and their associated data
     * @return
     */
    public ArrayList<AvailableProject> getAvailableProjects() {
        return availableProjects;
    }

    public static String readJsonFromUrl(String url) {
        try {
            InputStream is = new URL(url).openStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String json = org.apache.commons.io.IOUtils.toString(br);

            is.close();
            return json;
        } catch(IOException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    public static void main (String[] args) throws Exception {
      /*  bcidConnector connector = new bcidConnector();
        connector.authenticate("demo","demo");
        AvailableProjectsFetcher fetcher = new AvailableProjectsFetcher(connector);
        */
        AvailableProjectsFetcher fetcher = new AvailableProjectsFetcher();
        AvailableProject aP = fetcher.getProject(1);
        System.out.println(aP.getValidationXml() + "\n" +
                aP.getProjectTitle() + "\n" +
                aP.getProjectCode());


        /*Iterator it = fetcher.getAvailableProjects().iterator();
        while (it.hasNext()) {
            AvailableProject a = (AvailableProject) it.next();
            System.out.println(a.getProjectCode());
        }
        */
    }

}
