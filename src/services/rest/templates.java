package services.rest;

import bcid.resolver;
import fimsExceptions.UnauthorizedRequestException;
import run.process;
import run.processController;
import run.templateProcessor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.List;

import javax.ws.rs.core.Context;


/**
 * Query interface for Biocode-fims expedition
 */
@Path("templates")
public class templates {
    @Context
    static ServletContext context;
    @Context
    static HttpServletResponse response;
    @Context
    static HttpServletRequest request;

    /**
     * Return the available attributes for a particular graph
     *
     * @return
     */
    @GET
    @Path("/attributes/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateCheckboxes(
            @QueryParam("projectId") Integer projectId,
            @Context HttpServletRequest request) {

        HttpSession session = request.getSession();


        //File configFile = new configurationFileFetcher(projectId, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(projectId, uploadPath(), true);

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
     * get a specific template generator configuration
     * @param configName
     * @return
     */
    @GET
    @Path("/getConfig/{projectId}/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getConfig(@PathParam("configName") String configName,
                          @PathParam("projectId") Integer projectId)
        throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/projectService/getTemplateConfig/" +
                projectId + "/" + configName);
        dispatcher.forward(request, response);
        return;
    }

    /**
     * Get the template generator configurations for a given project
     * @param projectId
     * @return
     */
    @GET
    @Path("/getConfigs/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getConfigs(@PathParam("projectId") Integer projectId)
        throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/projectService/getTemplateConfigs/" + projectId);
        dispatcher.forward(request, response);
        return;
    }

    /**
     * Remove the template generator configuration
     * @param projectId
     * @return
     */
    @GET
    @Path("/removeConfig/{projectId}/{configName}")
    @Produces(MediaType.APPLICATION_JSON)
    public void removeConfig(@PathParam("projectId") Integer projectId,
                             @PathParam("configName") String configName)
        throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/projectService/removeTemplateConfig/" +
                projectId + "/" + configName);
        dispatcher.forward(request, response);
        return;
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
            @QueryParam("projectId") Integer projectId) {

        //File configFile = new configurationFileFetcher(projectId, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(projectId, uploadPath(), true);

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
            @FormParam("projectId") Integer projectId,
            @FormParam("accession_number") Integer accessionNumber,
            @FormParam("dataset_code") String expeditionCode,
            @FormParam("operation") String operation,
            @Context HttpServletRequest request) {

        // Create the configuration file
        //File configFile = new configurationFileFetcher(projectId, uploadPath(), true).getOutputFile();
        if (accessionNumber != null || expeditionCode != null) {
            if (accessionNumber == null || expeditionCode == null) {
                return Response.status(400).entity("{\"error\": \"" +
                        " Both an Accession Number and a Dataset Code are required if this is a NMNH project.").build();
                // only need to check that expeditionCode is valid since an exception would have been thrown if accessionNumber
                // wasn't an Integer
            } else if (!expeditionCode.matches("^\\w{4,50}$")) {
                return Response.status(400).entity("{\"error\": \"The Dataset Code must be an alphanumeric between" +
                        " 4 and 50 characters.").build();
            }

        }

        if (operation == null || !operation.equalsIgnoreCase("update")) {
            operation = "insert";
        }

        // Check if the project is an NMNH project
        processController processController = new processController(projectId, expeditionCode);
        processController.setAccessionNumber(accessionNumber);


        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");
        process p = new process(
                null,
                uploadPath(),
                processController);

        // Handle creating an expedition on template generation
        if (p.isNMNHProject()) {
            // Return if we don't have the necessary information
            if (accessionNumber == null || expeditionCode == null) {
                return Response.status(400).entity("{\"error\": " +
                        "\"This is a NMNH project. Accession number and Dataset Code are required.}").build();
            } else {

                // Only create expedition if necessary
                if (operation.equalsIgnoreCase("insert")) {
                    if (username == null) {
                        throw new UnauthorizedRequestException("You must be logged in to create a new expedition.");
                    }
                    processController.setUserId(username);
                    p.runExpeditionCreate();
                }
            }
        }
        templateProcessor t;
        // Create the template processor which handles all functions related to the template, reading, generation
        if (accessionNumber != null) {
            // Get the ARK associated with this expedition code
            // TODO: Resource may change in future... better to figure this out programatically at some point
            resolver r = new resolver(expeditionCode, projectId, "Resource");
            String ark = r.getArk();
            r.close();

            // Construct the new templateProcessor
            t = new templateProcessor(
                    projectId,
                    uploadPath(),
                    true,
                    accessionNumber,
                    expeditionCode,
                    ark,
                    username);
        } else {
            t = new templateProcessor(projectId, uploadPath(), true);
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
            @QueryParam("projectId") Integer projectId,
            @QueryParam("column_name") String column_name) {

        //File configFile = new configurationFileFetcher(projectId, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(projectId, uploadPath(), true);

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

