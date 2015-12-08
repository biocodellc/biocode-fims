package services.id;

import bcid.ResourceTypes;
import bcid.dataGroupMinter;
import bcid.database;
import bcid.elementMinter;
import bcid.inputFileParser;
import fimsExceptions.BadRequestException;
import fimsExceptions.ServerErrorException;
import ezid.EZIDException;
import ezid.EZIDService;
import net.sf.json.JSONArray;
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
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * REST interface for creating elements, to be called from the interface or other consuming applications.
 */
@Path("elementService")
public class elementService {

    @Context
    static ServletContext context;
    static String bcidShoulder;
    static String doiShoulder;
    static SettingsManager sm;
    private static Logger logger = LoggerFactory.getLogger(elementService.class);

    /**
     * Load settings manager, set ontModelSpec. 
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();

        // Initialize ezid account
        EZIDService ezidAccount = new EZIDService();
        try {
            // Setup EZID account/login information
            ezidAccount.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));
        } catch (EZIDException e) {
            //TODO should we silence this exception?
            logger.warn("EZIDException trying to login.", e);
        }
    }

    /**
     * Populate select boxes for BCID service options
     *
     * @param 'resourceTypes|resourceTypesMinusDataset'
     *
     * @return String with JSON response
     */
    @GET
    @Path("/select/{select}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonSelectOptions(@PathParam("select") String select) {

        if (select.equalsIgnoreCase("resourceTypes")) {
            ResourceTypes rts = new ResourceTypes();
            return Response.ok(rts.getAllAsJSON()).build();
        } else if (select.equalsIgnoreCase("resourceTypesMinusDataset")) {
            ResourceTypes rts = new ResourceTypes();
            return Response.ok(rts.getAllButDatasetAsJSON()).build();
        } else {
            throw new BadRequestException("Invalid value. Must be either resourceTypes or resourceTypesMinusDataset");
        }
    }

    /**
     * Get resourceTypes as a TABLE
     *
     * @return
     */
    @GET
    @Path("/resourceTypes")
    @Produces(MediaType.TEXT_HTML)
    public Response htmlResourceTypes() {
        ResourceTypes rts = new ResourceTypes();
        return Response.ok(rts.getResourceTypesAsTable()).build();
    }


    /**
     * Create a bunch of BCIDs
     *
     * @param dataset_id
     * @param title
     * @param resourceType
     * @param data
     * @param doi
     * @param webaddress
     * @param request
     * @return
     */
    @POST
    @Path("/creator")
    @Produces(MediaType.APPLICATION_JSON)
    public Response creator(@FormParam("datasetList") Integer dataset_id,
                            @FormParam("title") String title,
                            @FormParam("resourceTypesMinusDataset") Integer resourceType,
                            @FormParam("data") String data,
                            @FormParam("doi") String doi,
                            @FormParam("webaddress") String webaddress,
                            @FormParam("graph") String graph,
                            @FormParam("suffixPassThrough") String stringSuffixPassThrough,
                            @Context HttpServletRequest request) {

        dataGroupMinter dataset;
        database db;
        Boolean suffixPassthrough = false;
        HttpSession session = request.getSession();
        String username = session.getAttribute("user").toString();


        // Initialize database
        db = new database();

        // Get the user_id
        Integer user_id = db.getUserId(username);

        db.close();

        // Request creation of new dataset
        if (dataset_id == 0) {

            // Format Input variables
            if (!stringSuffixPassThrough.isEmpty() &&
                    (stringSuffixPassThrough.equalsIgnoreCase("true") || stringSuffixPassThrough.equalsIgnoreCase("on"))) {
                suffixPassthrough = true;
            }

            // Some input form validation
            // TODO: create a generic way of validating this input form content
            if (resourceType == 0 ||
                            resourceType == ResourceTypes.SPACER1 ||
                            resourceType == ResourceTypes.SPACER2 ||
                            resourceType == ResourceTypes.SPACER3 ||
                            resourceType == ResourceTypes.SPACER4 ||
                            resourceType == ResourceTypes.SPACER5 ||
                            resourceType == ResourceTypes.SPACER6 ||
                            resourceType == ResourceTypes.SPACER7
                    ) {
                throw new BadRequestException("Must choose a valid concept!");
            }
            // TODO: check for valid local ID's, no reserved characters

            // Create a new dataset
            dataset = new dataGroupMinter(true, suffixPassthrough);
            // we don't know DOI or webaddress from this call, so we set them to NULL
            dataset.mint(
                    new Integer(sm.retrieveValue("bcidNAAN")),
                    user_id,
                    new ResourceTypes().get(resourceType).uri,
                    doi,
                    webaddress,
                    graph,
                    title,
                    false);
            // Load an existing dataset we've made already
        } else {
            dataset = new dataGroupMinter(dataset_id);

            // TODO: check that dataset.users_id matches the user that is logged in!

        }

        // Create a bcid Minter instance
        elementMinter minter = null;
        minter = new elementMinter(dataset.getDatasets_id());

        try {
            // Parse input file
            ArrayList elements = null;
            elements = new inputFileParser(data, dataset).elementArrayList;

            // Mint the list of identifiers
            String datasetUUID = null;
            datasetUUID = minter.mintList(elements);

            // Array of identifiers, or an error message
            String returnVal = JSONArray.fromObject(minter.getIdentifiers(datasetUUID)).toString();


            // Send an Email that this completed
           /* sendEmail sendEmail = new sendEmail(sm.retrieveValue("mailUser"),
                    sm.retrieveValue("mailPassword"),
                    sm.retrieveValue("mailFrom"),
                    sm.retrieveValue("mailTo"),
                    "New Elements From " + username,
                    returnVal);
            sendEmail.start();
            */
            dataset.close();
            minter.close();
            return Response.ok(returnVal).build();
        } catch (URISyntaxException e) {
            dataset.close();
            minter.close();
            throw new ServerErrorException("Server Error", "URISyntaxException while parsing input file: " + data, e);
        } catch (IOException e) {
            dataset.close();
            minter.close();
            throw new ServerErrorException("Server Error", "IOException while parsing input file: " + data, e);
        }
    }
}