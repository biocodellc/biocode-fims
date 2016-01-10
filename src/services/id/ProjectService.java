package services.id;

import auth.oauth2.OAuthProvider;
import bcid.Database;
import bcid.ProjectMinter;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import digester.Attribute;
import digester.Mapping;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import run.Process;
import run.ProcessController;
import services.BiocodeFimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * REST interface calls for working with projects.  This includes fetching details associated with projects.
 * Currently, there are no REST services for creating projects, which instead must be added to the Database
 * manually by an administrator
 */
@Path("projectService")
public class ProjectService extends BiocodeFimsService{

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
    public Response fetchList() {
        Integer userId = null;
        String username = null;

        // if accessToken != null, then OAuth client is accessing on behalf of a user
        if (accessToken != null) {
            OAuthProvider p = new OAuthProvider();
            username = p.validateToken(accessToken);
            p.close();
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
    public Response getLatestGraphsByExpedition(@PathParam("projectId") Integer projectId) {
        OAuthProvider p = new OAuthProvider();
        ProjectMinter project= new ProjectMinter();
        String username = p.validateToken(accessToken);
        p.close();

        JSONArray graphs = project.getLatestGraphs(projectId, username);
        project.close();

        return Response.ok(graphs.toJSONString()).header("Access-Control-Allow-Origin", "*").build();
    }

    /**
     * Given an project Bcid, get the users latest datasets by expedition
     *
     * @return
     */
    @GET
    @Path("/myGraphs/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMyLatestGraphs() {
        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

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
    public Response getDatasets() {
        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

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
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your projects");
        }

        ProjectMinter project= new ProjectMinter();
        JSONArray projects = project.getAdminProjects(username);
        project.close();

        return Response.ok(projects.toJSONString()).build();
    }

    /**
     * service to retrieve a project's metadata
     * @param projectId
     * @return
     */
    @GET
    @Path("/metadata/{projectId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getMetadata(@PathParam("projectId") Integer projectId) {
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be this project's admin in order to view its configuration");
        }
        ProjectMinter project = new ProjectMinter();
        JSONObject metadata = project.getMetadata(projectId, username);
        project.close();
        return Response.ok(metadata.toJSONString()).build();
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateConfig(@PathParam("projectId") Integer projectID,
                                 @FormParam("title") String title,
                                 @FormParam("validationXml") String validationXML,
                                 @FormParam("public") String publicProject) {
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to edit a project's config.");
        }
        ProjectMinter p = new ProjectMinter();
        Database db = new Database();
        Integer userId = db.getUserId(username.toString());
        db.close();

        try {
            if (!p.isProjectAdmin(userId, projectID)) {
                throw new ForbiddenRequestException("You must be this project's admin in order to edit the config");
            }

            JSONObject metadata = p.getMetadata(projectID, username.toString());
            Hashtable<String, String> update = new Hashtable<String, String>();

            if (title != null &&
                    !metadata.get("title").equals(title)) {
                update.put("projectTitle", title);
            }
            if (!metadata.containsKey("validationXml") || !metadata.get("validationXml").equals(validationXML)) {
                update.put("validationXml", validationXML);
            }
            if ((publicProject != null && (publicProject.equals("on") || publicProject.equals("true")) && metadata.get("public").equals("false")) ||
                    (publicProject == null && metadata.get("public").equals("true"))) {
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
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();
        ProjectMinter p = new ProjectMinter();

        if (username == null) {
            throw new UnauthorizedRequestException("You must login");
        }
        if (!p.isProjectAdmin(username, projectId)) {
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
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response addUser(@FormParam("projectId") Integer projectId,
                            @FormParam("userId") Integer userId) {
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must login to access this service.");
        }

        // userId of 0 means create new user, using ajax to create user, shouldn't ever receive userId of 0
        if (userId == 0) {
            throw new BadRequestException("invalid userId");
        }


        ProjectMinter p = new ProjectMinter();
        if (!p.isProjectAdmin(username, projectId)) {
            p.close();
            throw new ForbiddenRequestException("You are not this project's admin");
        }
        p.addUserToProject(userId, projectId);
        p.close();

        return Response.ok("{\"success\": \"User has been successfully added to this project\"}").build();
    }

    /**
     * retrieve all members of a project
     * @param projectId
     * @return
     */
    @GET
    @Path("/getUsers/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectUsers(@PathParam("projectId") Integer projectId) {
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();
        ProjectMinter p = new ProjectMinter();

        if (username == null) {
            throw new UnauthorizedRequestException("You must login");
        }
        if (!p.isProjectAdmin(username, projectId)) {
            // only display system users to project admins
            throw new ForbiddenRequestException("You are not an admin to this project");
        }

        JSONObject response = p.getProjectUsers(projectId);
        p.close();
        return Response.ok(response.toJSONString()).build();
    }

    /**
     * Service used to retrieve a JSON representation of the project's a user is a member of.
     * @param accessToken
     * @return
     */
    @GET
    @Path("/listUserProjects")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserProjects() {
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

        if (username == null) {
            throw new UnauthorizedRequestException("authorization_error");
        }

        ProjectMinter p = new ProjectMinter();
        JSONArray projects = p.listUsersProjects(username);
        p.close();
        return Response.ok(projects.toJSONString()).build();
    }

    /**
     * Service used to save a fims template generator configuration
     * @param checkedOptions
     * @param configName
     * @param projectId
     * @return
     */
    @POST
    @Path("/saveTemplateConfig")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response saveTemplateConfig(@FormParam("checkedOptions") List<String> checkedOptions,
                                       @FormParam("configName") String configName,
                                       @FormParam("projectId") Integer projectId) {

        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To change the default config, talk to the project admin.\"}").build();
        }

        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to save a configuration.");
        }

        Database db = new Database();
        Integer userId = db.getUserId(username);
        db.close();

        ProjectMinter projectMinter = new ProjectMinter();

        if (projectMinter.configExists(configName, projectId)) {
            if (projectMinter.usersConfig(configName, projectId, userId)) {
                projectMinter.updateTemplateConfig(configName, projectId, userId, checkedOptions);
            } else {
                return Response.ok("{\"error\": \"A configuration with that name already exists, and you are not the owner.\"}").build();
            }
        } else {
            projectMinter.saveTemplateConfig(configName, projectId, userId, checkedOptions);
        }
        projectMinter.close();

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
        JSONArray configs = p.getTemplateConfigs(projectId);
        p.close();

        return Response.ok(configs.toJSONString()).build();
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
                                 @PathParam("projectId") Integer projectId) {
        if (configName.equalsIgnoreCase("default")) {
            return Response.ok("{\"error\": \"To remove the default config, talk to the project admin.\"}").build();
        }

        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

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

    @GET
    @Path("/getLatLongColumns/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatLongColumns(@PathParam("projectId") int projectId) {
        String decimalLatDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLatitude";
        String decimalLongDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLongitude";
        JSONObject response = new JSONObject();

        try {
            ProcessController pc = new ProcessController(projectId, null);
            Process p = new Process(null, uploadPath(), pc);

            Mapping mapping = p.getMapping();
            String defaultSheet = mapping.getDefaultSheetName();
            ArrayList<Attribute> attributeList = mapping.getAllAttributes(defaultSheet);

            response.put("data_sheet", defaultSheet);

            for (Attribute attribute : attributeList) {
                // when we find the column corresponding to the definedBy for lat and long, add them to the response
                if (attribute.getDefined_by().equalsIgnoreCase(decimalLatDefinedBy)) {
                    response.put("lat_column", attribute.getColumn());
                } else if (attribute.getDefined_by().equalsIgnoreCase(decimalLongDefinedBy)) {
                    response.put("long_column", attribute.getColumn());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new FimsRuntimeException(500, e);
        }
        return Response.ok(response.toJSONString()).build();
    }
}
