package services.id;

import auth.oauth2.provider;
import bcid.bcid;
import bcid.Renderer.JSONRenderer;
import bcid.Renderer.Renderer;
import bcid.ResourceTypes;
import bcid.bcidMinter;
import bcid.database;
import bcid.expeditionMinter;
import fimsExceptions.BadRequestException;
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
import javax.ws.rs.core.Response;
import java.util.Hashtable;

/**
 * REST interface calls for working with data groups.    This includes creating a group, looking up
 * groups by user associated with them, and JSON representation of group metadata.
 */
@Path("groupService")
public class groupService {

    final static Logger logger = LoggerFactory.getLogger(groupService.class);

    @Context
    static ServletContext context;
    static String bcidShoulder;
    static String doiShoulder;
    //static SettingsManager sm;

    /**
     * Load settings manager, set ontModelSpec.
     */

    /**
     * Create a data group
     *
     * @param doi
     * @param webaddress
     * @param title
     * @param request
     *
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response mint(@FormParam("doi") String doi,
                         @FormParam("webaddress") String webaddress,
                         @FormParam("graph") String graph,
                         @FormParam("title") String title,
                         @FormParam("resourceType") String resourceTypeString,
                         @FormParam("resourceTypesMinusDataset") Integer resourceTypesMinusDataset,
                         @FormParam("suffixPassThrough") String stringSuffixPassThrough,
                         @FormParam("finalCopy") @DefaultValue("false") Boolean finalCopy,
                         @QueryParam("access_token") String accessToken,
                         @Context HttpServletRequest request) {

        // If resourceType is specified by an integer, then use that to set the String resourceType.
        // If the user omits
        try {
            if (resourceTypesMinusDataset != null && resourceTypesMinusDataset > 0) {
                resourceTypeString = new ResourceTypes().get(resourceTypesMinusDataset).uri;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new BadRequestException("BCID System Unable to set resource type",
                    "There was an error retrieving the resource type uri. Did you provide a valid resource type?");
        }

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
            throw new UnauthorizedRequestException("You must be logged in to create a data group.");
        }

        Boolean suffixPassthrough;
        // Format Input variables
        suffixPassthrough = !stringSuffixPassThrough.isEmpty() && (stringSuffixPassThrough.equalsIgnoreCase("true") ||
                stringSuffixPassThrough.equalsIgnoreCase("on"));

        // Initialize settings manager
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        // Create a Bcid
        database db = new database();
        // Check for remote-user
        Integer user_id = db.getUserId(username);
        db.close();

        // Mint the data group
        bcidMinter bcidMinter = new bcidMinter(suffixPassthrough);
        String prefix = bcidMinter.createEntityBcid(user_id, resourceTypeString, webaddress, graph, doi, finalCopy);
        bcidMinter.close();

        return Response.ok("{\"prefix\": \"" + prefix + "\"}").build();
    }

    /**
     * Return a JSON representation of bcids metadata
     *
     * @param bcid_id
     *
     * @return
     */
    @GET
    @Path("/metadata/{bcid_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String run(@PathParam("bcid_id") Integer bcid_id) {
        bcid bcid = new bcid(bcid_id);
        Renderer renderer = new JSONRenderer();

        return "[" + renderer.render(bcid) + "]";
    }

    /**
     * Return JSON response showing data groups available to this user
     *
     * @return String with JSON response
     */
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response bcidList(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your data groups.");
        }

        bcidMinter d = new bcidMinter();
        String response = d.bcidList(username);
        d.close();

        return Response.ok(response).build();
    }

    /**
     * Return HTML response showing a table of groups belonging to this user
     *
     * @return String with HTML response
     */
    @GET
    @Path("/listUserBCIDsAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listUserBCIDsAsTable(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your BCIDs.");
        }

        bcidMinter d = new bcidMinter();
        String response = d.bcidTable(username);
        d.close();

        return Response.ok(response).build();
    }

    /**
     * Return HTML response showing a table of groups belonging to this user
     *
     * @return String with HTML response
     */
    @GET
    @Path("/listUserExpeditionsAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response listUserExpeditionsAsTable(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your expeditions.");
        }

        expeditionMinter e = new expeditionMinter();
        String tablename = e.expeditionTable(username);
        e.close();
        return Response.ok(tablename).build();
    }

    /**
     * returns an HTML table used to edit a bcid's configuration.
     *
     * @param prefix
     *
     * @return
     */
    @GET
    @Path("/dataGroupEditorAsTable")
    @Produces(MediaType.TEXT_HTML)
    public Response bcidEditorAsTable(@QueryParam("ark") String prefix,
                                      @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to edit your BCID's configuration.");
        }

        if (prefix == null) {
            throw new BadRequestException("You must provide an \"ark\" query parameter.");
        }

        bcidMinter d = new bcidMinter();
        String response = d.bcidEditorAsTable(username, prefix);
        d.close();
        return Response.ok(response).build();
    }

    /**
     * Service to update a bcid's configuration.
     *
     * @param doi
     * @param webaddress
     * @param title
     * @param resourceTypeString
     * @param resourceTypesMinusDataset
     * @param stringSuffixPassThrough
     * @param prefix
     *
     * @return
     */
    @POST
    @Path("/dataGroup/update")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response bcidUpdate(@FormParam("doi") String doi,
                               @FormParam("webaddress") String webaddress,
                               @FormParam("title") String title,
                               @FormParam("resourceType") String resourceTypeString,
                               @FormParam("resourceTypesMinusDataset") Integer resourceTypesMinusDataset,
                               @FormParam("suffixPassThrough") String stringSuffixPassThrough,
                               @FormParam("prefix") String prefix,
                               @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        Object username = session.getAttribute("user");
        Hashtable<String, String> config;
        Hashtable<String, String> update = new Hashtable<String, String>();

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to edit BCID's.");
        }

        // get this BCID's config

        bcidMinter b = new bcidMinter();
        config = b.getBcidConfig(prefix, username.toString());

        if (resourceTypesMinusDataset != null && resourceTypesMinusDataset > 0) {
            resourceTypeString = new ResourceTypes().get(resourceTypesMinusDataset).string;
        }

        // compare every field and if they don't match, add them to the update hashtable
        if (doi != null && (!config.containsKey("doi") || !config.get("doi").equals(doi))) {
            update.put("doi", doi);
        }
        if (webaddress != null && (!config.containsKey("webaddress") || !config.get("webaddress").equals(webaddress))) {
            update.put("webaddress", webaddress);
        }
        if (!config.containsKey("title") || !config.get("title").equals(title)) {
            update.put("title", title);
        }
        if (!config.containsKey("resourceType") || !config.get("resourceType").equals(resourceTypeString)) {
            update.put("resourceTypeString", resourceTypeString);
        }
        if ((stringSuffixPassThrough != null && (stringSuffixPassThrough.equals("on") || stringSuffixPassThrough.equals("true")) && config.get("suffix").equals("false")) ||
                (stringSuffixPassThrough == null && config.get("suffix").equals("true"))) {
            if (stringSuffixPassThrough != null && (stringSuffixPassThrough.equals("on") || stringSuffixPassThrough.equals("true"))) {
                update.put("suffixPassthrough", "true");
            } else {
                update.put("suffixPassthrough", "false");
            }
        }

        if (update.isEmpty()) {
            b.close();
            return Response.ok("{\"success\": \"Nothing needed to be updated.\"}").build();
        // try to update the config by calling d.updateBcidConfig
        } else if (b.updateBcidConfig(update, prefix, username.toString())) {
            b.close();
            return Response.ok("{\"success\": \"BCID successfully updated.\"}").build();
        } else {
            b.close();
            // if we are here, the bcid wasn't found
            throw new BadRequestException("Bcid wasn't found");
        }

    }
}
