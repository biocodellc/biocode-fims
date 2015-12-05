package services.rest;

import bcid.projectMinter;
import digester.Attribute;
import digester.Mapping;
import fims.fimsFilterCondition;
import fims.fimsQueryBuilder;
import org.apache.commons.digester3.Digester;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.configurationFileFetcher;
import run.process;
import settings.FIMSRuntimeException;
import settings.bcidConnector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;

/**
 * Query interface for Biocode-fims expedition
 */
@Path("query")
public class query {
    private static Logger logger = LoggerFactory.getLogger(query.class);

    @Context
    static HttpServletRequest request;
    @Context
    static ServletContext context;

    /**
     * Return JSON for a graph query.
     *
     * @param graphs indicate a comma-separated list of graphs, or all
     * @return
     */
    @GET
    @Path("/json/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJson(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) {

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
     */
    @POST
    @Path("/json/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJsonAsPOST(
            MultivaluedMap<String, String> form) {

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
     * @param graphs indicate a comma-separated list of graphs, or all
     * @return
     */
    @GET
    @Path("/kml/")
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKml(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) {

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
    }

    /**
     * Return KML for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * @return
     */
    @POST
    @Path("/kml/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/vnd.google-earth.kml+xml")
    public Response queryKml(
            MultivaluedMap<String, String> form) {

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
    }

      /**
     * Return KML for a graph query using POST
     * filter is just a single value to filter the entire dataset
     *
     * @param graphs indicate a comma-separated list of graphs, or all
     * @return
     */
    @GET
    @Path("/cspace/")
    @Produces(MediaType.APPLICATION_XML)
    public Response queryCspace(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) {

        // Construct a file
        File file = GETQueryResult(graphs, project_id, filter, "cspace");

        // Return file to client
        Response.ResponseBuilder response = Response.ok((Object) file);

       // response.header("Content-Disposition",
         //       "attachment; filename=biocode-fims-output.xml");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
        }

    }

    /**
     * Return Excel for a graph query.  The GET query runs a simple FILTER query for any term
     *
     * @param graphs indicate a comma-separated list of graphs, or all
     * @return
     */
    @GET
    @Path("/excel/")
    @Produces("application/vnd.ms-excel")
    public Response queryExcel(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) {

        File file = GETQueryResult(graphs, project_id, filter, "excel");

        // Return file to client
        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition",
                "attachment; filename=biocode-fims-output.xlsx");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
        }
    }

    /**
     * Return Tab delimited data for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     *
     * @return
     */
    @POST
    @Path("/tab/")
    @Consumes("application/x-www-form-urlencoded")
    public Response queryTab(
            MultivaluedMap<String, String> form) {

        // Build the query, etc..
        fimsQueryBuilder q = POSTQueryResult(form);

        // Run the query, passing in a format and returning the location of the output file
        File file = new File(q.run("tab"));

        // Return file to client
        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition",
                "attachment; filename=biocode-fims-output.txt");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
        }
    }
    /**
     * Return Tab delimited data for a graph query.  The GET query runs a simple FILTER query for any term
     *
     * @param graphs indicate a comma-separated list of graphs, or all
     * @return
     */
    @GET
    @Path("/tab/")
    public Response queryTab(
            @QueryParam("graphs") String graphs,
            @QueryParam("project_id") Integer project_id,
            @QueryParam("filter") String filter) {

        File file = GETQueryResult(graphs, project_id, filter, "tab");

        // Return file to client
        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition",
                "attachment; filename=biocode-fims-output.txt");

        // Return response
        if (response == null) {
            return Response.status(204).build();
        } else {
            return response.build();
        }
    }

    /**
     * Return Excel for a graph query using POST
     * * <p/>
     * filter parameters are of the form:
     * name={URI} value={filter value}
     *
     * 
     * @return
     */
    @POST
    @Path("/excel/")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("application/vnd.ms-excel")
    public Response queryExcel(
            MultivaluedMap<String, String> form) {

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
    }

    /**
     * Get the POST query result as a file
     *
     * @return
     */
    private fimsQueryBuilder POSTQueryResult(MultivaluedMap<String, String> form) {
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
                System.out.println("project_id_val=" + (String)value.get(0) );
                System.out.println("project_id_int=" + project_id );
            } else if (key.equalsIgnoreCase("boolean")) {
                /// AND|OR
                //project_id = Integer.parseInt((String) value.get(0));
            } else if (key.equalsIgnoreCase("submit")) {
                // do nothing with this
            } else {
                String v = (String) value.get(0);// only expect 1 value here
                filterMap.put(key, v);
            }
        }

        // Make sure graphs and project_id are set
        if (graphs != null && graphs.length < 1 && project_id != null) {
            throw new FIMSRuntimeException("ERROR: incomplete arguments", 400);
        }

        if (graphs[0].equalsIgnoreCase("all")) {
            graphs = getAllGraphs(project_id);
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
     */
    private File GETQueryResult(String graphs, Integer project_id, String filter, String format) {
        java.net.URLDecoder decoder = new java.net.URLDecoder();
        String[] graphsArray;

        try {
            graphs = URLDecoder.decode(graphs, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException", e);
        }

        if (graphs.equalsIgnoreCase("all")) {
            graphsArray = getAllGraphs(project_id);
        } else {
            graphsArray = graphs.split(",");
        }

        process p = getProcess(project_id);

        // Parse the GET filter
        fimsFilterCondition filterCondition = parseGETFilter(filter, p);

        if (filterCondition != null) {
            // Create a filter statement
            ArrayList<fimsFilterCondition> arrayList = new ArrayList<fimsFilterCondition>();
            arrayList.add(filterCondition);

            // Run the query
            return new File(p.query(graphsArray, format, arrayList));
        } else {
            return new File(p.query(graphsArray, format, null));
        }
    }

    private String[] getAllGraphs(Integer project_id) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");
        List<String> graphs = new ArrayList<String>();

        projectMinter project= new projectMinter();

        JSONObject response = ((JSONObject) JSONValue.parse(project.getLatestGraphs(project_id, username)));
        project.close();
        JSONArray jArray = ((JSONArray) response.get("data"));
        Iterator it = jArray.iterator();

        while (it.hasNext()) {
            JSONObject obj = (JSONObject) it.next();
            graphs.add((String) obj.get("graph"));
        }

        return graphs.toArray(new String[graphs.size()]);
    }

    private process getProcess(Integer project_id) {
        File configFile = new configurationFileFetcher(project_id, uploadPath(), true).getOutputFile();

        // Create a process object
        process p = new process(
                project_id,
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
     */
    private String readFile(String file) {
        FileReader fr;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            throw new FIMSRuntimeException(500, e);
        }
        BufferedReader reader = new BufferedReader(fr);
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        try {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
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
     */
    public static fimsFilterCondition parsePOSTFilter(String key, String value, process process) {
        java.net.URI uri = null;

        if (key == null || key.equals("") || value == null || value.equals(""))
            return null;

        // this is a predicate/URI query
        if (key.contains(":")) {
            try {
                uri = new URI(key);
            } catch (URISyntaxException e) {
                throw new FIMSRuntimeException(500, e);
            }
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
     */
    public static fimsFilterCondition parseGETFilter(String filter, process process) {
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
        try {
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
        } catch (UnsupportedEncodingException e) {
            throw new FIMSRuntimeException(500, e);
        } catch (URISyntaxException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }
}

