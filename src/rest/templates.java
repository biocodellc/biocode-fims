package rest;

import run.configurationFileFetcher;
import run.process;
import settings.FIMSException;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;

import javax.ws.rs.core.Context;


/**
 * Query interface for Biocode-fims expedition
 */
@Path("templates")
public class templates {
    @Context
    static ServletContext context;

    /**
     * Return the available attributes for a particular graph
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/attributes/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateCheckboxes(
            @QueryParam("project_id") Integer project_id) throws Exception {

        process p = null;
        File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

        try {
            p = new process(
                    uploadPath(),
                    configFile
            );
        } catch (FIMSException e) {
            return Response.ok("\nError: " + e.getMessage()).build();
        }

        // Write the response to a String Variable
        String response = p.template();

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
        }
    }
    /**
     * Return a definition for a particular column
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/definition/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefinitions(
             @QueryParam("project_id") Integer project_id,
            @QueryParam("column_name") String column_name) throws Exception {

        process p = null;
        File configFile = new configurationFileFetcher(project_id, uploadPath(),true).getOutputFile();

        try {
            p = new process(
                    uploadPath(),
                    configFile
            );
        } catch (FIMSException e) {
            //e.printStackTrace();
            return Response.ok("\nError: " + e.getMessage()).build();
        }

        // Write the response to a String Variable
        String response = p.templateDefinition(column_name);

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
        }
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

