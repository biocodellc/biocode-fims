package services.id;

import auth.oauth2.OAuthProvider;
import bcid.Database;
import bcid.ProjectMinter;
import fimsExceptions.BadRequestException;
import fimsExceptions.ForbiddenRequestException;
import fimsExceptions.UnauthorizedRequestException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Hashtable;
import java.util.List;

/**
 * REST interface calls for working with projects.  This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 */
@Path("projectService")
public class ProjectService {

    @Context
    static HttpServletRequest request;

     /**
     * Given a projectId, return the validationXML file
     *
     * @param projectId
     * @return
     */
    @GET
    @Path("/validation/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAlias(@PathParam("projectId") Integer projectId) {

        ProjectMinter project = new ProjectMinter();
        String response = project.getValidationXML(projectId);
        project.close();

        if (response == null) {
            return Response.status(Response.Status.NO_CONTENT)
                    .entity("{\"url\": \"\"}").build();
        } else {
            return Response.ok("{\"url\": \"" + response + "\"}").header("Access-Control-Allow-Origin", "*").build();
        }
    }

    /**
     * Produce a list of all publically available projects and the private projects the logged in user is a memeber of
     *
     * @return  Generates a JSON listing containing project metadata as an array
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchList(@QueryParam("access_token") String accessToken) {
        Integer userId = null;
        String username;

        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username != null) {
            Database db = new Database();
            userId = db.getUserId(username);
            db.close();
        }

        ProjectMinter project = new ProjectMinter();
        String response = project.listProjects(userId);
        project.close();

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Given an project Bcid, get the latest graphs by expedition
     *
     * @param projectId
     * @return
     */
    @GET
    @Path("/graphs/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestGraphsByExpedition(@PathParam("projectId") Integer projectId,
                                                @QueryParam("access_token") String accessToken) {
        String username;

        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }
        ProjectMinter project= new ProjectMinter();

        String response = project.getLatestGraphs(projectId, username);
        project.close();

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Given an project Bcid, get the users latest datasets by expedition
     *
     * @return
     */
    @GET
    @Path("/myGraphs/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyLatestGraphs(@QueryParam("access_token") String accessToken) {
        String username;

        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username == null) {
            throw new UnauthorizedRequestException("You must login to retrieve you're graphs.");
        }

        ProjectMinter project= new ProjectMinter();

        String response = project.getMyLatestGraphs(username);
        project.close();

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

     /**
     * Get the users datasets
     *
     * @return
     */
    @GET
    @Path("/myDatasets/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasets(@QueryParam("access_token") String accessToken) {
        String username;

        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username == null) {
            throw new UnauthorizedRequestException("You must login to retrieve you're graphs.");
        }

        ProjectMinter project= new ProjectMinter();

        String response = project.getMyTemplatesAndDatasets(username);
        project.close();

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }
    /**
     * Return a json representation to be used for select options of the projects that a user is an admin to
     * @return
     */
    @GET
    @Path("/admin/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserAdminProjects() {
        HttpSession session = request.getSession();

        if (session.getAttribute("projectAdmin") == null) {
            throw new ForbiddenRequestException("You must be the project's admin.");
        }
        String username = session.getAttribute("user").toString();

        ProjectMinter project= new ProjectMinter();
        String response = project.listUserAdminProjects(username);
        project.close();

        return Response.ok(response).build();
    }

    /**
     * return an HTML table of a project's configuration.
     * @param projectId
     * @return
     */
    @GET
    @Path("/configAsTable/{projectId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getProjectConfig(@PathParam("projectId") Integer projectId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be this project's admin in order to view its configuration");
        }
        ProjectMinter project = new ProjectMinter();
        String response = project.getProjectConfigAsTable(projectId, username.toString());
        project.close();
        return Response.ok(response).build();
    }

    /**
     * return an HTML table used for editing a project's configuration.
     * @param projectId
     * @return
     */
    @GET
    @Path("/configEditorAsTable/{projectId}")
    @Produces(MediaType.TEXT_HTML)
    public String getConfigEditorAsTable(@PathParam("projectId") Integer projectId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be this project's admin in order to edit its configuration");
        }

        ProjectMinter project = new ProjectMinter();
        String response = project.getProjectConfigEditorAsTable(projectId, username.toString());
        project.close();
        return response;
    }

    /**
     * Service used for updating a project's configuration.
     * @param projectID
     * @param title
     * @param validationXML
     * @param publicProject
     * @return
     */
    @POST
    @Path("/updateConfig/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfig(@PathParam("projectId") Integer projectID,
                                 @FormParam("title") String title,
                                 @FormParam("validationXml") String validationXML,
                                 @FormParam("public") String publicProject) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to edit a project's config.");
        }
        ProjectMinter p = new ProjectMinter();
        Database db = new Database();
        Integer userId = db.getUserId(username.toString());
        db.close();

        try {
            if (!p.userProjectAdmin(userId, projectID)) {
                throw new ForbiddenRequestException("You must be this project's admin in order to edit the config");
            }

            Hashtable config = p.getProjectConfig(projectID, username.toString());
            Hashtable<String, String> update = new Hashtable<String, String>();

            if (title != null &&
                    !config.get("title").equals(title)) {
                update.put("projectTitle", title);
            }
            if (!config.containsKey("validationXml") || !config.get("validationXml").equals(validationXML)) {
                update.put("validationXml", validationXML);
            }
            if ((publicProject != null && (publicProject.equals("on") || publicProject.equals("true")) && config.get("public").equals("false")) ||
                    (publicProject == null && config.get("public").equals("true"))) {
                if (publicProject != null && (publicProject.equals("on") || publicProject.equals("true"))) {
                    update.put("public", "true");
                } else {
                    update.put("public", "false");
                }
            }

            if (!update.isEmpty()) {
                if (p.updateConfig(update, projectID)) {
                    return Response.ok("{\"success\": \"Successfully update project config.\"}").build();
                } else {
                    throw new BadRequestException("Project wasn't found");
                }
            } else {
                return Response.ok("{\"success\": \"nothing needed to be updated\"}").build();
            }
        } finally {
            p.close();
        }
    }

    /**
     * Service used to remove a user as a member of a project.
     * @param projectId
     * @param userId
     * @return
     */
    @GET
    @Path("/removeUser/{projectId}/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUser(@PathParam("projectId") Integer projectId,
                               @PathParam("userId") Integer userId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must login.");
        }

        ProjectMinter p = new ProjectMinter();

        Database db = new Database();
        Integer loggedInUserId = db.getUserId(username.toString());
        db.close();
        if (!p.userProjectAdmin(loggedInUserId, projectId)) {
            p.close();
            throw new ForbiddenRequestException("You are not this project's admin.");
        }

        p.removeUser(userId, projectId);
        p.close();

        return Response.ok("{\"success\": \"User has been successfully removed\"}").build();
    }

    /**
     * Service used to add a user as a member of a project.
     * @param projectId
     * @param userId
     * @return
     */
    @POST
    @Path("/addUser")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUser(@FormParam("projectId") Integer projectId,
                            @FormParam("userId") Integer userId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        // userId of 0 means create new user, using ajax to create user, shouldn't ever receive userId of 0
        if (userId == 0) {
            throw new BadRequestException("invalid userId");
        }

        if (username == null) {
            throw new UnauthorizedRequestException("You must login to access this service.");
        }
        Database db = new Database();
        Integer lodedInUserId = db.getUserId(username.toString());
        db.close();

        ProjectMinter p = new ProjectMinter();
        if (!p.userProjectAdmin(lodedInUserId, projectId)) {
            p.close();
            throw new ForbiddenRequestException("You are not this project's admin");
        }
        p.addUserToProject(userId, projectId);
        p.close();

        return Response.ok("{\"success\": \"User has been successfully added to this project\"}").build();
    }

    /**
     * return an HTML table listing all members of a project
     * @param projectId
     * @return
     */
    @GET
    @Path("/listProjectUsersAsTable/{projectId}")
    @Produces(MediaType.TEXT_HTML)
    public String getSystemUsers(@PathParam("projectId") Integer projectId) {
        HttpSession session = request.getSession();

        if (session.getAttribute("projectAdmin") == null) {
            // only display system users to project admins
            throw new ForbiddenRequestException("You are not an admin to this project");
        }

        ProjectMinter p = new ProjectMinter();
        String response = p.listProjectUsersAsTable(projectId);
        p.close();
        return response;
    }

    /**
     * Service used to retrieve a JSON representation of the project's a user is a member of.
     * @param accessToken
     * @return
     */
    @GET
    @Path("/listUserProjects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjects(@QueryParam("access_token") String accessToken) {
        String username;

        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username == null) {
            throw new UnauthorizedRequestException("authorization_error");
        }

        ProjectMinter p = new ProjectMinter();
        String response = p.listUsersProjects(username);
        p.close();
        return Response.ok(response).build();
    }

    /**
     * Service used to save a fims template generator configuration
     * @param checkedOptions
     * @param configName
     * @param projectId
     * @param accessToken
     * @return
     */
    @POST
    @Path("/saveTemplateConfig")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveTemplateConfig(@FormParam("checkedOptions") List<String> checkedOptions,
                                       @FormParam("configName") String configName,
                                       @FormParam("projectId") Integer projectId,
                                       @QueryParam("access_token") String accessToken) {

        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To change the default config, talk to the project admin.\"}").build();
        }

        String username;
        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }
        Database db = new Database();
        Integer userId = db.getUserId(username);
        db.close();

        if (userId == null) {
            throw new UnauthorizedRequestException("You must be logged in to save a configuration.");
        }

        ProjectMinter p = new ProjectMinter();

        if (p.configExists(configName, projectId)) {
            if (p.usersConfig(configName, projectId, userId)) {
                p.updateTemplateConfig(configName, projectId, userId, checkedOptions);
            } else {
                return Response.ok("{\"error\": \"A configuration with that name already exists, and you are not the owner.\"}").build();
            }
        } else {
            p.saveTemplateConfig(configName, projectId, userId, checkedOptions);
        }
        p.close();

        return Response.ok("{\"success\": \"Successfully saved template configuration.\"}").build();
    }

    /**
     * Service used to get the fims template generator configurations for a given project
     * @param projectId
     * @return
     */
    @GET
    @Path("/getTemplateConfigs/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateConfigs(@PathParam("projectId") Integer projectId) {
        ProjectMinter p = new ProjectMinter();
        String response = p.getTemplateConfigs(projectId);
        p.close();

        return Response.ok(response).build();
    }

    /**
     * Service used to get a specific fims template generator configuration
     * @param configName
     * @param projectId
     * @return
     */
    @GET
    @Path("/getTemplateConfig/{projectId}/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("configName") String configName,
                              @PathParam("projectId") Integer projectId) {
        ProjectMinter p = new ProjectMinter();
        String response = p.getTemplateConfig(configName, projectId);
        p.close();

        return Response.ok(response).build();
    }
    /**
     * Service used to delete a specific fims template generator configuration
     * @param configName
     * @param projectId
     * @return
     */
    @GET
    @Path("/removeTemplateConfig/{projectId}/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeConfig(@PathParam("configName") String configName,
                                 @PathParam("projectId") Integer projectId,
                                 @QueryParam("access_token") String accessToken) {
        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To remove the default config, talk to the project admin.\"}").build();
        }

        String username;
        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }
        Database db = new Database();
        Integer userId = db.getUserId(username);
        db.close();

        if (userId == null) {
            throw new UnauthorizedRequestException("Only the owners of a configuration can remove the configuration");
        }

        ProjectMinter p = new ProjectMinter();
        if (p.configExists(configName, projectId) && p.usersConfig(configName, projectId, userId)) {
            p.removeTemplateConfig(configName, projectId);
        } else {
            return Response.ok("{\"error\": \"Only the owners of a configuration can remove the configuration.\"}").build();
        }
        p.close();

        return Response.ok("{\"success\": \"Successfully removed template configuration.\"}").build();
    }
}
