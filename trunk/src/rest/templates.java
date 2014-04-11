package rest;

import com.sun.org.apache.bcel.internal.classfile.AttributeReader;
import digester.Attribute;
import digester.Fims;
import digester.Mapping;
import org.apache.commons.digester.Digester;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import run.configurationFileFetcher;
import run.process;
import run.templateProcessor;
import settings.FIMSException;
import settings.PathManager;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

        File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
        templateProcessor t = new templateProcessor(uploadPath(),configFile);

        // Write the all of the checkbox definitions to a String Variable
        String response = t.printCheckboxes();

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
            @FormParam("project_id") Integer project_id) throws Exception {

        // Create the configuration file
        File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

        // Create the template processor which handles all functions related to the template, reading, generation
        templateProcessor t = new templateProcessor(uploadPath(),configFile);

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

        process p = null;
        File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();
         templateProcessor t = new templateProcessor(uploadPath(),configFile);

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

