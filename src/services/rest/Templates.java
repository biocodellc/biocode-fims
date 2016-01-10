package services.rest;

import bcid.Resolver;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import run.Process;
import run.ProcessController;
import run.TemplateProcessor;
import services.BiocodeFimsService;

import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.List;


/**
 * Query interface for Biocode-fims expedition
 */
@Path("templates")
public class Templates extends BiocodeFimsService {
    /**
     * Return the available attributes for a particular graph
     *
     * @return
     */
    @GET
    @Path("/attributes/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTemplateAttributes(@PathParam("projectId") Integer projectId) {

        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), true);

        // Write the all of the checkbox definitions to a String Variable
        JSONObject attributes = t.getAttributesByGroup();

        return Response.ok(attributes.toJSONString()).build();
    }

    /**
     * Return the abstract for a particular graph
     *
     * @return
     */
    @GET
    @Path("/abstract/{projectId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAbstract(
            @PathParam("projectId") Integer projectId) {

        JSONObject obj = new JSONObject();
        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), true);

        // Write the all of the checkbox definitions to a String Variable
        obj.put("abstract", JSONValue.escape(t.printAbstract()));

        return Response.ok(obj.toJSONString()).build();
    }

    @POST
    @Path("/createExcel/")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response createExcel(
            @FormParam("fields") List<String> fields,
            @FormParam("projectId") Integer projectId,
            @FormParam("accession_number") Integer accessionNumber,
            @FormParam("dataset_code") String expeditionCode,
            @FormParam("operation") String operation) {

        // Create the configuration file
        //File configFile = new ConfigurationFileFetcher(projectId, uploadPath(), true).getOutputFile();
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
        ProcessController processController = new ProcessController(projectId, expeditionCode);
        processController.setAccessionNumber(accessionNumber);


        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");
        Process p = new Process(
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
        TemplateProcessor t;
        // Create the template processor which handles all functions related to the template, reading, generation
        if (accessionNumber != null) {
            // Get the ARK associated with this expedition code
            // TODO: Resource may change in future... better to figure this out programatically at some point
            Resolver r = new Resolver(expeditionCode, projectId, "Resource");
            String identifier = r.getIdentifier();
            r.close();

            // Construct the new TemplateProcessor
            t = new TemplateProcessor(
                    projectId,
                    uploadPath(),
                    true,
                    accessionNumber,
                    expeditionCode,
                    identifier,
                    username);
        } else {
            t = new TemplateProcessor(projectId, uploadPath(), true);
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
    @Path("/definition/{projectId}/{columnName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDefinitions(
            @PathParam("projectId") Integer projectId,
            @PathParam("columnName") String columnName) {

        TemplateProcessor t = new TemplateProcessor(projectId, uploadPath(), true);
        JSONObject response = t.getDefinition(columnName);

        return Response.ok(response.toJSONString()).build();
    }
}

