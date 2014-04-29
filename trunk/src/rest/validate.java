package rest;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import run.process;
import run.processController;
import settings.FIMSException;
import settings.bcidConnector;
import utils.stringGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.*;

/**
 * Created by rjewing on 4/18/14.
 */
@Path("validate")
public class validate {

    @Context
    static ServletContext context;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public String validate(@FormDataParam("project_id") Integer project_id,
                           @FormDataParam("expedition_code") String expedition_code,
                           @FormDataParam("upload") String upload,
                           @FormDataParam("dataset") InputStream is,
                           @FormDataParam("dataset") FormDataContentDisposition fileData,
                           @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");

        // create a new processController
        processController processController = new processController(project_id, expedition_code);

        // update the status
        processController.appendStatus("Initializing...\n");
        processController.appendStatus("\tinputFilename = " + fileData.getFileName() + "\n");

        // Save the uploaded file
        String splitArray[] = fileData.getFileName().split("\\.");
        String ext;
        if (splitArray.length == 0) {
            // if no extension is found, then guess
            ext = "xls";
        } else {
            ext = splitArray[splitArray.length - 1];
        }
        String input_file = saveTempFile(is, ext);
        // if input_file null, then there was an error saving the file
        if (input_file == null) {
            return "[{\"done\": \"server error saving file\"}]";
        }

        // TODO: fetch the connector from a session
        bcidConnector connector = new bcidConnector(accessToken);

        // Create the process object --- this is done each time to orient the application
        process p = null;
        try {
            p = new process(
                    input_file,
                    uploadpath(),
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

        StringBuilder retVal = new StringBuilder();
        Boolean deleteInputFile = true;
        try {
            processController.appendStatus("Validating...\n");
            p.runValidation();

            // if there were validation errors, we can't upload
            if (processController.getHasErrors()) {
                retVal.append("{\"done\": \"");
                retVal.append(processController.getErrorsSB().toString() + "\n");
                retVal.append(processController.getWarningsSB().toString() + "\n");
                retVal.append("\tErrors found on " + processController.getWorksheetName() + " worksheet.  Must fix to continue.");
                retVal.append("\"}");

            } else if (upload != null && upload.equals("on")) {
                 // if there were vaildation warnings and user would like to upload, we need to ask the user to continue
                 if (!processController.isValidated() && processController.getHasWarnings()) {
                    retVal.append("{\"continue\": {\"message\": \"");
                    retVal.append("Warnings found on " + processController.getWorksheetName() + " worksheet.\n");
                    retVal.append("\t" + processController.getWarningsSB().toString());
                    retVal.append("\"}}");

                // there were no validation warnings and the user would like to upload, so continue
                } else {
                     // TODO continue w/o user consent
                     retVal.append("{\"continue\": {}}");
                }

                // don't delete the inputFile because we'll need it for uploading
                deleteInputFile = false;

                // If the user would like to upload set the session processController to what the process class did with it
                session.setAttribute("processController", processController);

            // User doesn't want to upload, inform them of any validation warnings
            } else if (processController.getHasWarnings()) {
                retVal.append("{\"done\": \"");
                retVal.append("Warnings found on " + processController.getWorksheetName() + " worksheet.\n");
                retVal.append(processController.getWarningsSB().toString());
//                retVal.delete(retVal.lastIndexOf("\n"), retVal.lastIndexOf("\n") + 1);
                retVal.append("\"}");
            // User doesn't want to upload and the validation passed w/o any warnings or errors
            } else {
                retVal.append("{\"done\": \"");
                retVal.append(processController.getWorksheetName());
                retVal.append(" worksheet successfully validated.");
                retVal.append("\"}");
            }
        } catch (FIMSException e) {
            e.printStackTrace();
            retVal.append("{\"done\": \"Server Error\"}");
        }

        if (deleteInputFile) {
            new File(input_file).delete();
        }
        // need to escape all \t and \n characters for valid json
        return retVal.toString().replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t");
    }

    @GET
    @Path("/status")
    public String status(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();

        processController processController = (processController) session.getAttribute("processController");
        if (processController == null) {
            return "[{\"error\": \"No validation is currently running.\"}]";
        }

        return "[{\"status\": \"" + processController.getStatusSB().toString() + "\"}]";
    }

    @GET
    @Path("/continue")
    public void continueUpload(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();
    }

    private String saveTempFile(InputStream is, String ext) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File f = new File(tempDir, new stringGenerator().generateString(20) + '.' + ext);

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

    static String uploadpath() {
        return context.getRealPath("tripleOutput") + File.separator;
    }

}
