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

    @GET
    @Path("login")
    public void login(@Context HttpServletResponse response,
                      @Context HttpServletRequest request) throws IOException {
        stringGenerator sg = new stringGenerator();
        String state = sg.generateString(20);

        HttpSession session = request.getSession();
        session.setAttribute("oauth_state", state);

        SettingsManager sm = SettingsManager.getInstance();
        try {
            sm.loadProperties();
        } catch (Exception e) {
            e.printStackTrace();
        }

        response.sendRedirect(sm.retrieveValue("authorize_uri") + "client_id=" + sm.retrieveValue("client_id") + "&redirect_uri=" + sm.retrieveValue("redirect_uri") +
                "&state=" + state);
        return;
    }

    /**
     * first, exchange the returned oauth code for an access token. Then use the access token to obtain the user's
     * profile information, and store the username and user id in the session.
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
        bcidConnector bcidConnector = new bcidConnector();
        String oauthState = session.getAttribute("oauth_state").toString();

        if (code == null || state == null || !state.equals(oauthState)) {
            response.sendRedirect("/biocode-fims/index.jsp?error=authentication_error");
            return;
        }

        String postParams = "client_id=" + sm.retrieveValue("client_id") + "&client_secret=" + sm.retrieveValue("client_secret") +
                "&code=" + code + "&redirect_uri=" + sm.retrieveValue("redirect_uri");


        Object tokenResponse = JSONValue.parse(bcidConnector.createPOSTConnnection(url, postParams));
        JSONArray tokenArray = (JSONArray) tokenResponse;

        JSONObject tokenJSON = (JSONObject) tokenArray.get(0);

        if (tokenJSON.containsKey("error") ||(tokenJSON.containsKey("state") && !tokenJSON.get("state").equals(oauthState))) {
            response.sendRedirect("/biocode-fims/index.jsp?error=authentication_error");
            return;
        }

        String access_token = tokenJSON.get("access_token").toString();

        Object profileResponse = JSONValue.parse(bcidConnector.createGETConnection(new URL(profileURL + access_token)));
        JSONArray profileArray = (JSONArray) profileResponse;
        JSONObject profileJSON = (JSONObject) profileArray.get(0);

        session.setAttribute("user", profileJSON.get("username"));
        session.setAttribute("userId", profileJSON.get("user_id"));

        response.sendRedirect("/biocode-fims/index.jsp");
        return;
    }

    @GET
    @Path("logout")
    @Produces(MediaType.TEXT_HTML)
    public void logout(@Context HttpServletRequest req,
                       @Context HttpServletResponse res)
            throws IOException{

        HttpSession session = req.getSession();

        session.invalidate();
        res.sendRedirect("/biocode-fims/index.jsp");
        return;
    }
}
