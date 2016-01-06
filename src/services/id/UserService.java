package services.id;

import auth.Authenticator;
import bcid.Database;
import bcid.ProjectMinter;
import bcid.UserMinter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import biocode.fims.SettingsManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;

/**
 * The REST Interface for dealing with users. Includes user creation and profile updating.
 */
@Path("userService")
public class UserService {

    @Context
    HttpServletRequest request;

    private static Logger logger = LoggerFactory.getLogger(UserService.class);
    private static SettingsManager sm;
    private static String rootName;

    /**
     * Load settings manager
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();

        rootName = sm.retrieveValue("rootName");
    }

    /**
     * Service to create a new user.
     * @param username
     * @param password
     * @param firstName
     * @param lastName
     * @param email
     * @param institution
     * @param projectId
     * @return
     */
    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createUser(@FormParam("username") String username,
                               @FormParam("password") String password,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName,
                               @FormParam("email") String email,
                               @FormParam("institution") String institution,
                               @FormParam("projectId") Integer projectId) {

        HttpSession session = request.getSession();

        if (session.getAttribute("projectAdmin") == null) {
            // only project admins are able to create users
            throw new ForbiddenRequestException("Only project admins are able to create users.");
        }

        if ((username == null || username.isEmpty()) ||
                (password == null || password.isEmpty()) ||
                (firstName == null || firstName.isEmpty()) ||
                (lastName == null || lastName.isEmpty()) ||
                (email == null || email.isEmpty()) ||
                (institution == null) || institution.isEmpty()) {
            throw new BadRequestException("all fields are required");
        }

        // check that a valid email is given
        if (!email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
            throw new BadRequestException("please enter a valid email");
        }

        Hashtable<String, String> userInfo = new Hashtable<String, String>();
        userInfo.put("username", username);
        userInfo.put("firstName", firstName);
        userInfo.put("lastName", lastName);
        userInfo.put("email", email);
        userInfo.put("institution", institution);
        userInfo.put("password", password);

        UserMinter u = new UserMinter();
        ProjectMinter p = new ProjectMinter();
        try {
            String admin = session.getAttribute("user").toString();
            Database db = new Database();
            Integer adminId = db.getUserId(admin);
            db.close();

            if (u.checkUsernameExists(username)) {
                throw new BadRequestException("username already exists");
            }
            // check if the user is this project's admin
            if (!p.userProjectAdmin(adminId, projectId)) {
                throw new ForbiddenRequestException("You can't add a user to a project that you're not an admin.");
            }
            return Response.ok(u.createUser(userInfo, projectId)).build();
        } finally {
            u.close();
            p.close();
        }
    }

    /**
     * Returns an HTML table in order to create a user.
     * @return
     */
    @GET
    @Path("/createFormAsTable")
    @Produces(MediaType.TEXT_HTML)
    public String createFormAsTable() {
        UserMinter u = new UserMinter();
        String response = u.getCreateForm();
        u.close();
        return response;
    }

    /**
     * Service for a project admin to update a member user's profile
     * @param firstName
     * @param lastName
     * @param email
     * @param institution
     * @param new_password
     * @param username
     * @return
     */
    @POST
    @Path("/profile/update/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response adminUpdateProfile(@FormParam("firstName") String firstName,
                                       @FormParam("lastName") String lastName,
                                       @FormParam("email") String email,
                                       @FormParam("institution") String institution,
                                       @FormParam("new_password") String new_password,
                                       @PathParam("username") String username) {
        HttpSession session = request.getSession();
        Hashtable<String, String> update = new Hashtable<String, String>();

        if (session.getAttribute("projectAdmin") == null) {
            throw new ForbiddenRequestException("You must be a project admin to edit another user's profile");
        }

        // set new password if given
        if (!new_password.isEmpty()) {
            Authenticator authenticator = new Authenticator();
            Boolean success = authenticator.setHashedPass(username, new_password);
            authenticator.close();
            if (!success) {
                throw new BadRequestException("user: " + username + "not found");
            } else {
                // Make the user change their password next time they login
                update.put("hasSetPassword", "0");
            }
        }

        // Check if any other fields should be updated
        UserMinter u = new UserMinter();

        try {
            if (!firstName.equals(u.getFirstName(username))) {
                update.put("firstName", firstName);
            }
            if (!lastName.equals(u.getLastName(username))) {
                update.put("lastName", lastName);
            }
            if (!email.equals(u.getEmail(username))) {
                // check that a valid email is given
                if (email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
                    update.put("email", email);
                } else {
                    throw new BadRequestException("Please enter a valid email.");
                }
            }
            if (!institution.equals(u.getInstitution(username))) {
                update.put("institution", institution);
            }


            if (!update.isEmpty()) {
                Boolean success = u.updateProfile(update, username);
                if (!success) {
                    throw new BadRequestException("user: " + username + "not found");
                }
            }
            return Response.ok("{\"success\": \"true\"}").build();
        } finally {
            u.close();
        }
    }

    /**
     * Service for a user to update their profile.
     * @param firstName
     * @param lastName
     * @param email
     * @param institution
     * @param old_password
     * @param new_password
     * @param return_to
     * @returns either error message or the url to redirect to upon success
     */
    @POST
    @Path("/profile/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProfile(@FormParam("firstName") String firstName,
                                  @FormParam("lastName") String lastName,
                                  @FormParam("email") String email,
                                  @FormParam("institution") String institution,
                                  @FormParam("old_password") String old_password,
                                  @FormParam("new_password") String new_password,
                                  @QueryParam("return_to") String return_to,
                                  @Context HttpServletResponse response) {

        HttpSession session = request.getSession();
        String username = session.getAttribute("user").toString();
//        String error = "";
        Hashtable<String, String> update = new Hashtable<String, String>();

        // Only update user's password if both old_password and new_password fields contain values
        if (!old_password.isEmpty() && !new_password.isEmpty()) {
            Authenticator myAuth = new Authenticator();
            // Call the login function to verify the user's old_password
            Boolean valid_pass = myAuth.login(username, old_password);

            // If user's old_password matches stored pass, then update the user's password to the new value
            if (valid_pass) {
                Boolean success = myAuth.setHashedPass(username, new_password);
                if (!success) {
                    throw new ServerErrorException("Server Error", "User not found");
                }
                // Make sure that the hasSetPassword field is 1 (true) so they aren't asked to change their password after login
                else {
                    update.put("hasSetPassword", "1");
                }
            }
            else {
                throw new BadRequestException("Wrong Password");
            }
            myAuth.close();

        }
        Database db;

        // Check if any other fields should be updated
        UserMinter u = new UserMinter();

        try {
            if (!firstName.equals(u.getFirstName(username))) {
                update.put("firstName", firstName);
            }
            if (!lastName.equals(u.getLastName(username))) {
                update.put("lastName", lastName);
            }
            if (!email.equals(u.getEmail(username))) {
                // check that a valid email is given
                if (email.toUpperCase().matches("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}")) {
                    update.put("email", email);
                } else {
                    throw new BadRequestException("please enter a valid email");
                }
            }
            if (!institution.equals(u.getInstitution(username))) {
                update.put("institution", institution);
            }

            if (!update.isEmpty()) {
                Boolean success = u.updateProfile(update, username);
                if (!success) {
                    throw new ServerErrorException("Server Error", "User not found");
                }
            }

            if (return_to != null) {
                return Response.ok("{\"success\": \"" + return_to + "\"}").build();
            } else {
                return Response.ok("{\"success\": \"/" + rootName + "/secure/profile.jsp\"}").build();
            }
        } finally {
            u.close();
        }
    }

    /**
     * Returns an HTML table for editing a user's profile. Project admin use only.
     * @param username
     * @return
     */
    @GET
    @Path("/profile/listEditorAsTable/{username}")
    @Produces(MediaType.TEXT_HTML)
    public String getUsersProfile(@PathParam("username") String username) {
        HttpSession session = request.getSession();

        if (session.getAttribute("projectAdmin") == null) {
            throw new ForbiddenRequestException("You must be a project admin to edit a user's profile");
        }

        UserMinter u = new UserMinter();
        String response = u.getProfileEditorAsTable(username, true);
        u.close();
        return response;
    }

    /**
     * returns an HTML table for editing a user's profile.
     * @return
     */
    @GET
    @Path("/profile/listEditorAsTable")
    @Produces(MediaType.TEXT_HTML)
    public String getProfile() {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your profile.");
        }

        UserMinter u = new UserMinter();
        String response = u.getProfileEditorAsTable(username.toString(), false);
        u.close();
        return response;
    }

    /**
     * Return a HTML table displaying the user's profile
     *
     * @return String with HTML response
     */
    @GET
    @Path("/profile/listAsTable")
    @Produces(MediaType.TEXT_HTML)
    public String listUserProfile() {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your profile.");
        }

        UserMinter u = new UserMinter();
        String response = u.getProfileHTML(username.toString());
        u.close();
        return response;
    }

    /**
     * Service for oAuth client apps to retrieve a user's profile information.
     * @param access_token
     * @return
     */
    @GET
    @Path("/oauth")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserData(@QueryParam("access_token") String access_token) {
        if (access_token != null) {
            UserMinter u = new UserMinter();
            JSONObject response = u.getOauthProfile(access_token);
            u.close();
            return Response.ok(response.toString()).build();
        }
        throw new BadRequestException("invalid_grant", "access_token was null");
    }
}
