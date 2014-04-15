package settings;

import digester.Attribute;
import digester.Entity;
import digester.Mapping;
import net.sf.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import run.processController;

import java.io.*;
import java.net.*;
import java.net.CookieManager;
import java.nio.charset.Charset;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * General purpose class for authenticating against BCID system and creating BCIDs.
 */
public class bcidConnector {

    private List<String> cookies;
    //private HttpURLConnection conn;
    private final String USER_AGENT = "Mozilla/5.0";
    private final String ACCEPT_LANGUAGE = "en-US,en;q=0.5";
    private final String HOST = "biscicol.org";
    private final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private final String CONNECTION = "keep-alive";

    //private String authenticationURL = "http://biscicol.org/bcid/j_spring_security_check";
    private String authenticationURL = "http://biscicol.org/id/authenticationService/login";

    private String arkCreationURL = "http://biscicol.org/id/groupService";
    private String associateURL = "http://biscicol.org/id/expeditionService/associate";
    private String expeditionCreationURL = "http://biscicol.org/id/expeditionService";
    private String expeditionValidationURL = "http://biscicol.org/id/expeditionService/validateExpedition/";
    private String availableProjectsURL = "http://biscicol.org/id/projectService/listUserProjects";


    private Integer responseCode;

    private String connectionPoint;
    private String username;
    private String password;

    public static void main(String[] args) {

        String username = "demo";
        String password = "demo";
        bcidConnector bcid = new bcidConnector();
        String message = null;

        // Authenticate
        boolean success = false;
        try {
            success = bcid.authenticate(username, password);
            if (success)
                System.out.println("Able to authenticate!");
        } catch (Exception e) {
            message = e.getMessage();
            e.printStackTrace();
        }
        // TESTING user-expedition authentication
        try {
            //bcid.validateExpedition("DEMOH", 1, null);
            Iterator it = bcid.listAvailableProjects().iterator();
            while (it.hasNext()) {
                availableProject p = (availableProject) it.next();
                System.out.println(p.getProject_title());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * the constructor calls the CookieManager, used while the instantiated object is alive for authentication
     */
    public bcidConnector() {
        // make sure cookies is turn on
        CookieHandler.setDefault(new CookieManager());
    }

    /**
     * Authenticate against BCID system.  This is done first to set cookies in this class in the cookies class variable.
     *
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    public boolean authenticate(String username, String password) throws Exception {
        this.username = username;
        this.password = password;
        String postParams = "username=" + username + "&password=" + password;
        URL url = new URL(authenticationURL);
        String response = createPOSTConnnection(url, postParams);

        // TODO: find a more robust way to search for bad credentials than just parsing the response for text
        if (response.toString().contains("Bad Credentials")) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Create a Dataset BCID.  Uses cookies sent during authentication method.  suffixPassthrough is set to False
     * since we only want to represent a single entity here
     *
     * @return
     * @throws Exception
     */
    public String createDatasetBCID(String webaddress, String graph) throws Exception {
        String createBCIDDatasetPostParams =
                "title=Loaded Dataset from Biocode-FIMS&" +
                        "resourceTypesMinusDataset=1&" +
                        "suffixPassThrough=false&" +
                        "webaddress=" + webaddress;

        if (graph != null)
            createBCIDDatasetPostParams += "&graph=" + graph;

        URL url = new URL(arkCreationURL);
        String response = createPOSTConnnection(url, createBCIDDatasetPostParams);
        if (getResponseCode() == 401) {
            throw new Exception("User not authorized to upload to this expedition!");
        }
        return response.toString();
    }

    /**
     * Create BCIDs corresponding to expedition entities
     *
     * @return
     * @throws Exception
     */
    public String createEntityBCID(String webaddress, String resourceAlias, String resourceType) throws Exception {
        String createBCIDDatasetPostParams =
                "title=" + resourceAlias + "&" +
                        "resourceType=" + resourceAlias + "&" +
                        "suffixPassThrough=true&" +
                        "webaddress=" + webaddress;

        URL url = new URL(arkCreationURL);
        String response = createPOSTConnnection(url, createBCIDDatasetPostParams);

        if (getResponseCode() == 401) {
            throw new Exception("User authorization error!");
        }
        return response.toString();
    }

    /**
     * Asscociate a expedition_code to a BCID
     *
     * @return
     * @throws Exception
     */
    public String associateBCID(Integer project_id, String expedition_code, String bcid) throws Exception {
        String createPostParams =
                "expedition_code=" + expedition_code + "&" +
                        "project_id=" + project_id + "&" +
                        "bcid=" + bcid;

        URL url = new URL(associateURL);
        String response = createPOSTConnnection(url, createPostParams);

        return response.toString();
    }

    /**
     * List the available projects by User
     *
     * @return
     * @throws Exception
     */
    public ArrayList<availableProject> listAvailableProjects() throws Exception {
        ArrayList<availableProject> availableProjects = new ArrayList<availableProject>();

        //URL url = new URL(availableProjectsURL);
        //String response = createGETConnection(url);

        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(readJsonFromUrl(availableProjectsURL));
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

        return availableProjects;
    }

    /**
     * create a expedition
     *
     * @return
     * @throws Exception
     */
    public String createExpedition(String expedition_code,
                                   String expedition_title,
                                   Integer project_id) throws Exception {
        String createPostParams =
                "expedition_code=" + expedition_code + "&" +
                        "expedition_title=" + expedition_title + "&" +
                        "project_id=" + project_id;

        URL url = new URL(expeditionCreationURL);
        String response = createPOSTConnnection(url, createPostParams);

        // Catch Error using response string...
        // TODO: use response code formats here
        if (response.contains("ERROR")) {
            throw new Exception(response.toString());
        }

        // When i create a expedition, i also want to create

        return response.toString();
    }

    /**
     * validateExpedition ensures that this user is associated with this expedition and that the expedition code is unique within
     * a particular project
     *
     * @return
     * @throws Exception
     */
    public boolean checkExpedition(processController processController) throws Exception {
        URL url = new URL(expeditionValidationURL + processController.getProject_id() + "/" + processController.getExpeditionCode());
        String response = createGETConnection(url);
        String action = response.split(":")[0];
        if (getResponseCode() != 200) {
            throw new Exception("BCID service error");
        } else {
            if (action.equals("error")) {
                throw new Exception(response);
            } else if (action.equals("update")) {
                return false;
            } else if (action.equals("insert")) {
                return true;
                /*String message = "\nThe expedition code \"" + processController.getExpeditionCode() + "\" does not exist.  " +
                        "Do you wish to create it now?" +
                        "\nIf you choose to continue, your data will be associated with this new expedition code.";
                Boolean continueOperation = fimsInputter.in.continueOperation(message);
                return continueOperation;
                */
            } else {
                return false;
            }
        }
    }

    public boolean createExpedition(processController processController, Mapping mapping) throws Exception {
        try {
            fimsPrinter.out.println("\tCreating expedition " + processController.getExpeditionCode() + " ... this is a one time process " +
                    "before loading each spreadsheet and may take a minute...");
            String output = createExpedition(
                    processController.getExpeditionCode(),
                    processController.getExpeditionCode() + " spreadsheet expedition",
                    processController.getProject_id());
            //fimsPrinter.out.println("\t" + output);
        } catch (Exception e) {
            //e.printStackTrace();
            //
            throw new Exception("Unable to create expedition " + processController.getExpeditionCode() + "\n" + e.getMessage(), e);
        }
        // Loop the mapping file and create a BCID for every entity that we specified there!
        if (mapping != null) {
            LinkedList<Entity> entities = mapping.getEntities();
            Iterator it = entities.iterator();
            while (it.hasNext()) {
                Entity entity = (Entity) it.next();
                try {
                    fimsPrinter.out.println("\t\tCreating identifier root for " + entity.getConceptAlias() + " and resource type = " + entity.getConceptURI());
                    // Create the entity BCID
                    String bcid = createEntityBCID("", entity.getConceptAlias(), entity.getConceptURI());
                    // Associate this identifier with this expedition
                    associateBCID(processController.getProject_id(), processController.getExpeditionCode(), bcid);

                } catch (Exception e) {
                    throw new Exception("The expedition " + processController.getExpeditionCode() +
                            " has been created but unable to create a BCID for\n" +
                            "resourceType = " + entity.getConceptURI(), e);
                }
            }
        }

        return true;


    }

    /**
     * Generic method for creating a POST connection for which to talk to BCID services
     *
     * @param url
     * @param postParams
     * @return
     * @throws IOException
     */
    private String createPOSTConnnection(URL url, String postParams) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // Acts like a browser
        conn.setUseCaches(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Host", HOST);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", ACCEPT);
        conn.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        if (cookies != null) {
            for (String cookie : this.cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
            }
        }

        conn.setRequestProperty("Connection", CONNECTION);
        //conn.setRequestProperty("Referer", "https://accounts.google.com/ServiceLoginAuth");
        conn.setRequestProperty("Content-Type", CONTENT_TYPE);
        conn.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

        conn.setDoOutput(true);
        conn.setDoInput(true);

        // Send post request
        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
        wr.writeBytes(postParams);
        wr.flush();
        wr.close();

        /*
        int responseCode = conn.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + authenticationURL);
        System.out.println("Post parameters : " + postParams);
        System.out.println("Response Code : " + responseCode);
        */
        responseCode = conn.getResponseCode();

        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        // Get the response cookies
        setCookies(conn.getHeaderFields().get("Set-Cookie"));
        return response.toString();
    }

    /**
     * Custom BCID GET connection example
     *
     * @param url
     * @return
     * @throws IOException
     */
    private String createGETConnection(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        // default is GET
        conn.setRequestMethod("GET");
        conn.setUseCaches(false);

        // act like a browser
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        if (cookies != null) {
            for (String cookie : this.cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
            }
        }
        responseCode = conn.getResponseCode();
        //System.out.println("\nSending 'GET' request to URL : " + arkCreationURL);

        //System.out.println("Response Code : " + responseCode);

        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Get the response cookies
        setCookies(conn.getHeaderFields().get("Set-Cookie"));
        return response.toString();
    }

    /**
     * Return Cookies
     *
     * @return
     */
    public List<String> getCookies() {
        return cookies;
    }

    /**
     * Set Cookies
     *
     * @param cookies
     */
    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    private static String readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String json = org.apache.commons.io.IOUtils.toString(br);

        is.close();
        return json;
    }

}