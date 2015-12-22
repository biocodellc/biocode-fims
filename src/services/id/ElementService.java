package services.id;

import bcid.ResourceTypes;
import ezid.EzidService;
import fimsExceptions.BadRequestException;
import ezid.EzidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.SettingsManager;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST interface for creating elements, to be called from the interface or other consuming applications.
 */
@Path("elementService")
public class ElementService {

    @Context
    static ServletContext context;
    static SettingsManager sm;
    private static Logger logger = LoggerFactory.getLogger(ElementService.class);

    /**
     * Load settings manager, set ontModelSpec. 
     */
    static {
        // Initialize settings manager
        sm = SettingsManager.getInstance();
        sm.loadProperties();

        // Initialize ezid account
        EzidService ezidAccount = new EzidService();
        try {
            // Setup EZID account/login information
            ezidAccount.login(sm.retrieveValue("eziduser"), sm.retrieveValue("ezidpass"));
        } catch (EzidException e) {
            //TODO should we silence this exception?
            logger.warn("EzidException trying to login.", e);
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
        ResourceTypes rts = new ResourceTypes();

        if (select.equalsIgnoreCase("resourceTypes")) {
            return Response.ok(rts.getAllAsJSON()).build();
        } else if (select.equalsIgnoreCase("resourceTypesMinusDataset")) {
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
     * @param bcidsId
     * @param title
     * @param resourceType
     * @param data
     * @param doi
     * @param webAddress
     * @param request
     * @return
     */
   /* @POST
    @Path("/creator")
    @Produces(MediaType.APPLICATION_JSON)
    public Response creator(@FormParam("datasetList") Integer bcidsId,
                            @FormParam("title") String title,
                            @FormParam("resourceTypesMinusDataset") Integer resourceType,
                            @FormParam("data") String data,
                            @FormParam("doi") String doi,
                            @FormParam("webAddress") String webAddress,
                            @FormParam("graph") String graph,
                            @FormParam("suffixPassThrough") String stringSuffixPassThrough,
                            @Context HttpServletRequest request) {

        BcidMinter dataset;
        Database db;
        Boolean suffixPassthrough = false;
        HttpSession session = request.getSession();
        String username = session.getAttribute("user").toString();


        // Initialize Database
        db = new Database();

        // Get the userId
        Integer userId = db.getUserId(username);

        db.close();

        // Request creation of new dataset
        if (bcidsId == 0) {

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
            dataset = new BcidMinter(true, suffixPassthrough);
            // we don't know DOI or webAddress from this call, so we set them to NULL
            dataset.mint(
                    new Integer(sm.retrieveValue("bcidNAAN")),
                    userId,
                    new ResourceTypes().get(resourceType).uri,
                    doi,
                    webAddress,
                    graph,
                    title,
                    false);
            // Load an existing dataset we've made already
        } else {
            dataset = new BcidMinter(bcidsId);

            // TODO: check that dataset.userId matches the user that is logged in!

        }

        // Create a Bcid Minter instance
        elementMinter minter = null;
        minter = new elementMinter(dataset.getBcidsId());

        try {
            // Parse input file
            ArrayList elements = null;
            elements = new InputFileParser(data, dataset).elementArrayList;

            // Mint the list of identifiers
            String datasetUUID = null;
            datasetUUID = minter.mintList(elements);

            // Array of identifiers, or an error message
            String returnVal = JSONArray.fromObject(minter.getIdentifiers(datasetUUID)).toString();


            // Send an Email that this completed
           /* SendEmail SendEmail = new SendEmail(sm.retrieveValue("mailUser"),
                    sm.retrieveValue("mailPassword"),
                    sm.retrieveValue("mailFrom"),
                    sm.retrieveValue("mailTo"),
                    "New Elements From " + username,
                    returnVal);
            SendEmail.start();

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
    }*/
}