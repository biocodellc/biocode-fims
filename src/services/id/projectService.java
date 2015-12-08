package services.id;

import auth.oauth2.provider;
import bcid.database;
import bcid.projectMinter;
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
 * Currently, there are no REST services for creating projects, which instead must be added to the database
 * manually by an administrator
 */
@Path("projectService")
public class projectService {

    @Context
    static HttpServletRequest request;

     /**
     * Given a project_id, return the validationXML file
     *
     * @param project_id
     * @return
     */
    @GET
    @Path("/validation/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAlias(@PathParam("project_id") Integer project_id) {

        projectMinter project = new projectMinter();
        String response = project.getValidationXML(project_id);
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
            provider p = new provider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username != null) {
            database db = new database();
            userId = db.getUserId(username);
            db.close();
        }

        projectMinter project = new projectMinter();
        String response = project.listProjects(userId);
        project.close();

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Given an project identifier, get the latest graphs by expedition
     *
     * @param project_id
     * @return
     */
    @GET
    @Path("/graphs/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestGraphsByExpedition(@PathParam("project_id") Integer project_id,
                                                @QueryParam("access_token") String accessToken) {
        String username;

        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            provider p = new provider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }
        projectMinter project= new projectMinter();

        String response = project.getLatestGraphs(project_id, username);
        project.close();

        return Response.ok(response).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Given an project identifier, get the users latest datasets by expedition
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
            provider p = new provider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username == null) {
            throw new UnauthorizedRequestException("You must login to retrieve you're graphs.");
        }

        projectMinter project= new projectMinter();

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
            provider p = new provider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username == null) {
            throw new UnauthorizedRequestException("You must login to retrieve you're graphs.");
        }

        projectMinter project= new projectMinter();

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

        projectMinter project= new projectMinter();
        String response = project.listUserAdminProjects(username);
        project.close();

        return Response.ok(response).build();
    }

    /**
     * return an HTML table of a project's configuration.
     * @param project_id
     * @return
     */
    @GET
    @Path("/configAsTable/{project_id}")
    @Produces(MediaType.TEXT_HTML)
    public Response getProjectConfig(@PathParam("project_id") Integer project_id) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be this project's admin in order to view its configuration");
        }
        projectMinter project = new projectMinter();
        String response = project.getProjectConfigAsTable(project_id, username.toString());
        project.close();
        return Response.ok(response).build();
    }

    /**
     * return an HTML table used for editing a project's configuration.
     * @param projectId
     * @return
     */
    @GET
    @Path("/configEditorAsTable/{project_id}")
    @Produces(MediaType.TEXT_HTML)
    public String getConfigEditorAsTable(@PathParam("project_id") Integer projectId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be this project's admin in order to edit its configuration");
        }

        projectMinter project = new projectMinter();
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
    @Path("/updateConfig/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateConfig(@PathParam("project_id") Integer projectID,
                                 @FormParam("title") String title,
                                 @FormParam("validation_xml") String validationXML,
                                 @FormParam("public") String publicProject) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to edit a project's config.");
        }
        projectMinter p = new projectMinter();
        database db = new database();
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
                update.put("project_title", title);
            }
            if (!config.containsKey("validation_xml") || !config.get("validation_xml").equals(validationXML)) {
                update.put("bioValidator_validation_xml", validationXML);
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
    @Path("/removeUser/{project_id}/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUser(@PathParam("project_id") Integer projectId,
                               @PathParam("user_id") Integer userId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must login.");
        }

        projectMinter p = new projectMinter();

        database db = new database();
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
    public Response addUser(@FormParam("project_id") Integer projectId,
                            @FormParam("user_id") Integer userId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        // userId of 0 means create new user, using ajax to create user, shouldn't ever receive userId of 0
        if (userId == 0) {
            throw new BadRequestException("invalid userId");
        }


        if (username == null) {
            throw new UnauthorizedRequestException("You must login to access this service.");
        }
        database db = new database();
        Integer lodedInUserId = db.getUserId(username.toString());
        db.close();

        projectMinter p = new projectMinter();
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
    @Path("/listProjectUsersAsTable/{project_id}")
    @Produces(MediaType.TEXT_HTML)
    public String getSystemUsers(@PathParam("project_id") Integer projectId) {
        HttpSession session = request.getSession();

        if (session.getAttribute("projectAdmin") == null) {
            // only display system users to project admins
            throw new ForbiddenRequestException("You are not an admin to this project");
        }

        projectMinter p = new projectMinter();
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
            provider p = new provider();
            username = p.validateToken(accessToken);
            p.close();
        } else {
            HttpSession session = request.getSession();
            username = (String) session.getAttribute("user");
        }

        if (username == null) {
            throw new UnauthorizedRequestException("authorization_error");
        }

        projectMinter p = new projectMinter();
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
                                       @FormParam("project_id") Integer projectId,
                                       @QueryParam("access_token") String accessToken) {

        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To change the default config, talk to the project admin.\"}").build();
        }

        Integer userId = null;
        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            provider p = new provider();
            String username = p.validateToken(accessToken);
            p.close();
            database db = new database();
            userId = db.getUserId(username);
            db.close();        }

        if (userId == null) {
            throw new UnauthorizedRequestException("authorization_error");
        }

        projectMinter p = new projectMinter();

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
    @Path("/getTemplateConfigs/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateConfigs(@PathParam("project_id") Integer projectId) {
        projectMinter p = new projectMinter();
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
        projectMinter p = new projectMinter();
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

        Integer userId = null;
        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            provider p = new provider();
            String username = p.validateToken(accessToken);
            p.close();
            database db = new database();
            userId = db.getUserId(username);
            db.close();        }

        if (userId == null) {
            throw new UnauthorizedRequestException("authorization_error");
        }

        projectMinter p = new projectMinter();
        if (p.configExists(configName, projectId) && p.usersConfig(configName, projectId, userId)) {
            p.removeTemplateConfig(configName, projectId);
        } else {
            return Response.ok("{\"error\": \"Only the owners of a configuration can remove the configuration.\"}").build();
        }
        p.close();

        return Response.ok("{\"success\": \"Successfully removed template configuration.\"}").build();
    }
}
