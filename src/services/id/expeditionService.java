package services.id;

import auth.oauth2.provider;
import bcid.database;
import bcid.expeditionMinter;
import bcid.projectMinter;
import bcid.resolver;
import fimsExceptions.FIMSException;
import fimsExceptions.BadRequestException;
import fimsExceptions.ForbiddenRequestException;
import fimsExceptions.UnauthorizedRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * REST interface calls for working with expeditions.  This includes creating, updating and deleting expeditions.
 */
@Path("expeditionService")
public class expeditionService {

    @Context
    static ServletContext context;
    @Context
    HttpServletRequest request;

    private static Logger logger = LoggerFactory.getLogger(expeditionService.class);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/associate")
    public Response mint(@FormParam("expedition_code") String expedition_code,
                         @FormParam("bcid") String bcid,
                         @FormParam("project_id") Integer project_id) {
        expeditionMinter expedition;
        expedition = new expeditionMinter();
        expedition.attachReferenceToExpedition(expedition_code, bcid, project_id);
        expedition.close();
        //String deepRootsString = expedition.getDeepRoots(expedition_code,project_id);

        //return Response.ok("{\"success\": \"Data Elements Root: " + deepRootsString +"\"}").build();
        return Response.ok("{\"success\": \"Data Elements Root: " + expedition_code +"\"}").build();
    }

    /**
     * validateExpedition service checks the status of a new expedition code on the server and directing consuming
     * applications on whether this user owns the expedition and if it exists within an project or not.
     * Responses are error, update, or insert (first term followed by a colon)
     *
     * @param expedition_code
     * @param project_id
     * @param accessToken
     * @param ignore_user     if specified as true then we don't perform a check on what user owns the expedition
     *
     * @return
     */
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/validateExpedition/{project_id}/{expedition_code}")
    public Response mint(@PathParam("expedition_code") String expedition_code,
                         @PathParam("project_id") Integer project_id,
                         @QueryParam("access_token") String accessToken,
                         @QueryParam("ignore_user") Boolean ignore_user) {
        String username;

        // Decipher the expedition code
        try {
            expedition_code = URLDecoder.decode(expedition_code, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in expeditionService.mint method.", e);
        }

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
            throw new UnauthorizedRequestException("your session has expired or you have not yet logged in.<br>You may "
                    + "have to logout and then re-login.");
        }
        // Get the user_id
        database db = new database();
        Integer user_id = db.getUserId(username);
        db.close();

        // Create the expeditionMinter object so we can test and validate it
        expeditionMinter expedition = new expeditionMinter();

        return Response.ok(expedition.validateExpedition(expedition_code, project_id, ignore_user, user_id)).build();
    }

    /**
     * Service for a user to mint a new expedition
     *
     * @param expedition_code
     * @param expedition_title
     * @param project_id
     * @param isPublic
     * @param accessToken      (optional) the access token that represents the user who you are minting an expedition
     *                         on behalf.
     *
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mint(@FormParam("expedition_code") String expedition_code,
                         @FormParam("expedition_title") String expedition_title,
                         @FormParam("project_id") Integer project_id,
                         @FormParam("public") Boolean isPublic,
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

        if (username == null) {
            throw new UnauthorizedRequestException("User is not authorized to create a new expedition.");
        }

        if (isPublic == null) {
            isPublic = true;
        }

        // Get the user_id
        database db = new database();
        Integer user_id = db.getUserId(username);
        db.close();

        Integer expedition_id = null;
        expeditionMinter expedition = null;

        try {
            // Mint a expedition
            expedition = new expeditionMinter();
            expedition_id = expedition.mint(
                    expedition_code,
                    expedition_title,
                    user_id,
                    project_id,
                    isPublic
            );

            // Initialize settings manager
            SettingsManager sm = SettingsManager.getInstance();
            sm.loadProperties();

            // Send an Email that this completed
            // Not all clients have sendMail on... turning this off for now.  Need more secure way to monitor anyway
            /* sendEmail sendEmail = new sendEmail(
                    sm.retrieveValue("mailUser"),
                    sm.retrieveValue("mailPassword"),
                    sm.retrieveValue("mailFrom"),
                    sm.retrieveValue("mailTo"),
                    "New Expedition",
                    expedition.printMetadata(expedition_id));
            sendEmail.start();
            */

            return Response.ok("{\"success\": \"Succesfully created expedition:<br>" +
                    expedition.printMetadataHTML(expedition_id) + "\"}").build();
        } catch (FIMSException e) {
            throw new BadRequestException(e.getMessage());
        } finally {
            expedition.close();
        }
    }

    /**
     * Given a graph name, return metadata
     *
     * @param graph
     *
     * @return
     */
    @GET
    @Path("/graphMetadata/{graph}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGraphMetadata(@PathParam("graph") String graph) {
        expeditionMinter e = new expeditionMinter();
        String response = e.getGraphMetadata(graph);
        e.close();
        return Response.ok(response).build();
    }

    /**
     * Given a expedition code and a resource alias, return a BCID
     *
     * @param expedition
     * @param resourceAlias
     *
     * @return
     */
    @GET
    @Path("/{project_id}/{expedition}/{resourceAlias}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAlias(@PathParam("expedition") String expedition,
                               @PathParam("project_id") Integer project_id,
                               @PathParam("resourceAlias") String resourceAlias) {
        try {
            expedition = URLDecoder.decode(expedition, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in expeditionService.fetchAlias method.", e);
        }

        resolver r = new resolver(expedition, project_id, resourceAlias);
        String response = r.getArk();
        r.close();
        if (response == null) {
            return Response.status(Response.Status.NO_CONTENT).entity("{\"ark\": \"\"}").build();
        } else {
            //System.out.println("fetchAlias = " + response);
            return Response.ok("{\"ark\": \"" + response + "\"}").build();
        }
    }

    /**
     * Given an project and a expedition code return a list of resource Types associated with it
     *
     * @param expedition
     *
     * @return
     */
    @GET
    @Path("/deepRoots/{project_id}/{expedition}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchDeepRoots(@PathParam("expedition") String expedition,
                                   @PathParam("project_id") Integer project_id) {
        expeditionMinter expeditionMinter = new expeditionMinter();

        String response = expeditionMinter.getDeepRoots(expedition, project_id);

        expeditionMinter.close();

        return Response.ok(response).build();
    }

    /**
     * Return a JSON representation of the expedition's that a user is a member of
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/list/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listExpeditions(@PathParam("project_id") Integer projectId,
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

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your expeditions.");
        }

        expeditionMinter e = new expeditionMinter();

        String response = e.listExpeditions(projectId, username.toString());

        e.close();
        return Response.ok(response).build();
    }

    /**
     * Returns an HTML table of an expedition's resources
     *
     * @param expeditionId
     *
     * @return
     */
    @GET
    @Path("resourcesAsTable/{expedition_id}")
    @Produces(MediaType.TEXT_HTML)
    public Response listResourcesAsTable(@PathParam("expedition_id") Integer expeditionId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this expedition's resources.");
        }

        expeditionMinter e = new expeditionMinter();

        String response = e.listExpeditionResourcesAsTable(expeditionId);

        e.close();
        return Response.ok(response).build();
    }

   /**
     * Service to retrieve an expedition's configuration as an HTML table.
     *
     * @param expeditionId
     *
     * @return
     */
    @GET
    @Path("configurationAsTable/{expedition_id}")
    @Produces(MediaType.TEXT_HTML)
    public Response listConfigurationAsTable(@PathParam("expedition_id") Integer expeditionId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this expedition's configuration.");
        }

        expeditionMinter e = new expeditionMinter();
        String results = e.listExpeditionConfigurationAsTable(expeditionId);
        e.close();
        return Response.ok(results).build();
    }

    /**
     * Service to retrieve an expedition's datasets as an HTML table.
     *
     * @param expeditionId
     *
     * @return
     */
    @GET
    @Path("datasetsAsTable/{expedition_id}")
    @Produces(MediaType.TEXT_HTML)
    public Response listDatasetsAsTable(@PathParam("expedition_id") Integer expeditionId) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this expedition's datasets.");
        }

        expeditionMinter e = new expeditionMinter();
        String results = e.listExpeditionDatasetsAsTable(expeditionId);
        e.close();
        return Response.ok(results).build();
    }

    /**
     * Service to retrieve all of the project's expeditions. For use by project admin only.
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/admin/listExpeditionsAsTable/{project_id}")
    @Produces(MediaType.TEXT_HTML)
    public Response listExpeditionAsTable(@PathParam("project_id") Integer projectId) {
        HttpSession session = request.getSession();
        Object admin = session.getAttribute("projectAdmin");
        Object username = session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this project's expeditions.");
        }
        if (admin == null) {
            throw new ForbiddenRequestException("You must be this project's admin in order to view its expeditions.");
        }

        expeditionMinter e = new expeditionMinter();
        String response = e.listExpeditionsAsTable(projectId, username.toString());
        e.close();
        return Response.ok(response).build();
    }


    /**
     * Service to set/unset the public attribute of a set of expeditions specified in a MultivaluedMap
     * The expedition_id's are specified simply by their internal expedition_id code
     *
     * @param data
     *
     * @return
     */
    @POST
    @Path("/admin/publicExpeditions")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response publicExpeditions(MultivaluedMap<String, String> data) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");
        Integer projectId = new Integer(data.remove("project_id").get(0));

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to update an expedition's public status.");
        }

        database db = new database();
        projectMinter p = new projectMinter();
        Integer userId = db.getUserId(username.toString());
        Boolean projectAdmin = p.userProjectAdmin(userId, projectId);
        db.close();
        p.close();

        if (!projectAdmin) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update a project expedition's public status.");
        }
        expeditionMinter e = new expeditionMinter();

        e.updateExpeditionsPublicStatus(data, projectId);
        e.close();
        return Response.ok("{\"success\": \"successfully updated.\"}").build();
    }

    /**
     * Service to update a single expedition bcid
     *
     * @param projectId
     * @param expeditionCode
     * @param publicStatus
     *
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/publicExpedition/{project_id}/{expedition_code}/{public_status}")
    public Response publicExpedition(
            @PathParam("project_id") Integer projectId,
            @PathParam("expedition_code") String expeditionCode,
            @PathParam("public_status") Boolean publicStatus,
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

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to update an expedition's public status.");
        }

        database db = new database();
        expeditionMinter e = new expeditionMinter();

        Integer userId = db.getUserId(username.toString());


        // Update the expedition public status for what was just passed in

        if (e.updateExpeditionPublicStatus(userId, expeditionCode, projectId, publicStatus)) {
            e.close();
            return Response.ok("{\"success\": \"successfully updated.\"}").build();
        } else {
            e.close();
            return Response.ok("{\"success\": \"nothing to update.\"}").build();
        }
    }

}

