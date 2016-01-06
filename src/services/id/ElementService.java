package services.id;

import bcid.ResourceTypes;
import ezid.EzidService;
import biocode.fims.fimsExceptions.BadRequestException;
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

    /**
     * get all resourceTypes minus Dataset
     *
     * @return String with JSON response
     */
    @GET
    @Path("/resourceTypesMinusDataset}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response jsonSelectOptions() {
        ResourceTypes rts = new ResourceTypes();
        return Response.ok(rts.getAllButDatasetAsJSON().toJSONString()).build();
    }

    /**
     * Get all resourceTypes
     *
     * @return
     */
    @GET
    @Path("/resourceTypes")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resourceTypes() {
        ResourceTypes rts = new ResourceTypes();
        return Response.ok(rts.getAllAsJSON().toJSONString()).build();
    }
}