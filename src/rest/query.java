package rest;

import digester.Attribute;
import digester.Mapping;
import fims.fimsFilterCondition;
import fims.fimsQueryBuilder;
import org.apache.commons.digester.Digester;
import run.configurationFileFetcher;
import run.process;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

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

        File file = GETQueryResult(graphs, project_id, filter, "json");

        String response = readFile(file.getAbsolutePath());

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
        }
    }

    /**
     * Return JSON for a graph query as POST
     * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     * @throws Exception
     */
    @POST
    @Path("/json/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJsonAsPOST(
            MultivaluedMap<String, String> form) throws Exception {

        // Build the query, etc..
        fimsQueryBuilder q = POSTQueryResult(form);

        // Run the query, passing in a format and returning the location of the output file
        File file = new File(q.run("json"));

        // Wrie the file to a String and return it
        String response = readFile(file.getAbsolutePath());

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return Response.ok(response).build();
        }
    }

    /**
     * Return KML for a graph query using POST
     * filter is just a single value to filter the entire dataset
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
            // Construct a file
            File file = GETQueryResult(graphs, project_id, filter, "kml");

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
     * Return KML for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     * @throws Exception
     */
    @POST
    @Path("/kml/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKml(
            MultivaluedMap<String, String> form) throws Exception {

        try {
            // Build the query, etc..
            fimsQueryBuilder q = POSTQueryResult(form);

            // Run the query, passing in a format and returning the location of the output file
            File file = new File(q.run("kml"));

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
     * Return Excel for a graph query.  The GET query runs a simple FILTER query for any term
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
            @QueryParam("filter") String filter
    ) throws Exception {

        try {
            File file = GETQueryResult(graphs, project_id, filter, "excel");

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
     * Return Excel for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     * @throws Exception
     */
    @POST
    @Path("/excel/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/vnd.ms-excel")
    public Response queryExcel(
            MultivaluedMap<String, String> form) throws Exception {

        try {
            // Build the query, etc..
            fimsQueryBuilder q = POSTQueryResult(form);

            // Run the query, passing in a format and returning the location of the output file
            File file = new File(q.run("excel"));

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
     * Get the POST query result as a file
     *
     * @return
     * @throws Exception
     */
    private fimsQueryBuilder POSTQueryResult(MultivaluedMap<String, String> form) throws Exception {
        Iterator entries = form.entrySet().iterator();
        String[] graphs = null;
        Integer project_id = null;

        HashMap<String, String> filterMap = new HashMap<String, String>();
        ArrayList<fimsFilterCondition> filterConditionArrayList = new ArrayList<fimsFilterCondition>();

        while (entries.hasNext()) {
            Map.Entry thisEntry = (Map.Entry) entries.next();
            String key = (String) thisEntry.getKey();
            // Values come over as a linked list
            LinkedList value = (LinkedList) thisEntry.getValue();
            if (key.equalsIgnoreCase("graphs")) {
                Object[] valueArray = value.toArray();
                graphs = Arrays.copyOf(valueArray, valueArray.length, String[].class);
            } else if (key.equalsIgnoreCase("project_id")) {
                project_id = Integer.parseInt((String) value.get(0));
            } else if (key.equalsIgnoreCase("submit")) {
                // do nothing with this
            } else {
                String v = (String) value.get(0);// only expect 1 value here
                filterMap.put(key, v);
            }
        }

        // Make sure graphs and project_id are set
        if (graphs != null && graphs.length < 1 && project_id != null) {
            throw new Exception("ERROR: incomplete arguments");
        }

        // Create a process object here so we can look at uri/column values
        process process = getProcess(project_id);

        // Build the Query
        fimsQueryBuilder q = new fimsQueryBuilder(process, graphs, uploadPath());

        // Loop the filterMap entries and build the filterConditionArrayList
        Iterator it = filterMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            fimsFilterCondition f = parsePOSTFilter((String) pairs.getKey(), (String) pairs.getValue(), process);
            if (f != null)
                filterConditionArrayList.add(f);
            it.remove();
        }

        // Add our filter conditions
        if (filterConditionArrayList != null && filterConditionArrayList.size() > 0)
            q.addFilter(filterConditionArrayList);

        return q;
    }

    /**
     * Get the query result as a file
     *
     * @param graphs
     * @param project_id
     * @param filter
     * @param format
     * @return
     * @throws Exception
     */
    private File GETQueryResult(String graphs, Integer project_id, String filter, String format) throws Exception {
        java.net.URLDecoder decoder = new java.net.URLDecoder();

        graphs = URLDecoder.decode(graphs, "UTF-8");

        process p = getProcess(project_id);

        // Parse the GET filter
        fimsFilterCondition filterCondition = parseGETFilter(filter, p);

        if (filterCondition != null) {
            // Create a filter statement
            ArrayList<fimsFilterCondition> arrayList = new ArrayList<fimsFilterCondition>();
            arrayList.add(filterCondition);

            // Run the query
            return new File(p.query(graphs, format, arrayList));
        } else {
            return new File(p.query(graphs, format, null));
        }
    }

    private process getProcess(Integer project_id) throws Exception {
        File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

        // Create a process object
        process p = new process(
                uploadPath(),
                configFile
        );
        return p;
    }


    /**
     * Get real path of the uploads folder from context.
     * Needs context to have been injected before.
     *
     * @return Real path of the uploads folder with ending slash.
     */
    private static String uploadPath() {
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

    /**
     * Parse the GET filter string smartly.  Maps what looks like a column to a URI using the configuration file
     * and if it looks like a URI then creates a URI straight from the key.
     *
     * @param key
     * @param value
     * @param process
     * @return
     * @throws Exception
     */
    public static fimsFilterCondition parsePOSTFilter(String key, String value, process process) throws Exception {
        java.net.URI uri = null;

        if (key == null || key.equals("") || value == null || value.equals(""))
            return null;

        // this is a predicate/URI query
        if (key.contains(":")) {
            uri = new URI(key);
        } else {
            Mapping mapping = new Mapping();
            process.addMappingRules(new Digester(), mapping);
            ArrayList<Attribute> attributeArrayList = mapping.getAllAttributes(mapping.getDefaultSheetName());
            uri = mapping.lookupColumn(key, attributeArrayList);
        }
        return new fimsFilterCondition(uri, value, fimsFilterCondition.AND);

    }

    /**
     * Parse the GET filter string smartly.  This looks for either column Names or URI Properties, and
     * if it finds a column name maps to a URI Property.  Values are assumed to be last element past a semicolon ALWAYS.
     *
     * @param filter
     * @return
     * @throws Exception
     */
    public static fimsFilterCondition parseGETFilter(String filter, process process) throws Exception {
        Mapping mapping = new Mapping();
        process.addMappingRules(new Digester(), mapping);

        String delimiter = ":";
        java.net.URI uri = null;
        java.net.URLDecoder decoder = new java.net.URLDecoder();

        if (filter == null)
            return null;

        String[] filterSplit = filter.split(":");

        // Get the value we're looking for
        Integer lastValue = filterSplit.length - 1;
        String value = decoder.decode(filterSplit[lastValue], "UTF8").toString();

        // Build the predicate.
        if (filterSplit.length != lastValue) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lastValue; i++) {
                if (i > 0) {
                    sb.append(delimiter);
                }
                sb.append(decoder.decode(filterSplit[i], "UTF8").toString());
            }

            // re-assemble the string
            String key = sb.toString();

            // If the key contains a semicolon, then assume it is a URI
            if (key.contains(":")) {
                uri = new java.net.URI(key);
            }
            // If there is no semicolon here then assume the user passed in a column name
            else {
                ArrayList<Attribute> attributeArrayList = mapping.getAllAttributes(mapping.getDefaultSheetName());
                uri = mapping.lookupColumn(key, attributeArrayList);
            }
        }

        return new fimsFilterCondition(uri, value, fimsFilterCondition.AND);
    }
}

