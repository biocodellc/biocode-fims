package settings;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.CookieManager;
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
    private String authenticationURL = "http://biscicol.org/id/loginService";

    private String arkCreationURL = "http://biscicol.org/id/groupService";
    private String associateURL = "http://biscicol.org/id/projectService/associate";
    private String projectCreationURL = "http://biscicol.org/id/projectService";
    private String projectValidationURL = "http://biscicol.org/id/projectService/validateProject/";

    private Integer responseCode;

    private String connectionPoint;
    private String username;
    private String password;

    public static void main(String[] args) {

        bcidConnector bcid = new bcidConnector();
        String message = null;

        // Authenticate
        boolean success = false;
        try {
            success = bcid.authenticate("biocode", "biocode2013");
            if (success)
                System.out.println("Able to authenticate!");
        } catch (Exception e) {
            message = e.getMessage();
            e.printStackTrace();
        }
        // TESTING user-project authentication
        try {
            bcid.validateProject("DEMOH", 1);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        /*
       // Success then create ARK
       if (success)
           try {
               message = "Created BCID = " + bcid.createDatasetBCID(null);
           } catch (Exception e) {
               message = e.getMessage();
               e.printStackTrace();
           }
       else
           message = "Unable to authenticate";

       try {

           System.out.println(bcid.associateBCID("DEMOH", "ark:/21547/Fu2"));
       } catch (Exception e) {
           e.printStackTrace();
       }

       System.out.println(message);
        */
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
        //String postParams = "j_username=" + username + "&j_password=" + password;
        String postParams = "username=" + username + "&password=" + password;
        URL url = new URL(authenticationURL);
        String response = createPOSTConnnection(url, postParams);

        // TODO: use a proper method for determining response/error codes (like HTTP Response code = 401 or 403)!
        if (response.toString().contains("Bad credentials")) {
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
            throw new Exception("User not authorized to upload to this project!");
        }
        return response.toString();
    }

    /**
     * Create a Dataset BCID.  Uses cookies sent during authentication method.  suffixPassthrough is set to False
     * since we only want to represent a single entity here
     *
     * @return
     * @throws Exception
     */
    public String createBCID(String webaddress, String title, Integer resourceTypesMinusDataset) throws Exception {
        String createBCIDDatasetPostParams =
                "title=" + title + "&" +
                        "resourceTypesMinusDataset=" + resourceTypesMinusDataset + "&" +
                        "suffixPassThrough=true&" +
                        "webaddress=" + webaddress;

        URL url = new URL(arkCreationURL);
        String response = createPOSTConnnection(url, createBCIDDatasetPostParams);

        return response.toString();
    }

    /**
     * Asscociate a project_code to a BCID
     *
     * @return
     * @throws Exception
     */
    public String associateBCID(String project_code, String bcid) throws Exception {
        String createPostParams =
                "project_code=" + project_code + "&" +
                        "bcid=" + bcid;

        URL url = new URL(associateURL);
        String response = createPOSTConnnection(url, createPostParams);

        return response.toString();
    }

    /**
     * Asscociate a project_code to a BCID
     *
     * @return
     * @throws Exception
     */
    public String createProject(String project_code, 
                                String project_title, 
                                String abstractString, 
                                Integer expedition_id) throws Exception {
        String createPostParams =
                "project_code=" + project_code + "&" +
                        "project_title=" + project_title + "&" +
                        "abstract=" + abstractString + "&" +
                        "expedition_id=" + expedition_id;

        URL url = new URL(projectCreationURL);
        String response = createPOSTConnnection(url, createPostParams);

        // Catch Error using response string...
        // TODO: use response code formats here
        if (response.contains("ERROR")) {
            throw new Exception(response.toString());
        }
        return response.toString();
    }

    /**
     * validateProject ensures that this user is associated with this project and that the project code is unique within
     * a particular expedition
     *
     * @param project_code
     * @return
     * @throws Exception
     */
    public boolean validateProject(String project_code, Integer expedition_id) throws Exception {
        URL url = new URL(projectValidationURL + expedition_id + "/" + project_code);
        String response = createGETConnection(url);
        String action = response.split(":")[0];
        if (getResponseCode() != 200) {
            throw new Exception("BCID service error");
        } else {
            if (action.equals("error")) {
                throw new Exception(response);
            } else if (action.equals("update")) {
                return true;
            } else if (action.equals("insert")) {
                String message = "\nThe project code \"" + project_code + "\" does not exist.  " +
                        "Do you wish to create it now?" +
                        "\nIf you choose to continue, your data will be associated with this new project code.";
                if (fimsInputter.in.continueOperation(message)) {
                    try {
                        String output = createProject(
                                project_code,
                                project_code + " spreadsheet project",
                                null,
                                expedition_id);
                        fimsPrinter.out.println("\t" + output);
                        return true;
                    } catch (Exception e) {
                        throw new Exception("Unable to create project " + project_code + "\nPlease be sure project codes are between 4 and 6 characters in length\nand do not contain spaces or special characters.", e);
                    }

                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
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
}