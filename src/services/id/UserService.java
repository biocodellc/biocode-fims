package services.id;

import auth.Authenticator;
import auth.Authorizer;
import auth.oauth2.OAuthProvider;
import bcid.ProjectMinter;
import bcid.UserMinter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import services.BiocodeFimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;

/**
 * The REST Interface for dealing with users. Includes user creation and profile updating.
 */
@Path("userService")
public class UserService extends BiocodeFimsService {

    @GET
    @Path("/list")
    public Response getUsers() {
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must login to access this service.");
        }
        Authorizer authorizer = new Authorizer();
        if (!authorizer.userProjectAdmin(username)) {
            throw new ForbiddenRequestException("You must be a project admin to view the users.");
        }
        authorizer.close();

        UserMinter userMinter = new UserMinter();
        JSONArray users = userMinter.getUsers();
        userMinter.close();

        return Response.ok(users.toJSONString()).build();
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createUser(@FormParam("username") String username,
                               @FormParam("password") String password,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName,
                               @FormParam("email") String email,
                               @FormParam("institution") String institution,
                               @FormParam("projectId") Integer projectId) {
        OAuthProvider provider = new OAuthProvider();
        String aUser = provider.validateToken(accessToken);
        provider.close();

        if (aUser == null) {
            throw new UnauthorizedRequestException("You must be logged in.");
        }

        Authorizer authorizer = new Authorizer();
        if (!authorizer.userProjectAdmin(aUser)) {
            // only project admins are able to create users
            throw new ForbiddenRequestException("Only project admins are able to create users.");
        }
        authorizer.close();

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
            if (u.checkUsernameExists(username)) {
                throw new BadRequestException("username already exists");
            }
            // check if the user is this project's admin
            if (!p.isProjectAdmin(aUser, projectId)) {
                throw new ForbiddenRequestException("You can't add a user to a project that you're not an admin.");
            }
            return Response.ok(u.createUser(userInfo, projectId)).build();
        } finally {
            u.close();
            p.close();
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
     * @param returnTo
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
                                  @FormParam("username") String username,
                                  @QueryParam("return_to") String returnTo) {
        OAuthProvider provider = new OAuthProvider();
        String aUser = provider.validateToken(accessToken);
        provider.close();

        if (aUser == null) {
            throw new UnauthorizedRequestException("You must login");
        }

        Authorizer authorizer = new Authorizer();

        if (!aUser.equals(username.trim()) && !authorizer.userProjectAdmin(aUser)) {
            throw new ForbiddenRequestException("You must be a project admin to update someone else's profile.");
        }

        Boolean adminAccess = false;
        if (!aUser.equals(username.trim()) && authorizer.userProjectAdmin(aUser))
            adminAccess = true;

        authorizer.close();

        Hashtable<String, String> update = new Hashtable<String, String>();

        // Only update user's password if both old_password and new_password fields contain values and the user is updating
        // their own profile
        if (!adminAccess) {
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
                } else {
                    throw new BadRequestException("Wrong Password");
                }
                myAuth.close();

            }
        } else {
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

            JSONObject response = new JSONObject();
            response.put("adminAccess", adminAccess);
            if (returnTo != null) {
                response.put("returnTo", returnTo);
            }
            return Response.ok(response.toJSONString()).build();
        } finally {
            u.close();
        }
    }

    /**
     * retrieves the user's profile. Project admin use only.
     * @param username
     * @return
     */
    @GET
    @Path("/profile/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersProfile(@PathParam("username") String username) {
        OAuthProvider provider = new OAuthProvider();
        String admin = provider.validateToken(accessToken);
        provider.close();

        if (admin == null) {
            throw new UnauthorizedRequestException("You must login.");
        }
        Authorizer authorizer = new Authorizer();

        if (!authorizer.userProjectAdmin(admin)) {
            throw new ForbiddenRequestException("You must be a project admin to edit a user's profile");
        }

        UserMinter u = new UserMinter();
        JSONObject profile = u.getUserProfile(username);
        u.close();
        return Response.ok(profile.toJSONString()).build();
    }

    /**
     * Service for oAuth client apps to retrieve a user's profile information.
     * @return
     */
    @GET
    @Path("/profile")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserData() {
        if (accessToken != null) {
            UserMinter u = new UserMinter();
            JSONObject response = u.getOauthProfile(accessToken);
            u.close();
            return Response.ok(response.toJSONString()).build();
        }
        throw new BadRequestException("invalid_grant", "access_token was null");
    }
}
