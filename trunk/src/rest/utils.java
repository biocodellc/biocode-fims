package rest;

import digester.Validation;
import org.apache.commons.digester3.Digester;
import run.configurationFileFetcher;
import run.process;
import settings.bcidConnector;
import utils.SettingsManager;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;


/**
 * Biocode-FIMS utility services
 */
@Path("utils/")
public class utils {
    @Context
    static ServletContext context;

    /**
     * Refresh the configuration File cache
     *
     * @return
     * @throws Exception
     */
    @GET
    @Path("/refreshCache/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response queryJson(
            @QueryParam("project_id") Integer project_id
    ) throws Exception {

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
     * @return
     * @throws Exception
     */
    @GET
    @Path("/expeditionCodes/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExpeditionCodes(@PathParam("project_id") Integer projectId,
                                       @Context HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");
        bcidConnector bcidConnector = new bcidConnector(accessToken, refreshToken);

        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();
        String expedition_list_uri = sm.retrieveValue("expedition_list_uri");

//        URL url = new URL("http://biscicol.org:8080/id/expeditionService/list/" + projectId + "?access_token=" + accessToken);
        URL url = new URL(expedition_list_uri + projectId + "?access_token=" + accessToken);

        String response = bcidConnector.createGETConnection(url);

        return Response.status(bcidConnector.getResponseCode()).entity(response).build();
    }

    /**
     * Retrieve a user's expeditions in a given project from bcid. This uses an access token to access the
     * bcid service.
     *
     * @param projectId
     * @return
     * @throws Exception
     */
    @GET
    @Path("/getListFields/{list_name}/")
    @Produces(MediaType.TEXT_HTML)
    public Response getListFields(@QueryParam("project_id") Integer projectId,
                                  @PathParam("list_name") String list_name) throws Exception {

        File configFile = new configurationFileFetcher(projectId, uploadPath(), true).getOutputFile();

        // Create a process object
        process p = new process(
                uploadPath(),
                configFile
        );

        Validation validation = new Validation();
        p.addValidationRules(new Digester(), validation);
        digester.List results = (digester.List) validation.findList(list_name);
        Iterator it = results.getFields().iterator();
        StringBuilder sb = new StringBuilder();
        sb.append("Acceptable values for " + list_name + "<br>\n");
        while (it.hasNext()) {
            sb.append("<li>" + (String) it.next() + "</li>\n");
        }

        return Response.ok(sb.toString()).build();
    }
}

