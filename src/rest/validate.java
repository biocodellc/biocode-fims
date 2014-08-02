package rest;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.json.simple.JSONObject;
import run.configurationFileError;
import run.configurationFileTester;
import run.process;
import run.processController;
import settings.FIMSException;
import settings.bcidConnector;
import settings.fimsPrinter;

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

    /**
     * service to validate a dataset against a project's rules
     *
     * @param project_id
     * @param expedition_code
     * @param upload
     * @param is
     * @param fileData
     * @param request
     *
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public String validate(@FormDataParam("project_id") Integer project_id,
                           @FormDataParam("expedition_code") String expedition_code,
                           @FormDataParam("upload") String upload,
                           @FormDataParam("dataset") InputStream is,
                           @FormDataParam("dataset") FormDataContentDisposition fileData,
                           @Context HttpServletRequest request) {
        StringBuilder retVal = new StringBuilder();
        Boolean removeController = true;
        Boolean deleteInputFile = true;
        String input_file = null;

        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");

        try {

            // create a new processController
            processController processController = new processController(project_id, expedition_code);

            // place the processController in the session here so that we can track the status of the validation process
            // by calling rest/validate/status
            session.setAttribute("processController", processController);


            // update the status
            processController.appendStatus("Initializing...<br>");
            processController.appendStatus("inputFilename = " + processController.stringToHTMLJSON(
                    fileData.getFileName()) + "<br>");

            // Save the uploaded file
            String splitArray[] = fileData.getFileName().split("\\.");
            String ext;
            if (splitArray.length == 0) {
                // if no extension is found, then guess
                ext = "xls";
            } else {
                ext = splitArray[splitArray.length - 1];
            }
            input_file = processController.saveTempFile(is, ext);
            // if input_file null, then there was an error saving the file
            if (input_file == null) {
                throw new FIMSException("Server error saving file.");
            }

            bcidConnector connector = new bcidConnector(accessToken, refreshToken);

            // Create the process object --- this is done each time to orient the application
            process p = null;
            p = new process(
                    input_file,
                    uploadpath(),
                    connector,
                    processController
            );

            // Test the configuration file to see that we're good to go... definately stop the process if there is an error
            configurationFileTester cFT = new configurationFileTester();
            boolean configurationGood = true;
            try {
                cFT.testConfigFile(p.configFile);
            } catch (configurationFileError configurationFileError) {
                String message = "<br>CONFIGURATION FILE ERROR...<br>Please talk to your project administrator to fix the following error:<br>\t" + configurationFileError.getMessage();
                processController.setHasErrors(true);
                processController.setValidated(false);
                processController.appendStatus(message + "<br>");
                configurationGood = false;
                retVal.append("{\"done\": \"");
                retVal.append(processController.getStatusSB().toString());
                retVal.append("\"}");
            }

            // Run the process only if the configuration is good.
            if (configurationGood) {
                processController.appendStatus("Validating...<br>");

                p.runValidation();

                // if there were validation errors, we can't upload
                if (processController.getHasErrors()) {
                    retVal.append("{\"done\": \"");
                    retVal.append(processController.getStatusSB().toString());
                    retVal.append("\"}");

                } else if (upload != null && upload.equals("on")) {
                    // if there were vaildation warnings and user would like to upload, we need to ask the user to continue
                    if (!processController.isValidated() && processController.getHasWarnings()) {
                        retVal.append("{\"continue\": {\"message\": \"");
                        retVal.append(processController.getStatusSB().toString());
                        retVal.append("\"}}");

                        // there were no validation warnings and the user would like to upload, so continue
                    } else {
                        retVal.append("{\"continue\": {}}");
                    }

                    // don't delete the inputFile because we'll need it for uploading
                    deleteInputFile = false;

                    // don't remove the controller as we will need it later for uploading this file
                    removeController = false;

                    // User doesn't want to upload, inform them of any validation warnings
                } else if (processController.getHasWarnings()) {
                    retVal.append("{\"done\": \"");
                    retVal.append(processController.getStatusSB().toString());
                    retVal.append("\"}");
                    // User doesn't want to upload and the validation passed w/o any warnings or errors
                } else {
                    //processController.appendStatus("<br><font color=#188B00>" + processController.getWorksheetName() +
                    processController.appendStatus("<br>" + processController.getWorksheetName() +
                            " worksheet successfully validated.");
                    retVal.append("{\"done\": \"");
                    retVal.append(processController.getStatusSB());
                    retVal.append("\"}");
                }
            }
        } catch (FIMSException e) {
            e.printStackTrace();
            // Delete the input file if an exception was thrown
            try {
                new File(input_file).delete();
            } catch (Exception e2) {
                return "{\"done\": \"Server Error: " + e.getMessage() + ";" + e2.getMessage() + "\"}";
            }
            return "{\"done\": \"Server Error: " + e.getMessage() + "\"}";
        }

        if (deleteInputFile && input_file != null) {
            new File(input_file).delete();
        }
        if (removeController) {
            session.removeAttribute("processController");
        }

        return retVal.toString();
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param createExpedition
     * @param request
     *
     * @return
     */
    @GET
    @Path("/continue")
    @Produces(MediaType.APPLICATION_JSON)
    public String upload(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition,
                         @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");
        processController processController = (processController) session.getAttribute("processController");

        // if no processController is found, we can't do anything
        if (processController == null) {
            return "{\"error\": \"No process was detected.\"}";
        }

        // if the process controller was stored in the session, then the user wants to continue, set warning cleared
        processController.setClearedOfWarnings(true);
        processController.setValidated(true);

        bcidConnector connector = new bcidConnector(accessToken, refreshToken);

        // Create the process object --- this is done each time to orient the application
        process p = null;
        try {
            try {
                p = new process(
                        processController.getInputFilename(),
                        uploadpath(),
                        connector,
                        processController
                );
            } catch (FIMSException e) {
                e.printStackTrace();
                //throw new FIMSException("{\"error\": \"Server Error.\"}");
                throw new FIMSException("{\"error\": \"Server Error: " + e.getMessage() + "\"}");
            }

            // create this expedition if the user wants to
            if (createExpedition) {
                try {
                    p.runExpeditionCreate();
                } catch (FIMSException e) {
                    e.printStackTrace();
                    throw new FIMSException("{\"error\": \"Error creating dataset.\"}");
                }
            }

            try {
                if (!processController.isExpeditionAssignedToUserAndExists()) {
                    p.runExpeditionCheck();
                }

                if (processController.isExpeditionCreateRequired()) {
                    // if a new access token was issued, update the session variables
                    if (connector.getRefreshedToken()) {
                        session.setAttribute("access_token", connector.getAccessToken());
                        session.setAttribute("refresh_token", connector.getRefreshToken());
                    }
                    // ask the user if they want to create this expedition
                    return "{\"continue\": \"The dataset code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                            "\\\" does not exist.  " +
                            "Do you wish to create it now?<br><br>" +
                            "If you choose to continue, your data will be associated with this new dataset code.\"}";
                }

                // upload the dataset
                p.runUpload();

                // delete the temporary file now that it has been uploaded
                new File(processController.getInputFilename()).delete();

                // remove the processController from the session
                session.removeAttribute("processController");

                if (connector.getRefreshedToken()) {
                    session.setAttribute("access_token", connector.getAccessToken());
                    session.setAttribute("refresh_token", connector.getRefreshToken());
                }

                processController.appendStatus("<br><font color=#188B00>Successfully Uploaded!</font>");

                return "{\"done\": \"" + processController.getStatusSB().toString() + "\"}";
            } catch (FIMSException e) {
                e.printStackTrace();
                throw new FIMSException("{\"error\": \"Server Message: " + e.getMessage() + "\"}");
            }
        } catch (FIMSException e) {
            // delete the temporary file now that it has been uploaded
            new File(processController.getInputFilename()).delete();
            // remove the processController from the session
            session.removeAttribute("processController");

            return e.getMessage();
        }
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param createExpedition
     * @param request
     *
     * @return
     */
    @GET
    @Path("/continue_spreadsheet")
    @Produces(MediaType.APPLICATION_JSON)
    public String upload_spreadsheet(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition,
                         @Context HttpServletRequest request) {
        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");
        processController processController = (processController) session.getAttribute("processController");

        // if no processController is found, we can't do anything
        if (processController == null) {
            return "{\"error\": \"No process was detected.\"}";
        }

        // if the process controller was stored in the session, then the user wants to continue, set warning cleared
        processController.setClearedOfWarnings(true);
        processController.setValidated(true);

        bcidConnector connector = new bcidConnector(accessToken, refreshToken);

        // Create the process object --- this is done each time to orient the application
        process p = null;
        try {
            try {
                p = new process(
                        processController.getInputFilename(),
                        uploadpath(),
                        connector,
                        processController
                );
            } catch (FIMSException e) {
                e.printStackTrace();
                //throw new FIMSException("{\"error\": \"Server Error.\"}");
                throw new FIMSException("{\"error\": \"Server Error: " + e.getMessage() + "\"}");
            }

            // create this expedition if the user wants to
            if (createExpedition) {
                try {
                    p.runExpeditionCreate();
                } catch (FIMSException e) {
                    e.printStackTrace();
                    throw new FIMSException("{\"error\": \"Error creating dataset.\"}");
                }
            }

            try {
                if (!processController.isExpeditionAssignedToUserAndExists()) {
                    p.runExpeditionCheck();
                }

                if (processController.isExpeditionCreateRequired()) {
                    // if a new access token was issued, update the session variables
                    if (connector.getRefreshedToken()) {
                        session.setAttribute("access_token", connector.getAccessToken());
                        session.setAttribute("refresh_token", connector.getRefreshToken());
                    }
                    // ask the user if they want to create this expedition
                    return "{\"continue\": \"The dataset code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                            "\\\" does not exist.  " +
                            "Do you wish to create it now?<br><br>" +
                            "If you choose to continue, your data will be associated with this new dataset code.\"}";
                }

                // upload the dataset
                //p.runUpload();

                // delete the temporary file now that it has been uploaded
                new File(processController.getInputFilename()).delete();

                // remove the processController from the session
                session.removeAttribute("processController");

                if (connector.getRefreshedToken()) {
                    session.setAttribute("access_token", connector.getAccessToken());
                    session.setAttribute("refresh_token", connector.getRefreshToken());
                }

                processController.appendStatus("<br><font color=#188B00>Successfully Uploaded!</font>");

                return "{\"done\": \"Successfully uploaded your spreadsheet to the server, <br>" +
                        "spreadsheet name = " + processController.getInputFilename() + "<br>" +
                        "dataset code = " + processController.getExpeditionCode() + "<br>" +
                        "please maintain a local copy for now.  You should be notified of action soon.\"}";

            } catch (FIMSException e) {
                e.printStackTrace();
                throw new FIMSException("{\"error\": \"Server Message: " + e.getMessage() + "\"}");
            }
        } catch (FIMSException e) {
            // delete the temporary file now that it has been uploaded
            new File(processController.getInputFilename()).delete();
            // remove the processController from the session
            session.removeAttribute("processController");

            return e.getMessage();
        }
    }
    /**
     * Service used for getting the current status of the dataset validation/upload.
     *
     * @param request
     *
     * @return
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public String status(@Context HttpServletRequest request) {
        HttpSession session = request.getSession();

        processController processController = (processController) session.getAttribute("processController");
        if (processController == null) {
            return "{\"error\": \"Waiting for validation to process...\"}";
        }

        return "{\"status\": \"" + processController.getStatusSB().toString() + "\"}";
    }

    static String uploadpath() {
        return context.getRealPath("tripleOutput") + File.separator;
    }

}
