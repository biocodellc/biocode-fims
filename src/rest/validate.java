package rest;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import org.json.simple.JSONObject;
import run.configurationFileTester;
import settings.FIMSRuntimeException;
import run.guidify;
import run.process;
import run.processController;
import settings.bcidConnector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.channels.FileChannel;

/**
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
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String validate(@FormDataParam("project_id") Integer project_id,
                           @FormDataParam("expedition_code") String expedition_code,
                           @FormDataParam("upload") String upload,
                           @FormDataParam("public_status") String publicStatus,
                           @FormDataParam("dataset") InputStream is,
                           @FormDataParam("dataset") FormDataContentDisposition fileData,
                           @Context HttpServletRequest request) {
        StringBuilder retVal = new StringBuilder();
        Boolean removeController = true;
        Boolean deleteInputFile = true;
        String input_file;

        HttpSession session = request.getSession();
        String accessToken = (String) session.getAttribute("access_token");
        String refreshToken = (String) session.getAttribute("refresh_token");

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
            throw new FIMSRuntimeException("Server error saving file.", 500);
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

        // Test the configuration file to see that we're good to go...
        configurationFileTester cFT = new configurationFileTester();
        boolean configurationGood = true;

        cFT.init(p.configFile);
        //if (!cFT.checkUniqueKeys().toString().equals("")) {}

        if (!cFT.checkUniqueKeys()) {
            String message = "<br>CONFIGURATION FILE ERROR...<br>Please talk to your project administrator to fix the following error:<br>\t\n";
            message += cFT.getMessages();
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

                // set public status to true in processController if user wants it on
                if (publicStatus != null && publicStatus.equals("on")) {
                       processController.setPublicStatus(true);
                }

                // if there were vaildation warnings and user would like to upload, we need to ask the user to continue
                if (!processController.isValidated() && processController.getHasWarnings()) {
                    retVal.append("{\"continue_message\": {\"message\": \"");
                    retVal.append(processController.getStatusSB().toString());
                    retVal.append("\"}}");

                    // there were no validation warnings and the user would like to upload, so continue
                } else {
                    retVal.append("{\"continue_message\": {}}");
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
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
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

        try {
            // Create the process object --- this is done each time to orient the application
            process p = null;
            p = new process(
                    processController.getInputFilename(),
                    uploadpath(),
                    connector,
                    processController
            );

            // create this expedition if the user wants to
            if (createExpedition) {
                p.runExpeditionCreate();
            }

            if (!processController.isExpeditionAssignedToUserAndExists()) {
                p.runExpeditionCheck(false);
            }

            if (processController.isExpeditionCreateRequired()) {
                // ask the user if they want to create this expedition
                return "{\"continue_message\": \"The dataset code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
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

            processController.appendStatus("<br><font color=#188B00>Successfully Uploaded!</font>");

            return "{\"done\": \"" + processController.getStatusSB().toString() + "\"}";
        } finally {
            // if a new access token was issued, update the session variables
            if (connector.getRefreshedToken()) {
                session.setAttribute("access_token", connector.getAccessToken());
                session.setAttribute("refresh_token", connector.getRefreshToken());
            }
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
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
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

        try {
            // Create the process object --- this is done each time to orient the application
            process p = null;
            p = new process(
                    processController.getInputFilename(),
                    uploadpath(),
                    connector,
                    processController
            );

            // create this expedition if the user wants to
            if (createExpedition) {
                p.runExpeditionCreate();
            }

            if (!processController.isExpeditionAssignedToUserAndExists()) {
                p.runExpeditionCheck(true);
            }

            // Check to see if we need to create a new Expedition, if so we make a slight diversition
            if (processController.isExpeditionCreateRequired()) {
                // Ask the user if they want to create this expedition
                return "{\"continue_message\": \"The dataset code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                        "\\\" does not exist.  " +
                        "Do you wish to create it now?<br><br>" +
                        "If you choose to continue, your data will be associated with this new dataset code.\"}";
            }

            /*
            * Copy Spreadsheet to a standard location
            */
            // Get the BCID Root
            String bcidRoot = null;
            bcidRoot = connector.getArkFromDataset(
                    processController.getProject_id(),
                    processController.getExpeditionCode(),
                    "Resource");

            // Set input and output files
            File inputFile = new File(processController.getInputFilename());
            File outputFile = new File("/opt/jetty_files/" + inputFile.getName());

            // Run guidify, which adds a BCID to the spreadsheet
            //System.out.println("userId = " + session.getAttribute("userId"));
            //System.out.println("Session string = " + session.toString());
            //System.out.println("session attribute names = " + session.getAttributeNames());
            Integer userId = Integer.valueOf((String) session.getAttribute("userId"));
            //System.out.println("now userId = " + userId);
            guidify g = new guidify(
                    inputFile,
                    processController.getWorksheetName(),
                    userId,
                    bcidRoot);
            g.getSpreadsheet(outputFile);

            // Represent the dataset by an ARK... In the Spreadsheet Uploader option this
            // gives us a way to track what spreadsheets are uploaded into the system as they can
            // be tracked in the mysql database.  They also get an ARK but that is probably not useful.
            String datasetArk = null;
            // Create a dataset BCID
            datasetArk = connector.createDatasetBCID(null, inputFile.getName());
            // associate the BCID
            connector.associateBCID(p.getProcessController().getProject_id(), p.getProcessController().getExpeditionCode(), datasetArk);
            // Set the public status if relevant
            if (processController.getPublicStatus()) {
                connector.setExpeditionPublicStatus(true, processController.getProject_id(), processController.getExpeditionCode());
            } else {
                connector.setExpeditionPublicStatus(false, processController.getProject_id(), processController.getExpeditionCode());
            }

            // Remove the processController from the session
            session.removeAttribute("processController");

            processController.appendStatus("<br><font color=#188B00>Successfully Uploaded!</font>");

            // This is the message the user sees after succesfully uploading a spreadsheet to the server
            return "{\"done\": \"Successfully uploaded your spreadsheet to the server!<br>" +
                    //"server filename = " + outputFile.getName() + "<br>" +  \
                    "dataset code = " + processController.getExpeditionCode() + "<br>" +
                    "dataset ARK = " + datasetArk + "<br>" +
                    "resource ARK = " + bcidRoot + "<br>" +
                    "Please maintain a local copy of your File!<br>" +
                    "Your file will be processed soon for ingestion into RCIS.\"}";
        } finally {
            // if a new access token was issued, update the session variables
            if (connector.getRefreshedToken()) {
                session.setAttribute("access_token", connector.getAccessToken());
                session.setAttribute("refresh_token", connector.getRefreshToken());
            }
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
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
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

    /**
     * Copying files utility for organizing loaded spreadsheets on server if needed
     *
     * @param sourceFile
     * @param destFile
     */
    public static void copyFile(File sourceFile, File destFile) {
        try {
            if (!destFile.exists()) {
                destFile.createNewFile();
            }

            FileChannel source = null;
            FileChannel destination = null;

            try {
                source = new FileInputStream(sourceFile).getChannel();
                destination = new FileOutputStream(destFile).getChannel();
                destination.transferFrom(source, 0, source.size());
            } finally {
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            }
            // Not real clean but need to be able to allow others on the system to see file
            Runtime.getRuntime().exec("chmod 775 " + destFile);
        } catch (IOException e) {
            throw new FIMSRuntimeException(500, e);
        }
    }

}


