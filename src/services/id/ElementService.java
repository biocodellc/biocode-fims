package services.id;

import bcid.ResourceTypes;
import services.BiocodeFimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST interface for creating elements, to be called from the interface or other consuming applications.
 */
@Path("elementService")
public class ElementService extends BiocodeFimsService {

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