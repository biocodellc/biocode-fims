package rest;

import com.sun.jersey.multipart.FormDataParam;
import com.sun.jndi.url.dns.dnsURLContext;
import run.process;
import run.processController;
import settings.FIMSException;
import settings.bcidConnector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
//import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.*;

/**
 * Created by rjewing on 4/18/14.
 */
@Path("validate")
public class validate {
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void validate(@FormDataParam("project_id") Integer project_id,
                         @FormDataParam("expedition_code") String expedition_code,
                         @FormDataParam("upload") String upload,
                         @FormDataParam("dataset") InputStream is,
                         @Context HttpServletRequest request) throws Exception{
        HttpSession session = request.getSession();

        // Find an existing processController or create a new one if it doesn't exist
        processController processController = (processController) session.getAttribute("processController");
        if (processController == null) {
            processController = new processController(project_id, expedition_code);
        }

        // TODO: delete this temp file after uploaded
        String input_file = saveTempFile(is);
        // TODO: fetch the connector from a session
        bcidConnector connector = null;

        // Create the process object --- this is done each time to orient the application
        process p = null;
        try {
            p = new process(
                    input_file,
                    "",//uploadpath(),
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

        // Set the session processController to what the process class did with it
        session.setAttribute("processController", processController);
    }

    private String saveTempFile(InputStream is) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File f = new File(tempDir, "filename");

        try {
            OutputStream os = new FileOutputStream(f);
            try {
                byte[] buffer = new byte[4096];
                for (int n; (n = is.read(buffer)) != -1; )
                    os.write(buffer, 0, n);
            }
            finally { os.close(); }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return f.getAbsolutePath();
    }

//    @Context
//    static ServletContext context;
//
//    static String uploadpath() {
//        return context.getRealPath("tripleOutput") + File.separator;
//    }

}
