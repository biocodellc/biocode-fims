package rest;

import utils.stringGenerator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import settings.bcidConnector;
import utils.SettingsManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * Created by rjewing on 4/12/14.
 */
@Path("authenticationService")
public class authenticationService {

    /**
     * Rest service that will initiate the oauth login process
     *
     * @param response
     * @param request
     *
     * @throws IOException
     */
    @GET
    @Path("login")
    public void login(@Context HttpServletResponse response,
                      @Context HttpServletRequest request) throws IOException {

        // Initialize settings
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If the redirect_uri contains a "www" on the beginning of the hostname, and the incoming request
        // to this login service does not contain a "www" in the hostname, then we want to call this service
        // again with a "www" inserted.
        // This is done because client browsers can add/remove the "www" on requests without predictability, and
        // which consequently means sessions are not recognized across redirects, and creating unusual behaviour
        // in the login process.
        String redirect_uri = sm.retrieveValue("redirect_uri");
        System.out.println("Biocode-FIMS redirect_uri = " + redirect_uri);

        // Pattern match on the redirect_uri to see if it contains a "www", and if so, then we need to check incomingURL
        if (Pattern.compile(Pattern.quote(redirect_uri), Pattern.CASE_INSENSITIVE).matcher("www").find()) {
            // This is the current incomingUrl
            URL incomingUrl = new URL(request.getRequestURI().toString());
            System.out.println("Biocode-FIMS incomingURL = " + incomingUrl);

            // Pattern match incomingURL to see if it contains a "www"
            if (!Pattern.compile(Pattern.quote(incomingUrl.getHost()), Pattern.CASE_INSENSITIVE).matcher("www").find()) {
                String loginRedirectURL = "http://www." + incomingUrl.getHost() + incomingUrl.getPath();
                System.out.println("Biocode-FIMS Login Redirecting to " + loginRedirectURL);
                response.sendRedirect(loginRedirectURL);
                return;
            }
        }

        // Prevent requests from forgery attacks by setting a state string
        stringGenerator sg = new stringGenerator();
        String state = sg.generateString(20);

        // Set the oauthState
        HttpSession session = request.getSession(true);
        session.setAttribute("oauth_state", state);

        // Debugging
        System.out.println("FIMS SESS_DEBUG login: sessionid=" + session.getId() + ";state=" + URLEncoder.encode(state,"utf-8"));

        // Redirect to BCID Login Service
        response.sendRedirect(sm.retrieveValue("authorize_uri") +
                "client_id=" + sm.retrieveValue("client_id") +
                "&redirect_uri=" + sm.retrieveValue("redirect_uri") +
                "&state=" + URLEncoder.encode(state, "utf-8")
        );

        return;
    }

    /**
     * first, exchange the returned oauth code for an access token. Then use the access token to obtain the user's
     * profile information, and store the username, user id, access token, and refresh token in the session.
     *
     * @param code
     * @param response
     * @param request
     *
     * @throws IOException
     */
    @GET
    @Path("access_token")
    @Consumes(MediaType.TEXT_HTML)
    public void access_token(@QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @Context HttpServletResponse response,
                             @Context HttpServletRequest request) throws IOException {

        // Initialize Settings
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

        URL url = new URL(sm.retrieveValue("access_token_uri"));
        String profileURL = sm.retrieveValue("profile_uri");
        HttpSession session = request.getSession();

        if (session != null) {
            if (state != null) {
                System.out.println("FIMS SESS_DEBUG access_token: sessionid=" + session.getId() + ";state=" + state);
            } else {
                System.out.println("FIMS SESS_DEBUG access_token: sessionid=" + session.getId() + ";state=NULL");
            }
        } else {
            System.out.println("FIMS SESS_DEBUG access_token: session is null!");
        }

        bcidConnector bcidConnector = new bcidConnector();
        String oauthState = null;
        try {
            oauthState = session.getAttribute("oauth_state").toString();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Authentication Error, session id " + session.getId() + " does not have a declared oauth_state");
            response.sendRedirect("/biocode-fims/index.jsp?error=invalid+sessionid.");
            return;
        }

        //
        if (oauthState == null) {
            System.out.println("Authentication Error, oauth state is null");
            response.sendRedirect("/biocode-fims/index.jsp?error=oauth state is null, there is an issue with the session.");
            return;
        }

        if (code == null || state == null || !state.equals(oauthState)) {
            System.out.println("Authentication Error, code or state is null");
            response.sendRedirect("/biocode-fims/index.jsp?error=Code or state is null.");
            return;
        }

        String postParams = "client_id=" + sm.retrieveValue("client_id") + "&client_secret=" + sm.retrieveValue("client_secret") +
                "&code=" + code + "&redirect_uri=" + sm.retrieveValue("redirect_uri");

        JSONObject tokenJSON = (JSONObject) JSONValue.parse(bcidConnector.createPOSTConnnection(url, postParams));

        if (tokenJSON.containsKey("error") || (tokenJSON.containsKey("state") && !tokenJSON.get("state").equals(oauthState))) {
            System.out.println("Authentication Error, here is the returned token string = " + tokenJSON.toString());
            if (tokenJSON.containsKey("error")) {
                response.sendRedirect("/biocode-fims/index.jsp?error=" + tokenJSON.get("error"));
                return;
            } else {
                response.sendRedirect("/biocode-fims/index.jsp?error=Returned state variable was not the same. Was the request/response hacked?");
            }
        }

        String access_token = tokenJSON.get("access_token").toString();

        JSONObject profileJSON = (JSONObject) JSONValue.parse(bcidConnector.createGETConnection(new URL(profileURL + access_token)));

        if (profileJSON.containsKey("error")) {
            response.sendRedirect("/biocode-fims/index.jsp?error=" + profileJSON.get("error"));
            return;
        }

        session.setAttribute("user", profileJSON.get("username"));
        session.setAttribute("userId", profileJSON.get("user_id"));
        session.setAttribute("access_token", access_token);
        session.setAttribute("refresh_token", tokenJSON.get("refresh_token").toString());

        response.sendRedirect("/biocode-fims/index.jsp");
        return;
    }

    /**
     * Rest service to log a user out of the biocode-fims system
     *
     * @param req
     * @param res
     *
     * @throws IOException
     */
    @GET
    @Path("logout")

    public void logout(@Context HttpServletRequest req,
                       @Context HttpServletResponse res)
            throws IOException {
        ////@Produces(MediaType.TEXT_HTML)
        HttpSession session = req.getSession(true);

        // Invalidate the session for Biocode FIMS
        session.invalidate();
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Need to also logout of the BCID system
        res.sendRedirect(sm.retrieveValue("logout_uri"));

        //res.sendRedirect("/biocode-fims/index.jsp");
        return;
    }
}
