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

        process p = null;
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
        String instructionSheetName = "Instructions";

        // Create the workbook
        HSSFWorkbook workbook = new HSSFWorkbook();

        // Create the Instructions Sheet, which is always first
        HSSFSheet instructionsSheet = workbook.createSheet(instructionSheetName);

        // Loop through all fields in schema and provide names, uris, and definitions
        Iterator entitiesIt = t.getMapping().getEntities().iterator();
        int rowNum = 0;
        Row row = instructionsSheet.createRow(rowNum++);
        row.createCell(0).setCellValue("ColumnName");
        row.createCell(1).setCellValue("Entity");
        row.createCell(2).setCellValue("URI");
        row.createCell(3).setCellValue("Definition");

        // Must loop entities first
        while (entitiesIt.hasNext()) {
            digester.Entity e = (digester.Entity) entitiesIt.next();
            // Loop attributes
            Iterator attributesIt = ((LinkedList<Attribute>) e.getAttributes()).iterator();

            // Then loop attributes
            while (attributesIt.hasNext()) {
                Attribute a = (Attribute) attributesIt.next();
                row = instructionsSheet.createRow(rowNum++);
                int column = 0;
                row.createCell(column++).setCellValue(a.getColumn());
                row.createCell(column++).setCellValue(e.getConceptAlias());
                row.createCell(column++).setCellValue(a.getUri());
                row.createCell(column++).setCellValue(a.getDefinition());
            }
        }
        // Create the Default Sheet sheet
        HSSFSheet defaultSheet = workbook.createSheet(defaultSheetname);

        //Create the header row
        row = defaultSheet.createRow(0);

        // Loop the fields that the user wants in the default sheet
        rowNum = 0;
        Iterator itFields = fields.iterator();
        while (itFields.hasNext()) {
            String field = (String) itFields.next();
            Cell cell = row.createCell(rowNum++);
            //Set value to new value
            cell.setCellValue(field);
        }

        // Write the excel file
        // Set the outputname
        String filename = "output";
        if (t.getFims().getMetadata().getShortname() != null && !t.getFims().getMetadata().getShortname().equals(""))
            filename = t.getFims().getMetadata().getShortname();
        String outputName = filename + ".xls";

        // Create the file
        File file = null;
        try {
            file = PathManager.createUniqueFile(outputName, uploadPath());
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Response.status(204).build();

        } catch (IOException e) {
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

