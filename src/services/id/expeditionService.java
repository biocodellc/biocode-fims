package services.id;

import auth.oAuth2.provider;
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
    public Response mint(@FormParam("expeditionCode") String expeditionCode,
                         @FormParam("bcid") String bcid,
                         @FormParam("projectId") Integer projectId) {
        expeditionMinter expedition;
        expedition = new expeditionMinter();
        expedition.attachReferenceToExpedition(expeditionCode, bcid, projectId);
        expedition.close();
        //String deepRootsString = expedition.getDeepRoots(expeditionCode,projectId);

        //return Response.ok("{\"success\": \"Data Elements Root: " + deepRootsString +"\"}").build();
        return Response.ok("{\"success\": \"Data Elements Root: " + expeditionCode +"\"}").build();
    }

    /**
     * validateExpedition service checks the status of a new expedition code on the server and directing consuming
     * applications on whether this user owns the expedition and if it exists within an project or not.
     * Responses are error, update, or insert (first term followed by a colon)
     *
     * @param expeditionCode
     * @param projectId
     * @param accessToken
     * @param ignore_user     if specified as true then we don't perform a check on what user owns the expedition
     *
     * @return
     */
    @GET
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/validateExpedition/{projectId}/{expeditionCode}")
    public Response mint(@PathParam("expeditionCode") String expeditionCode,
                         @PathParam("projectId") Integer projectId,
                         @QueryParam("access_token") String accessToken,
                         @QueryParam("ignore_user") Boolean ignore_user) {
        String username;

        // Decipher the expedition code
        try {
            expeditionCode = URLDecoder.decode(expeditionCode, "utf-8");
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
        // Get the userId
        database db = new database();
        Integer userId = db.getUserId(username);
        db.close();

        // Create the expeditionMinter object so we can test and validate it
        expeditionMinter expedition = new expeditionMinter();

        return Response.ok(expedition.validateExpedition(expeditionCode, projectId, ignore_user, userId)).build();
    }

    /**
     * Service for a user to mint a new expedition
     *
     * @param expeditionCode
     * @param expeditionTitle
     * @param projectId
     * @param isPublic
     * @param accessToken      (optional) the access token that represents the user who you are minting an expedition
     *                         on behalf.
     *
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mint(@FormParam("expeditionCode") String expeditionCode,
                         @FormParam("expeditionTitle") String expeditionTitle,
                         @FormParam("projectId") Integer projectId,
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

        // Get the userId
        database db = new database();
        Integer userId = db.getUserId(username);
        db.close();

        Integer expeditionId = null;
        expeditionMinter expedition = null;

        try {
            // Mint a expedition
            expedition = new expeditionMinter();
            expeditionId = expedition.mint(
                    expeditionCode,
                    expeditionTitle,
                    userId,
                    projectId,
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
                    expedition.printMetadata(expeditionId));
            sendEmail.start();
            */

            return Response.ok("{\"success\": \"Succesfully created expedition:<br>" +
                    expedition.printMetadataHTML(expeditionId) + "\"}").build();
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
    @Path("/{projectId}/{expedition}/{resourceAlias}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchAlias(@PathParam("expedition") String expedition,
                               @PathParam("projectId") Integer projectId,
                               @PathParam("resourceAlias") String resourceAlias) {
        try {
            expedition = URLDecoder.decode(expedition, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in expeditionService.fetchAlias method.", e);
        }

        resolver r = new resolver(expedition, projectId, resourceAlias);
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
    @Path("/deepRoots/{projectId}/{expedition}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchDeepRoots(@PathParam("expedition") String expedition,
                                   @PathParam("projectId") Integer projectId) {
        expeditionMinter expeditionMinter = new expeditionMinter();

        String response = expeditionMinter.getDeepRoots(expedition, projectId);

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
    @Path("/list/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listExpeditions(@PathParam("projectId") Integer projectId,
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
    @Path("resourcesAsTable/{expeditionId}")
    @Produces(MediaType.TEXT_HTML)
    public Response listResourcesAsTable(@PathParam("expeditionId") Integer expeditionId) {
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
    @Path("configurationAsTable/{expeditionId}")
    @Produces(MediaType.TEXT_HTML)
    public Response listConfigurationAsTable(@PathParam("expeditionId") Integer expeditionId) {
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
    @Path("datasetsAsTable/{expeditionId}")
    @Produces(MediaType.TEXT_HTML)
    public Response listDatasetsAsTable(@PathParam("expeditionId") Integer expeditionId) {
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
    @Path("/admin/listExpeditionsAsTable/{projectId}")
    @Produces(MediaType.TEXT_HTML)
    public Response listExpeditionAsTable(@PathParam("projectId") Integer projectId) {
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
     * The expeditionId's are specified simply by their internal expeditionId code
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
        Integer projectId = new Integer(data.remove("projectId").get(0));

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
    @Path("/publicExpedition/{projectId}/{expeditionCode}/{public_status}")
    public Response publicExpedition(
            @PathParam("projectId") Integer projectId,
            @PathParam("expeditionCode") String expeditionCode,
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

