package rest;

import fims.fimsFilterCondition;
import run.configurationFileFetcher;
import run.process;
import settings.FIMSException;

import javax.servlet.ServletContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;

/**
 * Query interface for Biocode-fims expedition
 */
@Path("query")
public class query {
    @Context
    static ServletContext context;

    /**
     * Return JSON for a graph query
     *
     * @param graphs indicate a comma-separated list of graphs
     * @return
     * @throws Exception
     */
    @GET
    @Path("/json/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJson(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) throws Exception {

        process p = null;
        File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

        try {
            p = new process(
                    uploadPath(),
                    configFile
            );
        } catch (FIMSException e) {
            e.printStackTrace();
            return Response.ok("\nError: " + e.getMessage()).build();
        }

        // Write the response to a String Variable
        String response = readFile(p.query(URLDecoder.decode(graphs, "UTF-8"), "json", constructFilters(filter)));

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
        }
    }

    /**
     * Return KML for a graph query
     *
     * @param graphs indicate a comma-separated list of graphs
     * @return
     * @throws Exception
     */
    @GET
    @Path("/kml/")
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKml(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) throws Exception {


        try {
            graphs = URLDecoder.decode(graphs, "UTF-8");
            File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

            process p = new process(
                    uploadPath(),
                    configFile
            );

            // Construct a file
            File file = new File(p.query(graphs, "kml", constructFilters(filter)));

            // Return file to client
            Response.ResponseBuilder response = Response.ok((Object) file);
            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.kml");

            // Return response
            if (response == null) {
                return Response.status(204).build();
            } else {
                return response.build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok("\nError: " + e.getMessage()).build();
        }
    }

    /**
     * Return Excel for a graph query
     *
     * @param graphs indicate a comma-separated list of graphs
     * @return
     * @throws Exception
     */
    @GET
    @Path("/excel/")
    @Produces("application/vnd.ms-excel")
    public Response queryExcel(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) throws Exception {

        try {

            graphs = URLDecoder.decode(graphs, "UTF-8");
            File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

            // Create a process object
            process p = new process(
                    uploadPath(),
                    configFile
            );

            // Construct a file
            File file = new File(p.query(graphs, "excel", constructFilters(filter)));

            // Return file to client
            Response.ResponseBuilder response = Response.ok((Object) file);
            response.header("Content-Disposition",
                    "attachment; filename=biocode-fims-output.xls");

            // Return response
            if (response == null) {
                return Response.status(204).build();
            } else {
                return response.build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok("\nError: " + e.getMessage()).build();
        }
    }

    /**
     * Convert a GET string representation of filter and make it into a fimsFilterCondition ArrayList.
     * The GET string should be one of the following forms:
     * <p/>
     * value
     * value|column
     * value|column|operation (1=AND, 2=OR, 3=NOT -- currently its all just AND)
     * <p/>
     * Multiple filter statements can be joined using commas, like:
     * <p/>
     * value|column, value|column,
     * <p/>
     * column MUST be the URI specification... this is the only truly persistent representation of this concept
     *
     * @param filters
     * @return
     */
    static ArrayList<fimsFilterCondition> constructFilters(String filters) throws URISyntaxException {
        String filterDelimeter = ",";
        String filterPartsDelimiter = "\\|";

        ArrayList<fimsFilterCondition> fimsFilterConditionArrayList = new ArrayList<fimsFilterCondition>();

        if (filters == null || filters.equals(""))
            return null;

        String[] filter = filters.split(filterDelimeter);
        for (int i = 0; i < filter.length; i++) {

            String[] conditions = filter[i].split(filterPartsDelimiter);

            URI uri = null;
            String value = null;
            Integer conditionInt = fimsFilterCondition.AND; // Default

            System.out.println("there are " + conditions.length + " conditions for " + filter[i]);

            for (int j = 0; j < conditions.length; j++) {
                if (j == 0)
                    value = conditions[j];
                if (j == 1)
                    uri = new URI(conditions[1]);
                if (j == 2)
                    conditionInt = Integer.parseInt(conditions[2]);
            }

            //System.out.println("\t" + uri.toString() + "|" + value + "|" + conditionInt);
            fimsFilterConditionArrayList.add(new fimsFilterCondition(uri, value, conditionInt));

        }

        return fimsFilterConditionArrayList;
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

    /**
     * Read a file and return it as a String... meant to be used within this class only
     *
     * @param file
     * @return
     * @throws IOException
     */
    private String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        return stringBuilder.toString();
    }
}

