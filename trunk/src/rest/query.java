package rest;

import digester.Fims;
import digester.Mapping;
import fims.fimsModel;
import fims.fimsQueryBuilder;
import org.apache.commons.digester.Digester;
import org.openjena.riot.ContentType;
import run.configurationFileFetcher;
import run.process;
import settings.FIMSException;
import settings.PathManager;
import settings.fimsPrinter;

import javax.servlet.ServletContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Query interface for Biocode-fims project
 */
@Path("")
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
            @QueryParam("configuration") String configuration,
            @QueryParam("filter") String filter) throws Exception {

        process p = null;
        File configFile = new configurationFileFetcher(new URL(URLDecoder.decode(configuration, "UTF-8")), uploadPath()).getOutputFile();

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
        String response = readFile(p.query(URLDecoder.decode(graphs, "UTF-8"), "json", filter));

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
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
            @QueryParam("configuration") String configuration,
            @QueryParam("filter") String filter) throws Exception {

        try {

            configuration = URLDecoder.decode(configuration, "UTF-8");
            graphs = URLDecoder.decode(graphs, "UTF-8");
            File configFile = new configurationFileFetcher(new URL(configuration), uploadPath()).getOutputFile();

            // Create a process object
            process p = new process(
                    uploadPath(),
                    configFile
            );

            // Construct a file
            File file = new File(p.query(graphs, "excel", filter));

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
     * Get real path of the uploads folder from context.
     * Needs context to have been injected before.
     *
     * @return Real path of the uploads folder with ending slash.
     */
    static String uploadPath() {
        return context.getRealPath("tripleOutput") + File.separator;
    }

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

