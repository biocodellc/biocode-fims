package rest;


import run.process;
import run.processController;
import settings.FIMSException;
import settings.bcidConnector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;

/**
 * REST interface for validating data
 */
@Path("validate")
public class validate {
    @Context
    static ServletContext context;

    @GET
    @Path("/validate")
    @Produces(MediaType.TEXT_HTML)
    public void validate(@QueryParam("project_id") Integer project_id,
                         @QueryParam("expedition_code") String expedition_code,
                         @Context HttpServletRequest req)
            throws IOException {


        HttpSession session = req.getSession();

        // Find an existing processController or create a new one if it doesn't exist
        processController processController = (processController) session.getAttribute("processController");
        if (processController == null) {
            processController = new processController(project_id, expedition_code);
        }

        // TODO: make some method to upload a file and refer to it here
        String input_file = "";
        // TODO: fetch the connector from a session
        bcidConnector connector = null;

        // Create the process object --- this is done each time to orient the application
        process p = null;
        try {
            p = new process(
                    input_file,
                    uploadPath(),
                    connector,
                    processController
            );
        } catch (FIMSException e) {
            e.printStackTrace();
        }

        // Print the status of the processController for testing
        System.out.println(processController.printStatus());

        // Run the process
        // TODO: See process.runAllLocally() and copy the interactive steps there...

        // Set session's processController to what the process class did with it
        session.setAttribute("processController", p.getProcessController());

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
