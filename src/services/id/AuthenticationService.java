package services.id;

import auth.Authenticator;
import auth.LDAPAuthentication;
import auth.Authorizer;
import auth.oauth2.OAuthProvider;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.QueryParams;

import utils.SettingsManager;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.OAuthException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.ErrorInfo;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST interface for handling user authentication
 */
@Path("authenticationService")
public class AuthenticationService {

    @Context
    static HttpServletRequest request;
    private static Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    static SettingsManager sm;
    @Context
    static ServletContext context;
    private static String rootName;

    /**
     * Load settings manager
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();

        rootName = sm.retrieveValue("rootName");
    }

    /**
     * Service to log a user into the Bcid system
     *
     * @param usr
     * @param pass
     * @param return_to the url to return to after login
     *
     * @throws IOException
     */
    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("username") String usr,
                          @FormParam("password") String pass,
                          @QueryParam("return_to") String return_to,
                          @Context HttpServletResponse res) {

        if (!usr.isEmpty() && !pass.isEmpty()) {
            Authenticator authenticator = new Authenticator();
            Boolean isAuthenticated;

            // Verify that the entered and stored passwords match
            isAuthenticated = authenticator.login(usr, pass);
            HttpSession session = request.getSession();

//            logger.debug("BCID SESS_DEBUG login: sessionid=" + session.getId());

            if (isAuthenticated) {
                // Place the user in the session
                session.setAttribute("user", usr);
                Authorizer myAuthorizer = null;

                myAuthorizer = new Authorizer();

                // Check if the user is an admin for any projects
                if (myAuthorizer.userProjectAdmin(usr)) {
                    session.setAttribute("projectAdmin", true);
                }

                myAuthorizer.close();

                // Check if the user has created their own password, if they are just using the temporary password, inform the user to change their password
                if (!authenticator.userSetPass(usr)) {
                    // don't need authenticator anymore
                    authenticator.close();

                    return Response.ok("{\"url\": \"secure/profile.jsp?error=Update Your Password" +
                                    new QueryParams().getQueryParams(request.getParameterMap(), false) + "\"}")
                                    .build();
                } else {
                    // don't need authenticator anymore
                    authenticator.close();
                }

                // Redirect to return_to uri if provided
                if (return_to != null) {

                    // check to see if oAuthLogin is in the session and set to true is so.
                    Object oAuthLogin = session.getAttribute("oAuthLogin");
                    if (oAuthLogin != null) {
                        session.setAttribute("oAuthLogin", true);
                    }

                    return Response.ok("{\"url\": \"" + return_to +
                                new QueryParams().getQueryParams(request.getParameterMap(), true) + "\"}")
                            .build();
                } else {
                    return Response.ok("{\"url\": \"index.jsp\"}").build();
                }
            }
            // stored and entered passwords don't match, invalidate the session to be sure that a user is not in the session
            else {
                session.invalidate();
                authenticator.close();
            }
        }

        return Response.status(400)
                .entity(new ErrorInfo("Bad Credentials", 400).toJSON())
                .build();
    }

    /**
     * Service to log a user into the Bcid system with 2-factor authentication using LDAP & Entrust Identity Guard
     *
     * @param usr
     *
     * @throws IOException
     */
    @POST
    @Path("/loginLDAP")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loginLDAP(@FormParam("username") String usr,
                              @FormParam("password") String pass,
                              @Context HttpServletResponse res) {
        LDAPAuthentication ldapAuthentication = new LDAPAuthentication();
        Integer numLdapAttemptsAllowed = Integer.parseInt(sm.retrieveValue("ldapAttempts"));
        Integer ldapLockout = Integer.parseInt(sm.retrieveValue("ldapLockedAccountTimeout"));

        if (!usr.isEmpty() && !pass.isEmpty()) {
            // ldap accounts lock after x # of attempts. We need to determine how many attempts the user currently has and inform
            // the user of a locked account
            Integer ldapAttempts = ldapAuthentication.getLoginAttempts(usr);

            if (ldapAttempts < numLdapAttemptsAllowed) {
                Authenticator authenticator = new Authenticator();
                String[] challengeQuestions;

                // attempt to login via ldap server. If ldap authentication is successful, then challenge questions are
                // retrieved from the Entrust IG server
                challengeQuestions = authenticator.loginLDAP(usr, pass, true);

                // if challengeQuestions is null, then ldap authentication failed
                if (challengeQuestions != null) {
                    //Construct query params
                    String queryParams = "?userid=" + usr;
                    for (int i = 0; i < challengeQuestions.length; i++) {
                        queryParams += "&question_" + (i + 1) + "=" + challengeQuestions[i];
                    }
                    queryParams += "&" + request.getQueryString();

                    // need to return status 302 in order to pass SI vulnerabilities assessment
                    return Response.status(302).entity("{\"url\": \"entrustChallenge.jsp" + queryParams + "\"}")
                            .build();
                }

                // increase the number of attempts
                ldapAttempts += 1;
            }


            // if more then allowed number of ldap attempts, then the user is locked out of their account. We need to inform the user
            if (ldapAttempts >= numLdapAttemptsAllowed) {
                return Response.status(400)
                        .entity(new ErrorInfo("Your account is now locked for " + ldapLockout + " mins.", 400).toJSON())
                        .build();
            }

            return Response.status(400)
                    .entity(new ErrorInfo("Bad Credentials. " + (numLdapAttemptsAllowed - ldapAttempts) + " attempts remaining.",
                            400).toJSON())
                    .build();
        }
        return Response.status(400)
                .entity(new ErrorInfo("Empty Username or Password.", 400).toJSON())
                .build();
    }

    /**
     * Service to respond to Entrust IG challenge questions to complete authentication
     * @param return_to
     * @param question1
     * @param question2
     * @param userid
     * @return
     */
    @POST
    @Path("/entrustChallenge")
    @Produces(MediaType.APPLICATION_JSON)
    public Response entrustChallenge(@QueryParam("return_to") String return_to,
                                     @FormParam("question_1") String question1,
                                     @FormParam("question_2") String question2,
                                     @FormParam("userid") String userid) {
        String[] respChallenge = {question1, question2};

        if (userid != null && question1 != null && question2 != null) {
            Authenticator authenticator = new Authenticator();
            HttpSession session = request.getSession();

            // verify with the entrust IG server that the correct responses were provided to the challenge questions
            // If so, then the user is logged in
            if (authenticator.entrustChallenge(userid, respChallenge)) {
                // Place the user in the session
                session.setAttribute("user", userid);
                Authorizer myAuthorizer = null;

                myAuthorizer = new Authorizer();

                // Check if the user is an admin for any projects
                if (myAuthorizer.userProjectAdmin(userid)) {
                    session.setAttribute("projectAdmin", true);
                }

                myAuthorizer.close();
                authenticator.close();

                // Redirect to return_to uri if provided
                if (return_to != null) {

                    // check to see if oAuthLogin is in the session and set to true is so.
                    Object oAuthLogin = session.getAttribute("oAuthLogin");
                    if (oAuthLogin != null) {
                        session.setAttribute("oAuthLogin", true);
                    }

                    // need to return status 302 in order to pass SI vulnerabilities assessment
                    return Response.status(302).entity("{\"url\": \"" + return_to +
                            new QueryParams().getQueryParams(request.getParameterMap(), true) + "\"}")
                            .build();
                } else {
                    // need to return status 302 in order to pass SI vulnerabilities assessment
                    return Response.status(302).entity("{\"url\": \"index.jsp\"}").build();
                }
            }
            return Response.status(500)
                    .entity(new ErrorInfo("Server Error", 500).toJSON())
                    .build();
        }
        return Response.status(400)
                .entity(new ErrorInfo("Bad Request", 400).toJSON())
                .build();
    }

    /**
     * Service for a client app to log a user into the system via oAuth.
     *
     * @param clientId
     * @param redirectURL
     * @param state
     * @param response
     */
    @GET
    @Path("/oauth/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorize(@QueryParam("client_id") String clientId,
                              @QueryParam("redirect_uri") String redirectURL,
                              @QueryParam("state") String state,
                              @Context HttpServletResponse response) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");
        Object sessionoAuthLogin = session.getAttribute("oAuthLogin");
        Boolean oAuthLogin = false;

        // oAuthLogin is used to force the user to re-authenticate for oAuth
        if (sessionoAuthLogin != null && ((Boolean) sessionoAuthLogin)) {
            oAuthLogin = true;
        }

        OAuthProvider p = new OAuthProvider();

        if (redirectURL == null) {
            String callback = null;
            try {
                callback = p.getCallback(clientId);
            } catch (OAuthException e) {
                logger.warn("OAuthException retrieving callback for OAUTH clientID {}", clientId, e);
            }

            if (callback != null) {
                try {
                    p.close();
                    return Response.status(302).location(new URI(callback + "?error=invalid_request")).build();
                } catch (URISyntaxException e) {
                    logger.warn("Malformed callback URI for oAuth client {} and callback {}", clientId, callback);
                }
            }
            p.close();
            throw new BadRequestException("invalid_request");
        }

        if (clientId == null || !p.validClientId(clientId)) {
            redirectURL += "?error=unauthorized_client";
            try {
                p.close();
                return Response.status(302).location(new URI(redirectURL)).build();
            } catch (URISyntaxException e) {
                p.close();
                throw new BadRequestException("invalid_request", "invalid redirect_uri provided");
            }
        }

        if (username == null || !oAuthLogin) {
            session.setAttribute("oAuthLogin", "false");
            // need the user to login
            try {
                p.close();
                return Response.status(Response.Status.TEMPORARY_REDIRECT)
                        .location(new URI("../" + rootName + "/login.jsp?return_to=/id/authenticationService/oauth/authorize?"
                                    + request.getQueryString()))
                        .build();
            } catch (URISyntaxException e) {
                p.close();
                throw new ServerErrorException(e);
            }
        }
        //TODO ask user if they want to share profile information with requesting party
        String code = p.generateCode(clientId, redirectURL, username.toString());
        p.close();

        // no longer need oAuthLogin session attribute
        session.removeAttribute("oAuthLogin");

        redirectURL += "?code=" + code;

        if (state != null) {
            redirectURL += "&state=" + state;
        }
//        System.out.println("BCID redirecting to " + redirectURL);
        try {
            return Response.status(302)
                    .location(new URI(redirectURL))
                    .build();
        } catch (URISyntaxException e) {
            throw new BadRequestException("invalid_request", "invalid redirect_uri provided");
        }
    }

    /**
     * Service for a client app to exchange an oAuth code for an access token
     *
     * @param code
     * @param clientId
     * @param clientSecret
     * @param redirectURL
     * @param state
     *
     * @return
     */
    @POST
    @Path("/oauth/access_token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response access_token(@FormParam("code") String code,
                                 @FormParam("client_id") String clientId,
                                 @FormParam("client_secret") String clientSecret,
                                 @FormParam("redirect_uri") String redirectURL,
                                 @FormParam("grant_type") @DefaultValue("authorization_code") String grantType,
                                 @FormParam("username") String username,
                                 @FormParam("password") String password,
                                 @FormParam("state") String state) {
        OAuthProvider p = new OAuthProvider();
        JSONObject accessToken;

        if (clientId == null || clientSecret == null || !p.validateClient(clientId, clientSecret)) {
            p.close();
            throw new BadRequestException("invalid_client");
        }

        if (grantType.equalsIgnoreCase("authorization_code")) {
            if (redirectURL == null) {
                throw new BadRequestException("invalid_request", "redirect_uri is null");
            }

            if (code == null || !p.validateCode(clientId, code, redirectURL)) {
                p.close();
                throw new BadRequestException("invalid_grant", "Either code was null or the code doesn't match the " +
                        "clientId or the redirect_uri didn't match the redirect_uri sent with the authorization_code request");
            }
            accessToken = p.generateToken(clientId, state, code);
        } else if (grantType.equalsIgnoreCase("password")) {
            Authenticator authenticator = new Authenticator();
            if (username == null || password == null || !authenticator.login(username, password)) {
                throw new BadRequestException("invalid_request", "the supplied username and/or password are incorrect");
            }

            accessToken = p.generateToken(clientId, username);
        } else {
            throw new BadRequestException("unsupported_grant_type", "invalid grant_type was requested");
        }
        p.close();

        return Response.ok(accessToken.toJSONString())
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    /**
     * Service for an oAuth client app to exchange a refresh token for a valid access token.
     *
     * @param clientId
     * @param clientSecret
     * @param refreshToken
     *
     * @return
     */
    @POST
    @Path("/oauth/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh(@FormParam("client_id") String clientId,
                            @FormParam("client_secret") String clientSecret,
                            @FormParam("refresh_token") String refreshToken) {
        OAuthProvider p = new OAuthProvider();

        if (clientId == null || clientSecret == null || !p.validateClient(clientId, clientSecret)) {
            p.close();
            throw new BadRequestException("invalid_client");
        }

        if (refreshToken == null || !p.validateRefreshToken(refreshToken)) {
            p.close();
            throw new BadRequestException("invalid_grant", "refresh_token is invalid");
        }

        JSONObject accessToken = p.generateToken(refreshToken);

        // refresh tokens are only good once, so delete the old access token so the refresh token can no longer be used
        p.deleteAccessToken(refreshToken);
        p.close();

        return Response.ok(accessToken.toJSONString())
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    /**
     * Service for a user to exchange their reset token in order to update their password
     *
     * @param password
     * @param token
     * @param response
     *
     * @throws IOException
     */
    @POST
    @Path("/reset")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public void resetPassword(@FormParam("password") String password,
                              @FormParam("token") String token,
                              @Context HttpServletResponse response)
        throws IOException {
        if (token == null) {
            response.sendRedirect("/" + rootName + "/resetPass.jsp?error=Invalid Reset Token");
            return;
        }

        if (password.isEmpty()) {
            response.sendRedirect("/" + rootName + "/resetPass.jsp?error=Invalid Password");
            return;
        }

        Authorizer authorizer = new Authorizer();
        Authenticator authenticator = new Authenticator();

        if (!authorizer.validResetToken(token)) {
            response.sendRedirect("/" + rootName + "/resetPass.jsp?error=Expired Reset Token");
            authenticator.close();
            authorizer.close();
            return;
        }
        authorizer.close();

        if (authenticator.resetPass(token, password)) {
            response.sendRedirect("/" + rootName + "/login.jsp");
            authenticator.close();
            return;
        }
        authenticator.close();
    }

    /**
     * Service for a user to request that a password reset token is sent to their email
     *
     * @param username
     *
     * @return
     */
    @POST
    @Path("/sendResetToken")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response sendResetToken(@FormParam("username") String username) {

        if (username.isEmpty()) {
            throw new BadRequestException("User not found.", "username is null");
        }
        Authenticator a = new Authenticator();
        JSONObject resetToken = a.generateResetToken(username);
        a.close();
        return Response.ok(resetToken.toJSONString()).build();
    }
}
