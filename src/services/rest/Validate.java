package services.rest;

import bcid.Bcid;
import bcid.BcidMinter;
import bcid.ExpeditionMinter;
import bcid.Resolver;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import digester.Mapping;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.simple.JSONObject;
import run.ConfigurationFileTester;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import run.Process;
import run.ProcessController;
import run.SIServerSideSpreadsheetTools;
import services.BiocodeFimsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.*;

/**
 */
@Path("validate")
public class Validate extends BiocodeFimsService {

    /**
     * service to validate a dataset against a project's rules
     *
     * @param projectId
     * @param expeditionCode
     * @param upload
     * @param is
     * @param fileData
     *
     * @return
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String validate(@FormDataParam("projectId") Integer projectId,
                           @FormDataParam("expeditionCode") String expeditionCode,
                           @FormDataParam("upload") String upload,
                           @FormDataParam("public_status") String publicStatus,
                           @FormDataParam("final_copy") String finalCopy,
                           @FormDataParam("dataset") InputStream is,
                           @FormDataParam("dataset") FormDataContentDisposition fileData) {
        StringBuilder retVal = new StringBuilder();
        Boolean removeController = true;
        Boolean deleteInputFile = true;
        String input_file;

        String username = (String) session.getAttribute("user");

        // create a new processController
        ProcessController processController = new ProcessController(projectId, expeditionCode);

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
            throw new FimsRuntimeException("Server error saving file.", 500);
        }

        // Create the process object --- this is done each time to orient the application
        Process p = null;
        p = new Process(
                input_file,
                uploadPath(),
                processController
        );

        // Test the configuration file to see that we're good to go...
        ConfigurationFileTester cFT = new ConfigurationFileTester();
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

                // verify that the user has logged in
                if (username == null) {
                    throw new UnauthorizedRequestException("You must be logged in to upload.");
                }
                processController.setUserId(username);
                // set public status to true in processController if user wants it on
                if (publicStatus != null && publicStatus.equals("on")) {
                       processController.setPublicStatus(true);
                }

                // set final copy to true in processController if user wants it on
                if (finalCopy != null && finalCopy.equals("on")) {
                    processController.setFinalCopy(true);
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
     *
     * @return
     */
    @GET
    @Path("/continue")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String upload(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition) {
        ProcessController processController = (ProcessController) session.getAttribute("processController");

        // if no processController is found, we can't do anything
        if (processController == null) {
            return "{\"error\": \"No process was detected.\"}";
        }

        // check if user is logged in
        if (processController.getUserId() == null) {
            return "{\"error\": \"You must be logged in to upload.\"}";
        }

        // if the process controller was stored in the session, then the user wants to continue, set warning cleared
        processController.setClearedOfWarnings(true);
        processController.setValidated(true);


        // Create the process object --- this is done each time to orient the application
        Process p = null;
        p = new Process(
                processController.getInputFilename(),
                uploadPath(),
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
            return "{\"continue_message\": \"The expedition code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                    "\\\" does not exist.  " +
                    "Do you wish to create it now?<br><br>" +
                    "If you choose to continue, your data will be associated with this new expedition code.\"}";
        }

        // upload the dataset
        p.runUpload();

        // delete the temporary file now that it has been uploaded
        new File(processController.getInputFilename()).delete();

        // remove the processController from the session
        session.removeAttribute("processController");

        processController.appendStatus("<br><font color=#188B00>Successfully Uploaded!</font>");

        return "{\"done\": \"" + processController.getStatusSB().toString() + "\"}";
    }

    /**
     * Service to upload a dataset to an expedition. The validate service must be called before this service.
     *
     * @param createExpedition
     *
     * @return
     */
    @GET
    @Path("/{a: (continue_nmnh|continue_spreadsheet) }")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String upload_spreadsheet(@QueryParam("createExpedition") @DefaultValue("false") Boolean createExpedition) {
        ProcessController processController = (ProcessController) session.getAttribute("processController");

        // if no processController is found, we can't do anything
        if (processController == null) {
            return "{\"error\": \"No process was detected.\"}";
        }

        if (processController.getUserId() == null) {
            throw new UnauthorizedRequestException("User is not authorized to create a new expedition.");
        }

        // if the process controller was stored in the session, then the user wants to continue, set warning cleared
        processController.setClearedOfWarnings(true);
        processController.setValidated(true);

        // Create the process object --- this is done each time to orient the application
        Process p = new Process(
                processController.getInputFilename(),
                uploadPath(),
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
            return "{\"continue_message\": \"The expedition code \\\"" + JSONObject.escape(processController.getExpeditionCode()) +
                    "\\\" does not exist.  " +
                    "Do you wish to create it now?<br><br>" +
                    "If you choose to continue, your data will be associated with this new expedition code.\"}";
        }

        /*
        * Copy Spreadsheet to a standard location
        */
        // Get the BCID Root
        Resolver r = new Resolver(processController.getExpeditionCode(), processController.getProjectId(), "Resource");
        String bcidRoot = r.getIdentifier();
        r.close();

        // Set input and output files
        File inputFile = new File(processController.getInputFilename());

        File outputFile = new File(sm.retrieveValue("serverRoot") + inputFile.getName());

        // Run guidify, which adds a BCID to the spreadsheet
        //System.out.println("userId = " + session.getAttribute("userId"));
        //System.out.println("Session string = " + session.toString());
        //System.out.println("session attribute names = " + session.getAttributeNames());
        Integer userId = Integer.valueOf((String) session.getAttribute("userId"));
        //System.out.println("now userId = " + userId);

        // Get the mapping object so we can discern the column_internal fields
        Mapping mapping = processController.getValidation().getMapping();

        // Smithsonian specific GUID to be attached to Sheet
        SIServerSideSpreadsheetTools siServerSideSpreadsheetTools = new SIServerSideSpreadsheetTools(
                inputFile,
                processController.getWorksheetName(),
                p.getMapping().getDefaultSheetUniqueKey(),
                bcidRoot);

        // Write GUIDs
        siServerSideSpreadsheetTools.guidify();

        siServerSideSpreadsheetTools.addInternalRowToHeader(mapping, Boolean.valueOf(sm.retrieveValue("replaceHeader")));

        siServerSideSpreadsheetTools.write(outputFile);

        // Represent the dataset by an ARK... In the Spreadsheet Uploader option this
        // gives us a way to track what spreadsheets are uploaded into the system as they can
        // be tracked in the mysql Database.  They also get an ARK but that is probably not useful.
        // Create a dataset BCID
        BcidMinter bcidMinter = new BcidMinter(Boolean.valueOf(sm.retrieveValue("ezidRequests")));
        String identifier = bcidMinter.createEntityBcid(new Bcid(userId, "http://purl.org/dc/dcmitype/Dataset", null,
                inputFile.getName(), null, processController.getFinalCopy(), false));
        bcidMinter.close();

        // associate the BCID
        ExpeditionMinter expeditionMinter = new ExpeditionMinter();
        expeditionMinter.attachReferenceToExpedition(p.getProcessController().getExpeditionCode(), identifier, p.getProject_id());
        // Set the public status
        expeditionMinter.updateExpeditionPublicStatus(userId, processController.getExpeditionCode(),
                    processController.getProjectId(), processController.getPublicStatus());
        expeditionMinter.close();

        // Remove the processController from the session
        session.removeAttribute("processController");

        processController.appendStatus("<br><font color=#188B00>Successfully Uploaded!</font>");

        // This is the message the user sees after succesfully uploading a spreadsheet to the server
        return "{\"done\": \"Successfully uploaded your spreadsheet to the server!<br>" +
                //"server filename = " + outputFile.getName() + "<br>" +  \
                "expedition code = " + processController.getExpeditionCode() + "<br>" +
                "dataset ARK = " + identifier + "<br>" +
                "resource ARK = " + bcidRoot + "<br>" +
                "Please maintain a local copy of your File!<br>" +
                "Your file will be processed soon for ingestion into RCIS.\"}";
    }

    /**
     * Service used for getting the current status of the dataset validation/upload.
     *
     * @return
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public String status() {
        ProcessController processController = (ProcessController) session.getAttribute("processController");
        if (processController == null) {
            return "{\"error\": \"Waiting for validation to process...\"}";
        }

        return "{\"status\": \"" + processController.getStatusSB().toString() + "\"}";
    }
}


