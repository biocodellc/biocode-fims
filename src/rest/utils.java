package rest;

import run.configurationFileFetcher;
import settings.bcidConnector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.net.URL;


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

    @GET
    @Path("/expeditionCodes/{project_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getExpeditionCodes(@PathParam("project_id") Integer projectId,
                                       @Context HttpServletRequest request)  throws Exception {
        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");
        bcidConnector bcidConnector = new bcidConnector(accessToken, refreshToken);

        URL url = new URL("http://localhost:8080/id/expeditionService/list/" + projectId + "?access_token=" + accessToken);

        String response = bcidConnector.createGETConnection(url);

        return Response.status(bcidConnector.getResponseCode()).entity(response).build();
    }


}

