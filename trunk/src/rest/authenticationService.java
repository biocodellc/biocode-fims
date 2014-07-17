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

/**
 * Created by rjewing on 4/12/14.
 */
@Path("authenticationService")
public class authenticationService {

    /**
     * Rest service that will initiate the oauth login process
     * @param response
     * @param request
     * @throws IOException
     */
    @GET
    @Path("login")
    public void login(@Context HttpServletResponse response,
                      @Context HttpServletRequest request) throws IOException {
        stringGenerator sg = new stringGenerator();
        String state = sg.generateString(20);

        HttpSession session = request.getSession(true);
        session.setAttribute("oauth_state", state);

        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

System.out.println("SESS_DEBUG login: sessionid="+session.getId() +";state=" + state);

        response.sendRedirect(sm.retrieveValue("authorize_uri") +
                "client_id=" + sm.retrieveValue("client_id") +
                "&redirect_uri=" + sm.retrieveValue("redirect_uri") +
                "&state=" + state);
        return;
    }

    /**
     * first, exchange the returned oauth code for an access token. Then use the access token to obtain the user's
     * profile information, and store the username, user id, access token, and refresh token in the session.
     * @param code
     * @param response
     * @param request
     * @throws IOException
     */
    @GET
    @Path("access_token")
    @Consumes(MediaType.TEXT_HTML)
    public void access_token(@QueryParam("code") String code,
                             @QueryParam("state") String state,
                             @Context HttpServletResponse response,
                             @Context HttpServletRequest request) throws IOException {
        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

        URL url = new URL(sm.retrieveValue("access_token_uri"));
        String profileURL = sm.retrieveValue("profile_uri");
        HttpSession session = request.getSession();

if(session !=null) {
    if (state != null) {
        System.out.println("SESS_DEBUG access_token: sessionid="+session.getId() +";state=" + state);
    }   else {
        System.out.println("SESS_DEBUG access_token: sessionid="+session.getId() +";state=NULL");
    }
}   else {
    System.out.println("SESS_DEBUG access_token: session is null!");
}

        bcidConnector bcidConnector = new bcidConnector();
        String oauthState = null;
        try {
            oauthState = session.getAttribute("oauth_state").toString();
        } catch (Exception e) {
            System.out.println("Authentication Error, session id " + session.getId()+ " is not recognized");
            response.sendRedirect("/biocode-fims/index.jsp?error=session id is not recognized... Try re-loading biocode-fims homepage, and logging in again.");
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
     * @param req
     * @param res
     * @throws IOException
     */
    @GET
    @Path("logout")

    public void logout(@Context HttpServletRequest req,
                       @Context HttpServletResponse res)
            throws IOException{
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
        //res.sendRedirect(sm.retrieveValue("logout_uri"));

        //res.sendRedirect("/biocode-fims/index.jsp");
        return;
    }
}
