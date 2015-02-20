package rest;

import run.process;
import run.processController;
import run.templateProcessor;
import settings.FIMSRuntimeException;
import settings.bcidConnector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
     */
    @GET
    @Path("/attributes/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateCheckboxes(
            @QueryParam("project_id") Integer project_id,
            @Context HttpServletRequest request) {

        HttpSession session = request.getSession();


        //File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(project_id, uploadPath(), true);

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
     */
    @GET
    @Path("/abstract/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(
            @QueryParam("project_id") Integer project_id) {

        //File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(project_id, uploadPath(), true);

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
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response createExcel(
            @FormParam("fields") List<String> fields,
            @FormParam("project_id") Integer project_id,
            @FormParam("accession_number") Integer accessionNumber,
            @FormParam("dataset_code") String datasetCode,
            @FormParam("operation") String operation,
            @Context HttpServletRequest request) {

        // Create the configuration file
        //File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
        if (accessionNumber != null || datasetCode != null) {
            if (accessionNumber == null || datasetCode == null) {
                return Response.status(400).entity("{\"error\": \"" +
                        " Both an Accession Number and a Dataset Code are required if this is a NMNH project.").build();
                // only need to check that datasetCode is valid since an exception would have been thrown if accessionNumber
                // wasn't an Integer
            } else if (!datasetCode.matches("^\\w{4,50}$")) {
                return Response.status(400).entity("{\"error\": \"The Dataset Code must be an alphanumeric between" +
                        " 4 and 50 characters.").build();
            }

        }

        if (operation == null || !operation.equalsIgnoreCase("update")) {
            operation = "insert";
        }

        // Check if the project is an NMNH project
        processController processController = new processController(project_id, datasetCode);
        processController.setAccessionNumber(accessionNumber);


        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");
        bcidConnector bcidConnector = new bcidConnector(accessToken, refreshToken);
        process p = new process(
                null,
                uploadPath(),
                bcidConnector,
                processController);

        // Handle creating an expedition on template generation
        if (p.isNMNHProject()) {
            // Return if we don't have the necessary information
            if (accessionNumber == null || datasetCode == null) {
                return Response.status(400).entity("{\"error\": " +
                        "\"This is a NMNH project. Accession number and Dataset Code are required.}").build();
            } else {

                // Only create expedition if necessary
                if (operation.equalsIgnoreCase("insert")) {
                    p.runExpeditionCreate();
                }
            }
        }
        templateProcessor t;
        // Create the template processor which handles all functions related to the template, reading, generation
        if (accessionNumber != null) {
            // Get the ARK associated with this dataset code
            // TODO: Resource may change in future... better to figure this out programatically at some point
            String ark;
            try {
              ark = bcidConnector.getArkFromDataset(project_id, URLEncoder.encode(datasetCode,"utf-8"),"Resource");
            } catch (UnsupportedEncodingException e) {
                throw new FIMSRuntimeException(500, e);
            }

            String username = session.getAttribute("user").toString();

            // Construct the new templateProcessor
            t = new templateProcessor(
                    project_id,
                    uploadPath(),
                    true,
                    accessionNumber,
                    datasetCode,
                    ark,
                    username);
        } else {
            t = new templateProcessor(project_id, uploadPath(), true);
        }

        // Set the default sheet-name
        String defaultSheetname = t.getMapping().getDefaultSheetName();

        File file = null;
        file = t.createExcelFile(defaultSheetname, uploadPath(), fields);

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
     */
    @GET
    @Path("/definition/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefinitions(
            @QueryParam("project_id") Integer project_id,
            @QueryParam("column_name") String column_name) {

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

