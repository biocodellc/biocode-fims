package rest;

import run.process;
import run.processController;
import run.templateProcessor;
import settings.bcidConnector;
import utils.stringGenerator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.List;

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
            @QueryParam("project_id") Integer project_id,
                             @Context HttpServletRequest request) throws Exception {

        HttpSession session = request.getSession();



        //File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(project_id,uploadPath(),true);

        // Write the all of the checkbox definitions to a String Variable
        String response = t.printCheckboxes();

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
        }
    }
     /**
     * Return the abstract for a particular graph
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/abstract/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(
            @QueryParam("project_id") Integer project_id) throws Exception {

        //File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(project_id,uploadPath(),true);

        // Write the all of the checkbox definitions to a String Variable
        String response = t.printAbstract();

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
        }
    }

    @POST
    @Path("/createExcel/")
    @Produces("application/vnd.ms-excel")
    public Response createExcel(
            @FormParam("fields") List<String> fields,
            @FormParam("project_id") Integer project_id,
            @FormParam("accession_number") Integer accessionNumber,
            @FormParam("dataset_code") String datasetCode,
            @Context HttpServletRequest request ) throws Exception {

        // Create the configuration file
        //File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

        if (accessionNumber != null || datasetCode != null) {
            if (accessionNumber == null || datasetCode == null) {
                return Response.status(400).entity("{\"error\": \"" +
                        " Both an Accession Number and a Dataset Code are required if this is an NMNH project.").build();
            // only need to check that datasetCode is valid since an exception would have been thrown if accessionNumber
            // wasn't an Integer
            } else if (!datasetCode.matches("^\\w{4,16}$")) {
                return Response.status(400).entity("{\"error\": \"The Dataset Code must be an alphanumeric between" +
                        " 4 and 16 characters.").build();
            }

        }

        // Check if the project is an NMNH project
        processController processController = new processController(project_id, datasetCode);

        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");
        bcidConnector bcidConnector = new bcidConnector(accessToken, refreshToken);

        process p = new process(
                null,
                uploadPath(),
                bcidConnector,
                processController);
        if (p.isNMNHProject()) {
            if (accessionNumber == null || datasetCode == null) {
                return Response.status(400).entity("{\"error\": " +
                    "\"This is an NMNH project. Accession number and Dataset Code are required.}").build();
            } else {
                // create the expedition
                p.runExpeditionCreate();
            }
        }

        templateProcessor t;
        // Create the template processor which handles all functions related to the template, reading, generation
        if (accessionNumber != null) {
            t = new templateProcessor(project_id, uploadPath(), true,
                    accessionNumber, datasetCode);
        } else {
            t = new templateProcessor(project_id, uploadPath(), true);
        }

        // Set the default sheet-name
        String defaultSheetname = t.getMapping().getDefaultSheetName();


        File file = null;
        try {
            file = t.createExcelFile(defaultSheetname, uploadPath(), fields);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(204).build();
        }

        // Catch a null file and return 204
        if (file == null)
            return Response.status(204).build();

        // Return response
        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition",
                "attachment; filename=" + file.getName());
        return response.build();
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

        //File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
         templateProcessor t = new templateProcessor(project_id, uploadPath(), true);

        // Write the response to a String Variable
        String response = t.definition(column_name);

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
