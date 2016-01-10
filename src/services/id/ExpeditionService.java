package services.id;

import auth.oauth2.OAuthProvider;
import bcid.Database;
import bcid.ExpeditionMinter;
import bcid.ProjectMinter;
import bcid.Resolver;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ForbiddenRequestException;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.BiocodeFimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * REST interface calls for working with expeditions.  This includes creating, updating and deleting expeditions.
 */
@Path("expeditionService")
public class ExpeditionService extends BiocodeFimsService {

    private static Logger logger = LoggerFactory.getLogger(ExpeditionService.class);

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/associate")
    public Response mint(@FormParam("expeditionCode") String expeditionCode,
                         @FormParam("bcid") String bcid,
                         @FormParam("projectId") Integer projectId) {
        ExpeditionMinter expedition;
        expedition = new ExpeditionMinter();
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
                         @QueryParam("ignore_user") Boolean ignore_user) {
        // Decipher the expedition code
        try {
            expeditionCode = URLDecoder.decode(expeditionCode, "utf-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException in ExpeditionService.mint method.", e);
        }

        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("your session has expired or you have not yet logged in.<br>You may "
                    + "have to logout and then re-login.");
        }
        // Get the userId
        Database db = new Database();
        Integer userId = db.getUserId(username);
        db.close();

        // Create the ExpeditionMinter object so we can test and validate it
        ExpeditionMinter expedition = new ExpeditionMinter();

        return Response.ok(expedition.validateExpedition(expeditionCode, projectId, ignore_user, userId)).build();
    }

    /**
     * Service for a user to mint a new expedition
     *
     * @param expeditionCode
     * @param expeditionTitle
     * @param projectId
     * @param isPublic
     *
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mint(@FormParam("expeditionCode") String expeditionCode,
                         @FormParam("expeditionTitle") String expeditionTitle,
                         @FormParam("projectId") Integer projectId,
                         @FormParam("public") Boolean isPublic) {
        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("User is not authorized to create a new expedition.");
        }

        if (isPublic == null) {
            isPublic = true;
        }

        // Get the userId
        Database db = new Database();
        Integer userId = db.getUserId(username);
        db.close();

        Integer expeditionId = null;
        ExpeditionMinter expedition = null;

        try {
            // Mint a expedition
            expedition = new ExpeditionMinter();
            expeditionId = expedition.mint(
                    expeditionCode,
                    expeditionTitle,
                    userId,
                    projectId,
                    isPublic
            );

            // Initialize settings manager

            // Send an Email that this completed
            // Not all clients have sendMail on... turning this off for now.  Need more secure way to monitor anyway
            /* SendEmail SendEmail = new SendEmail(
                    sm.retrieveValue("mailUser"),
                    sm.retrieveValue("mailPassword"),
                    sm.retrieveValue("mailFrom"),
                    sm.retrieveValue("mailTo"),
                    "New Expedition",
                    expedition.printMetadata(expeditionId));
            SendEmail.start();
            */

            return Response.ok("{\"success\": \"Succesfully created expedition:<br>" +
                    expedition.printMetadataHTML(expeditionId) + "\"}").build();
        } catch (FimsException e) {
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
        ExpeditionMinter e = new ExpeditionMinter();
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
            logger.warn("UnsupportedEncodingException in ExpeditionService.fetchAlias method.", e);
        }

        Resolver r = new Resolver(expedition, projectId, resourceAlias);
        String response = r.getIdentifier();
        r.close();
        if (response == null) {
            return Response.status(Response.Status.NO_CONTENT).entity("{\"identifier\": \"\"}").build();
        } else {
            //System.out.println("fetchAlias = " + response);
            return Response.ok("{\"identifier\": \"" + response + "\"}").build();
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
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();

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
    public Response listExpeditions(@PathParam("projectId") Integer projectId) {
        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your expeditions.");
        }

        ExpeditionMinter e = new ExpeditionMinter();
        JSONArray expeditions = e.listExpeditions(projectId, username.toString());
        e.close();

        return Response.ok(expeditions.toJSONString()).build();
    }

    /**
     * Returns the expedition's resources
     *
     * @param expeditionId
     *
     * @return
     */
    @GET
    @Path("resources/{expeditionId}")
    @Produces(MediaType.TEXT_HTML)
    public Response getResources(@PathParam("expeditionId") Integer expeditionId) {
        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this expedition's resources.");
        }

        ExpeditionMinter e = new ExpeditionMinter();

        JSONArray resources = e.getResources(expeditionId);

        e.close();
        return Response.ok(resources.toJSONString()).build();
    }

   /**
     * Service to retrieve an expedition's metadata
     *
     * @param expeditionId
     *
     * @return
     */
    @GET
    @Path("metadata/{expeditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMetadata(@PathParam("expeditionId") Integer expeditionId) {
        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this expedition's configuration.");
        }

        ExpeditionMinter e = new ExpeditionMinter();
        JSONObject metadata = e.getMetadata(expeditionId);
        e.close();
        return Response.ok(metadata.toJSONString()).build();
    }

    /**
     * Service to retrieve an expedition's datasets
     *
     * @param expeditionId
     *
     * @return
     */
    @GET
    @Path("datasets/{expeditionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasets(@PathParam("expeditionId") Integer expeditionId) {
        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this expedition's datasets.");
        }

        ExpeditionMinter e = new ExpeditionMinter();
        JSONArray datasets = e.getDatasets(expeditionId);
        e.close();
        return Response.ok(datasets.toJSONString()).build();
    }

    /**
     * Service to retrieve all of the project's expeditions. For use by project admin only.
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/admin/list/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExpeditions(@PathParam("projectId") Integer projectId) {
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view this project's expeditions.");
        }

        ProjectMinter projectMinter = new ProjectMinter();
        if (!projectMinter.isProjectAdmin(username, projectId)) {
            throw new ForbiddenRequestException("You must be this project's admin in order to view its expeditions.");
        }

        ExpeditionMinter e = new ExpeditionMinter();
        JSONArray expeditions = e.getExpeditions(projectId, username);
        e.close();
        return Response.ok(expeditions.toJSONString()).build();
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
        OAuthProvider provider = new OAuthProvider();
        String username = provider.validateToken(accessToken);
        provider.close();
        Integer projectId = new Integer(data.remove("projectId").get(0));

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to update an expedition's public status.");
        }

        ProjectMinter p = new ProjectMinter();
        Boolean projectAdmin = p.isProjectAdmin(username, projectId);
        p.close();

        if (!projectAdmin) {
            throw new ForbiddenRequestException("You must be this project's admin in order to update a project expedition's public status.");
        }
        ExpeditionMinter e = new ExpeditionMinter();

        e.updateExpeditionsPublicStatus(data, projectId);
        e.close();
        return Response.ok("{\"success\": \"successfully updated.\"}").build();
    }

    /**
     * Service to update a single expedition Bcid
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
    @Path("/publicExpedition/{projectId}/{expeditionCode}/{publicStatus}")
    public Response publicExpedition(
            @PathParam("projectId") Integer projectId,
            @PathParam("expeditionCode") String expeditionCode,
            @PathParam("publicStatus") Boolean publicStatus) {

        OAuthProvider p = new OAuthProvider();
        String username = p.validateToken(accessToken);
        p.close();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to update an expedition's public status.");
        }

        Database db = new Database();
        ExpeditionMinter e = new ExpeditionMinter();

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

