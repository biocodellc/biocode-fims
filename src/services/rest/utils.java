package services.rest;

import bcidExceptions.UnauthorizedRequestException;
import digester.Attribute;
import digester.Field;
import digester.Mapping;
import digester.Validation;
import org.apache.commons.digester3.Digester;
import org.json.simple.JSONObject;
import run.configurationFileFetcher;
import run.process;
import run.processController;
import settings.FIMSRuntimeException;
import settings.bcidConnector;
import utils.SettingsManager;
import utils.dashboardGenerator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Biocode-FIMS utility services
 */
@Path("utils/")
public class utils {
    @Context
    static ServletContext context;
    @Context
    static HttpServletResponse response;
    @Context
    static HttpServletRequest request;

    /**
     * Refresh the configuration File cache
     *
     * @return
     */
    @GET
    @Path("/refreshCache/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJson(
            @QueryParam("project_id") Integer project_id) {

        new configurationFileFetcher(project_id, uploadPath(), false).getOutputFile();

        return Response.ok("").build();

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
     * Retrieve a user's expeditions in a given project from bcid. This uses an access token to access the
     * bcid service.
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/expeditionCodes/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getExpeditionCodes(@PathParam("project_id") Integer projectId)
                               throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/expeditionService/list/" + projectId);
        dispatcher.forward(request, response);
        return;
    }

    /**
     * Retrieve a user's graphs in a given project from bcid. This uses an access token to access the
     * bcid service.
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/graphs/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void getGraphs(@PathParam("project_id") Integer projectId)
                      throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/projectService/graphs/" + projectId);
        dispatcher.forward(request, response);
        return;
    }

    /**
     * Check whether or not an expedition code is valid by calling the BCID expeditionService/validateExpedition
     * Service
     * Should return update, insert, or error
     *
     * @param projectId
     * @param expeditionCode
     *
     * @return
     */
    @GET
    @Path("/validateExpedition/{project_id}/{expedition_code}")
    @Produces(MediaType.APPLICATION_JSON)
    public void validateExpedition(@PathParam("project_id") Integer projectId,
                                   @PathParam("expedition_code") String expeditionCode)
        throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/expeditionService/list/" +
                projectId + "/" + expeditionCode);
        dispatcher.forward(request, response);
        return;
    }



    /**
     * Retrieve a user's expeditions in a given project from bcid. This uses an access token to access the
     * bcid service.
     *
     * @param projectId
     *
     * @return
     */
    @GET
    @Path("/getListFields/{list_name}/")
    @Produces(MediaType.TEXT_HTML)
    public Response getListFields(@QueryParam("project_id") Integer projectId,
                                  @PathParam("list_name") String list_name,
                                  @QueryParam("column_name") String column_name) {

        File configFile = new configurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        // Create a process object
        process p = new process(
                projectId,
                uploadPath(),
                configFile
        );

        Validation validation = new Validation();
        p.addValidationRules(new Digester(), validation);
        digester.List results = (digester.List) validation.findList(list_name);
        // NO results mean no list has been defined!
        if (results == null) {
            return Response.ok("No list has been defined for \"" + column_name + "\" but there is a rule saying it exists.  " +
                    "Please talk to your FIMS data manager to fix this").build();
        }
        Iterator it = results.getFields().iterator();
        StringBuilder sb = new StringBuilder();

        if (column_name != null && !column_name.trim().equals("")) {
            try {
                sb.append("<b>Acceptable values for " + URLDecoder.decode(column_name, "utf-8") + "</b><br>\n");
            } catch (UnsupportedEncodingException e) {
                throw new FIMSRuntimeException(500, e);
            }
        } else {
            sb.append("<b>Acceptable values for " + list_name + "</b><br>\n");
        }

        // Get field values
        while (it.hasNext()) {
            Field f = (Field)it.next();
            sb.append("<li>" + f.getValue() + "</li>\n");
        }

        return Response.ok(sb.toString()).build();
    }

    @GET
    @Path("/isNMNHProject/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isNMNHProject(@PathParam("project_id") Integer projectId) {
        processController processController = new processController(projectId, null);
        process p = new process(
                null,
                uploadPath(),
                null,
                processController);

        return Response.ok("{\"isNMNHProject\": \"" + p.isNMNHProject() + "\"}").build();
    }

    @GET
    @Path("/listProjects")
    @Produces(MediaType.APPLICATION_JSON)
    public void listProjects()
        throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/projectService/list/");
        dispatcher.forward(request, response);
        return;
    }

    @GET
    @Path("/callBCID")
    public Response callBcid(@QueryParam("url") String url,
                             @QueryParam("method") String method,
                             @QueryParam("postParams") String postParams) throws Exception{
        bcidConnector c = new bcidConnector();

        if ( method.equalsIgnoreCase("get") ) {
            return Response.ok(c.createGETConnection(new URL(url))).build();
        } else {
            return Response.ok(c.createPOSTConnnection(new URL(url), postParams)).build();
        }
    }

    @GET
    @Path("/getNAAN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNAAN() {
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        String naan = sm.retrieveValue("naan");

        return Response.ok("{\"naan\": \"" + naan + "\"}").build();
    }

    @GET
    @Path("/getDatasetDashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDatasetDashboard(@QueryParam("isNMNH") @DefaultValue("false") Boolean isNMNH) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("user");
        String dashboard;

        if (username == null) {
            throw new UnauthorizedRequestException("You must be logged in to view your dashboard.");
        }

        dashboardGenerator dashboardGenerator = new dashboardGenerator(username);
        if (isNMNH) {
            dashboard = dashboardGenerator.getNMNHDashboard();
        } else {
            dashboard = dashboardGenerator.getDashboard();
        }

        return Response.ok("{\"dashboard\": \"" + dashboard + "\"}").build();
    }

    @POST
    @Path("/updatePublicStatus")
    @Produces(MediaType.APPLICATION_JSON)
    public void updatePublicStatus(@FormParam("expedition_code") String expedition_code,
                                       @FormParam("project_id") int project_id,
                                       @FormParam("public") Boolean p)
        throws IOException, ServletException {

        RequestDispatcher dispatcher = request.getRequestDispatcher("/id/expeditionService/publicExpedition/" +
                project_id + "/" + expedition_code + "/" + p);
        dispatcher.forward(request, response);
        return;
    }

    @GET
    @Path("/getLatLongColumns/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatLongColumns(@PathParam("project_id") int projectId) {
        String decimalLatDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLatitude";
        String decimalLongDefinedBy = "http://rs.tdwg.org/dwc/terms/decimalLongitude";
        JSONObject response = new JSONObject();

        try {
            processController pc = new processController(projectId, null);
            process p = new process(null, uploadPath(), new bcidConnector(), pc);

            Mapping mapping = p.getMapping();
            String defaultSheet = mapping.getDefaultSheetName();
            ArrayList<Attribute> attributeList = mapping.getAllAttributes(defaultSheet);

            response.put("data_sheet", defaultSheet);

            for (Attribute attribute : attributeList) {
                // when we find the column corresponding to the definedBy for lat and long, add them to the response
                if (attribute.getDefined_by().equalsIgnoreCase(decimalLatDefinedBy)) {
                    response.put("lat_column", attribute.getColumn());
                } else if (attribute.getDefined_by().equalsIgnoreCase(decimalLongDefinedBy)) {
                    response.put("long_column", attribute.getColumn());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSRuntimeException(500, e);
        }
        return Response.ok(response.toJSONString()).build();
    }

    @GET
    @Path("/getMapboxToken")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMapboxToken() {
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        String token = sm.retrieveValue("mapboxAccessToken");

        return Response.ok("{\"accessToken\": \"" + token + "\"}").build();
    }
}

