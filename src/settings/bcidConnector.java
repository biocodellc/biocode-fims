package settings;

import digester.Entity;
import digester.Mapping;
import net.sf.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import run.processController;
import utils.SettingsManager;

import java.io.*;
import java.net.*;
import java.net.CookieManager;
import java.nio.charset.Charset;
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

    /*    private String authentication_uri = "http://biscicol.org:8080/id/authenticationService/login";
        private String ark_creation_uri = "http://biscicol.org:8080/id/groupService";
        private String associate_uri = "http://biscicol.org:8080/id/expeditionService/associate";
        private String expedition_creation_uri = "http://biscicol.org:8080/id/expeditionService";
        private String expedition_validation_uri = "http://biscicol.org:8080/id/expeditionService/validateExpedition/";
        private String available_projects_uri = "http://biscicol.org:8080/id/projectService/listUserProjects";
    */
    private String authentication_uri;
    private String ark_creation_uri;
    private String associate_uri;
    private String expedition_creation_uri;
    private String expedition_validation_uri;
    private String available_projects_uri;
    private String client_id;
    private String client_secret;
    private String refresh_uri;
    private String project_service_uri;
    private String expedition_public_status_uri;
    private Integer naan;
    private Boolean ignore_user;

    private Integer responseCode;
    private String accessToken;
    private String refreshToken;
    private Boolean refreshedToken = false;
    private Boolean triedToRefreshToken = false;

    private String connectionPoint;
    private String username;
    private String password;
    SettingsManager sm = SettingsManager.getInstance();

    public static void main(String[] args) {

        String post = "client_id=ThER8RQBsXfHptrjbHaS" +
                "&" +
                "client_secret=kgHyKWTx6TA5qyR7Q9aXZ2NFWnhxXR-g9U2zpeQU8djZG5tn9ZYTh7Cv5xk977hnwpK6SdfpuGG" +
                "&" +
                "code=!9M-KGU-UCwtuUKKhPJz" +
                "&" +
                "redirect_uri=http://biscicol.org/biocode-fims/rest/authenticationService/access_token/";
        bcidConnector bcid = new bcidConnector();
        try {
            //http://biscicol.org/id/authenticationService/oauth/access_token
            String results = bcid.createPOSTConnnection(new URL("http://biscicol.org/id/authenticationService/oauth/access_token"), post);
            System.out.println(results);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        /*

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
        */
    }

    /**
     * the constructor calls the CookieManager, used while the instantiated object is alive for authentication
     */
    public bcidConnector() {
        // make sure cookies is turn on
        CookieHandler.setDefault(new CookieManager());
        setProperties();
    }

    /**
     * this constructor is used when the user has authenticated via oauth.
     *
     * @param accessToken
     */
    public bcidConnector(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        CookieHandler.setDefault(new CookieManager());
        setProperties();
    }

    public void setProperties() {
        sm.loadProperties();

        authentication_uri = sm.retrieveValue("authentication_uri");
        ark_creation_uri = sm.retrieveValue("ark_creation_uri");
        associate_uri = sm.retrieveValue("associate_uri");
        expedition_creation_uri = sm.retrieveValue("expedition_creation_uri");
        expedition_validation_uri = sm.retrieveValue("expedition_validation_uri");
        expedition_public_status_uri = sm.retrieveValue("expedition_public_status_uri");
        available_projects_uri = sm.retrieveValue("available_projects_uri");
        project_service_uri = sm.retrieveValue("project_service_uri");
        client_id = sm.retrieveValue("client_id");
        client_secret = sm.retrieveValue("client_secret");
        refresh_uri = sm.retrieveValue("refresh_uri");
        naan = Integer.parseInt(sm.retrieveValue("naan"));
        ignore_user = Boolean.parseBoolean(sm.retrieveValue("ignore_user"));
        if (ignore_user == null) ignore_user = false;

        //trust_store = sm.retrieveValue("trust_store");
        //ls
        // trust_store_password = sm.retrieveValue("trust_store_password");

        // The following System properties are set to direct the Java-specific connection here
        // to the appropriate keystore location on the server... The keystore stores the
        // BCID certificates that have been installed.  Without an SSL certificate or a non-HTTPS
        // connection this can be safely ignored
        // System.setProperty("javax.net.ssl.trustStore", trust_store);
        // System.setProperty("javax.net.ssl.trustStorePassword", trust_store_password);
    }

    /**
     * Return the BCID NAAN that we expect for projects in this FIMS
     *
     * @return
     */
    public Integer getNAAN() {
        return naan;
    }

    /**
     * Authenticate against BCID system.  This is done first to set cookies in this class in the cookies class
     * variable,
     * unless the user authenticated via OAuth, then this method is not needed.
     *
     * @param username
     * @param password
     *
     * @return
     */
    public boolean authenticate(String username, String password) {
        this.username = username;
        this.password = password;
        String postParams = "username=" + username + "&password=" + password;
        try {
            URL url = new URL(authentication_uri);
            createPOSTConnnection(url, postParams);
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException("malformed url: " + authentication_uri, 500, e);
        } catch (BCIDConnectorException e) {
            return false;
        }

        return true;
    }

    public Boolean getRefreshedToken() {
        return this.refreshedToken;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getRefreshToken() {
        return this.refreshToken;
    }

    /**
     * Obtain a new access token from the BCID system in order to continue the upload process.
     */
    private void getValidAccessToken() {
        this.triedToRefreshToken = true;

        String params = "client_id=" + client_id + "&" +
                "client_secret=" + client_secret + "&" +
                "refresh_token=" + refreshToken;

        try {
            JSONObject tokenJSON = (JSONObject) JSONValue.parse(createPOSTConnnection(new URL(refresh_uri), params));

            if (tokenJSON.containsKey("usrMessage")) {
                //TODO if usrMessage = 'invalid_grant' then the user needs to re-login
                throw new BCIDConnectorException(tokenJSON);
            }

            this.accessToken = tokenJSON.get("access_token").toString();
            this.refreshToken = tokenJSON.get("refresh_token").toString();
            this.refreshedToken = true;
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

    /**
     * Create a Dataset BCID.  Uses cookies sent during authentication method, or OAuth access tokens if accessToken !=
     * null
     * suffixPassthrough is set to False since we only want to represent a single entity here
     *
     * @return
     */
    public String createDatasetBCID(String webaddress, String graph) {
        String createBCIDDatasetPostParams =
                "title=Loaded Dataset from Biocode-FIMS&" +
                        "resourceTypesMinusDataset=1&" +
                        "suffixPassThrough=false&" +
                        "webaddress=" + webaddress;

        if (graph != null)
            createBCIDDatasetPostParams += "&graph=" + graph;

        URL url;
        try {
            if (accessToken != null) {
                url = new URL(ark_creation_uri + "?access_token=" + accessToken);
            } else {
                url = new URL(ark_creation_uri);
            }
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }
        JSONObject response = (JSONObject) JSONValue.parse(createPOSTConnnection(url, createBCIDDatasetPostParams));

        return response.get("prefix").toString();
    }

    /**
     * Create BCIDs corresponding to expedition entities
     *
     * @return
     */
    public String createEntityBCID(String webaddress, String resourceAlias, String resourceType) {
        String createBCIDDatasetPostParams =
                "title=" + resourceAlias + "&" +
                        "resourceType=" + resourceAlias + "&" +
                        "suffixPassThrough=true&" +
                        "webaddress=" + webaddress;

        URL url;
        try {
            if (accessToken != null) {
                url = new URL(ark_creation_uri + "?access_token=" + accessToken);
            } else {
                url = new URL(ark_creation_uri);
            }
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }

        JSONObject response = (JSONObject) JSONValue.parse(createPOSTConnnection(url, createBCIDDatasetPostParams));

        return response.get("prefix").toString();
    }

    /**
     * Asscociate a expedition_code to a BCID
     *
     * @return
     */
    public String associateBCID(Integer project_id, String expedition_code, String bcid) {
        String createPostParams =
                "expedition_code=" + expedition_code + "&" +
                        "project_id=" + project_id + "&" +
                        "bcid=" + bcid;

        URL url;
        try {
            url = new URL(associate_uri);
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }
        JSONObject response = (JSONObject) JSONValue.parse(createPOSTConnnection(url, createPostParams));

        return response.get("success").toString();
    }

    /**
     * List the available projects by User
     *
     * @return
     */
    public ArrayList<availableProject> listAvailableProjects() {
        ArrayList<availableProject> availableProjects = new ArrayList<availableProject>();

        String url = available_projects_uri;
        if (accessToken != null) {
            url += "?access_token=" + accessToken;
        }
        try {
            JSONObject response = (JSONObject) JSONValue.parse(createGETConnection(new URL(url)));

            // loop array
            JSONArray msg = (JSONArray) response.get("projects");
            Iterator<JSONObject> iterator = msg.iterator();
            while (iterator.hasNext()) {
                availableProjects.add(new availableProject(iterator.next()));
            }

        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }

        return availableProjects;
    }

    /**
     * Given a project_id, dataset_code, and a resource give me an ARK
     *
     * @param project_id
     * @param dataset_code
     * @param resource
     *
     * @return
     */
    public String getArkFromDataset(Integer project_id, String dataset_code, String resource) {
        //http://localhost:8080/id/expeditionService/18/DEMO4/Resource
        try {
            URL url = new URL(expedition_creation_uri + "/" + project_id + "/" + dataset_code + "/" + resource);
            JSONObject response = (JSONObject) JSONValue.parse(createGETConnection(url));
            //System.out.println("FIMS response = " + response.toString() );
            //System.out.println("ark = " + response.get("ark").toString() );
            return response.get("ark").toString();
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

    /**
     * create a expedition
     *
     * @return
     */
    public String createExpedition(String expedition_code,
                                   String expedition_title,
                                   Integer project_id) {
        String createPostParams =
                "expedition_code=" + expedition_code + "&" +
                        "expedition_title=" + expedition_title + "&" +
                        "project_id=" + project_id;

        URL url;
        try {
            if (accessToken != null) {
                url = new URL(expedition_creation_uri + "?access_token=" + accessToken);
            } else {
                url = new URL(expedition_creation_uri);
            }
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException(500, e);
        }
        JSONObject response = (JSONObject) JSONValue.parse(createPOSTConnnection(url, createPostParams));

        return response.toString();
    }

    /**
     * Call service to make this expedition public if the user wants it public
     *
     * @param publicStatus
     * @param project_id
     * @param expedition_code
     *
     * @return
     */
    public boolean setExpeditionPublicStatus(Boolean publicStatus, Integer project_id, String expedition_code) {

        String urlString = expedition_public_status_uri + project_id + "/" + expedition_code + "/" + publicStatus +
                "?access_token=" + accessToken;

        try {
            URL url = new URL(urlString);

            JSONObject response = (JSONObject) JSONValue.parse(createGETConnection(url));

            // Some error message was returned from the expedition validation service
            if (getResponseCode() != 200) {
                throw new FIMSRuntimeException(response);
            } else {
                return true;
            }
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException("malformed uri: " + urlString, 500, e);
        }
    }

    /**
     * validateExpedition ensures that this user is associated with this expedition and that the expedition code is
     * unique within
     * a particular project
     *
     * @return true if we need to insert a new expedition
     */

    public boolean checkExpedition(processController processController) {
        // if the expedition code isn't set we can just immediately return true which is
        if (processController.getExpeditionCode() == null || processController.getExpeditionCode() == "") {
            return true;
        }

        // Validate expedition at this address
        String urlString = expedition_validation_uri + processController.getProject_id() + "/" + processController.getExpeditionCode();
        // Set the accessToken if not null
        if (accessToken != null) {
            urlString += "?access_token=" + accessToken;
        }
        // Set ignore_user if it is true (this tells BCID to not run user check for expedition owner)
        if (ignore_user) {
            if (accessToken != null) urlString += "&";
            else urlString += "?";
            urlString += "ignore_user=true";
        }

        try {
            URL url = new URL(urlString);
            JSONObject response = (JSONObject) JSONValue.parse(createGETConnection(url));

            // Some error message was returned from the expedition validation service
//            if (getResponseCode() != 200) {
//                    throw new NotAuthorizedException("" +
//                            "<br>User authorization error. " +
//                            "<br>This account may not be attached to this dataset or project." +
//                            "<br>A common cause of this error is when a person other than the one generating" +
//                            "<br>the dataset code attempts to load data to that dataset.");
//                throw new FIMSRuntimeException(response);
//            } else {
            if (response.containsKey("update")) {
                return false;
            } else if (response.containsKey("insert")) {
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
//            }
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException("malformed uri: " + urlString, 500, e);
        }
    }

    public boolean createExpedition(processController processController, Mapping mapping) {
        String status = "\tCreating dataset " + processController.getExpeditionCode() + " ... this is a one time process " +
                "before loading each spreadsheet and may take a minute...\n";
        processController.appendStatus(status);
        fimsPrinter.out.println(status);
        String expedition_title = processController.getExpeditionCode() + " spreadsheet";
        if (processController.getAccessionNumber() != null) {
            expedition_title += " (accession " + processController.getAccessionNumber() + ")";
        }
        String output = createExpedition(
                processController.getExpeditionCode(),
                expedition_title,
                processController.getProject_id());
        //fimsPrinter.out.println("\t" + output);

        // Loop the mapping file and create a BCID for every entity that we specified there!
        if (mapping != null) {
            LinkedList<Entity> entities = mapping.getEntities();
            Iterator it = entities.iterator();
            while (it.hasNext()) {
                Entity entity = (Entity) it.next();

                String s = "\t\tCreating identifier root for " + entity.getConceptAlias() + " and resource type = " + entity.getConceptURI() + "\n";
                processController.appendStatus(s);
                fimsPrinter.out.println(s);
                // Create the entity BCID
                String bcid = createEntityBCID("", entity.getConceptAlias(), entity.getConceptURI());
                // Associate this identifier with this expedition
                associateBCID(processController.getProject_id(), processController.getExpeditionCode(), bcid);

            }
        }

        return true;


    }

    /**
     * method for fetching public and current user member project's from bcid system
     * @return
     */
    public String fetchProjects() {

        String urlString = project_service_uri + "?access_token=" + accessToken;

        try {
            URL url = new URL(urlString);

            String response = createGETConnection(url);

            // Some error message was returned from the expedition validation service
            if (getResponseCode() != 200) {
                throw new FIMSRuntimeException((JSONObject) JSONValue.parse(response));
            } else {
                return response;
            }
        } catch (MalformedURLException e) {
            throw new FIMSRuntimeException("malformed uri: " + urlString, 500, e);
        }

    }

    /**
     * Generic method for creating a POST connection for which to talk to BCID services
     *
     * @param url
     * @param postParams
     *
     * @return
     */
    public String createPOSTConnnection(URL url, String postParams) {

        try {
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
            //System.out.println("URL = " + url.toString());
            //System.out.println("postparams = " + postParams);
            //System.out.println("Starting getting output stream");
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            //System.out.println("Ending getting output stream");
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


            BufferedReader in;

            if (responseCode >= 400) {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            // Get the response cookies
            setCookies(conn.getHeaderFields().get("Set-Cookie"));

            if (getResponseCode() != 200) {
                // try and authenticate if needed
                if ( getResponseCode() == 401 && (accessToken != null && !triedToRefreshToken)) {
                    getValidAccessToken();
                    return createPOSTConnnection(url, postParams);
                } else {
                    try {
                        JSONObject JSONresponse = (JSONObject) JSONValue.parse(response.toString());
                        throw new BCIDConnectorException(JSONresponse);
                    // response wasn't valid json
                    } catch (NullPointerException e) {
                        throw new BCIDConnectorException(responseCode, e);
                    }
                }
            }

            return response.toString();
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

    /**
     * Custom BCID GET connection example
     *
     * @param url
     *
     * @return
     */
    public String createGETConnection(URL url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();


            // default is GET
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            /*
        // act like a browser
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        if (cookies != null) {
            for (String cookie : this.cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
            }
        }    */

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
            //conn.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

            conn.setDoOutput(true);
            conn.setDoInput(true);


            responseCode = conn.getResponseCode();
            //System.out.println("\nSending 'GET' request to URL : " + arkCreationURL);

            //System.out.println("Response Code : " + responseCode);

            BufferedReader in;

            if (responseCode >= 400) {
                in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            }
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Get the response cookies
            setCookies(conn.getHeaderFields().get("Set-Cookie"));

            if (getResponseCode() != 200) {
                // try and authenticate if needed
                if ( getResponseCode() == 401 && (accessToken != null && !triedToRefreshToken)) {
                    getValidAccessToken();
                    return createGETConnection(url);
                } else {
                    try {
                        JSONObject JSONresponse = (JSONObject) JSONValue.parse(response.toString());
                        throw new BCIDConnectorException(JSONresponse);
                        // response wasn't valid json
                    } catch (NullPointerException e) {
                        throw new FIMSRuntimeException(responseCode, e);
                    }
                }
            }
            return response.toString();
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
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
