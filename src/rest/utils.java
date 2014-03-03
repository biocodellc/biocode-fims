package rest;

import run.configurationFileFetcher;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;


/**
 * Biocode-FIMS utility services
 */
public class utils {
    @Context
    static ServletContext context;

    /**
     * Refresh the configuration File cache
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/refreshCache/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJson(
            @FormParam("project_id") Integer project_id
    ) throws Exception {

        new configurationFileFetcher(project_id, uploadPath(), false).getOutputFile();

        return Response.ok("").build();

    }


    /**
     * Get real path of the uploads folder from context.
     * Needs context to have been injected before.
     *
     * @return Real path of the uploads folder with ending slash.
     */
    static String uploadPath() {
        return context.getRealPath("tripleOutput") + File.separator;
    }


}

