package run;

//import com.sun.xml.internal.fastinfoset.algorithm.BooleanEncodingAlgorithm;

import bcid.Database;
import biocode.fims.fimsExceptions.ServerErrorException;
import digester.Validation;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.StringGenerator;

import java.io.*;

/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class ProcessController {
    private Boolean hasErrors = false;
    private Boolean hasWarnings = false;
    private StringBuilder commandLineWarningsSB;
    private Boolean clearedOfWarnings = false;
    private Boolean expeditionAssignedToUserAndExists = false;   // checks that the user is authenticated against the supplied expedition
    private Boolean expeditionCreateRequired = false;
    private Boolean validated = false;
    private String inputFilename;
    private String expeditionCode;
    private Integer projectId;
    private Integer userId;
    private Validation validation;
    private String worksheetName;
    private StringBuilder statusSB = new StringBuilder();
    private Boolean NMNH;
    private Integer accessionNumber;
    private String defaultSheetUniqueKey;
    private Boolean publicStatus = false;   // default to false
    private Boolean finalCopy = false;

    private static Logger logger = LoggerFactory.getLogger(ProcessController.class);

    public String getWorksheetName() {
        return worksheetName;
    }

    public void setWorksheetName(String worksheetName) {
        this.worksheetName = worksheetName;
    }

    public Boolean getNMNH() {
        return NMNH;
    }

    public void setNMNH(String nmnh) {
        if (nmnh == null || !nmnh.equalsIgnoreCase("true"))
            NMNH = false;
        else
            NMNH = true;
    }

    public StringBuilder getStatusSB() {
        return statusSB;
    }

    public void appendStatus(String s) {
        statusSB.append(stringToHTMLJSON(s));
    }

    public ProcessController(Integer projectId, String expeditionCode) {
        this.expeditionCode = expeditionCode;
        this.projectId = projectId;
    }
     public ProcessController() {

    }

    public Boolean isExpeditionCreateRequired() {
        return expeditionCreateRequired;
    }

    public void setExpeditionCreateRequired(Boolean expeditionCreateRequired) {
        this.expeditionCreateRequired = expeditionCreateRequired;
    }

    public Boolean getHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
    }

    public Boolean getHasErrors() {
        return hasErrors;
    }

    public void setHasErrors(Boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public Boolean isClearedOfWarnings() {
        return clearedOfWarnings;
    }

    public void setClearedOfWarnings(Boolean clearedOfWarnings) {
        this.clearedOfWarnings = clearedOfWarnings;
    }

    public Boolean isValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public String getInputFilename() {
        return inputFilename;
    }

    public void setInputFilename(String inputFilename) {
        this.inputFilename = inputFilename;
    }

    public String getExpeditionCode() {
        return expeditionCode;
    }


    public Integer getProjectId() {
        return projectId;
    }

    public Boolean isExpeditionAssignedToUserAndExists() {
        return expeditionAssignedToUserAndExists;
    }

    public void setExpeditionAssignedToUserAndExists(Boolean expeditionAssignedToUserAndExists) {
        this.expeditionAssignedToUserAndExists = expeditionAssignedToUserAndExists;
    }

    public Validation getValidation() {
        return validation;
    }


    public void setValidation(Validation validation) {
        this.validation = validation;
    }

    public Boolean getPublicStatus() {
        return publicStatus;
    }

    public void setPublicStatus(Boolean publicStatus) {
        this.publicStatus = publicStatus;
    }

    /**
     * Tells whether the given filename is ready to upload
     *
     * @return
     */
    public Boolean isReadyToUpload() {
        if (expeditionAssignedToUserAndExists &&
                validated &&
                inputFilename != null &&
                expeditionCode != null &&
                projectId > 0)
            return true;
        else
            return false;
    }


    /**
     * return a string that is to be used in html and is json safe
     *
     * @param s
     *
     * @return
     */
    public String stringToHTMLJSON(String s) {
        s = s.replaceAll("\n", "<br>").replaceAll("\t", "");
        return JSONObject.escape(s);
    }

    public String printStatus() {
        String retVal = "";
        retVal += "\tprojectId = " + projectId + "\n";
        retVal += "\texpeditionCode = " + expeditionCode + "\n";
        retVal += "\tinputFilename = " + inputFilename + "\n";

        if (clearedOfWarnings)
            retVal += "\tclearedOfWarnings=true\n";
        else
            retVal += "\tclearedOfWarnings=true\n";
        if (hasWarnings)
            retVal += "\thasWarnings=true\n";
        else
            retVal += "\thasWarnings=true\n";
        if (expeditionAssignedToUserAndExists)
            retVal += "\texpeditionAssignedToUser=true\n";
        else
            retVal += "\texpeditionAssignedToUser=false\n";
        if (validated)
            retVal += "\tvalidated=true\n";
        else
            retVal += "\tvalidated=false\n";

        return retVal;
    }

    /**
     * take an InputStream and extension and write it to a file in the operating systems temp dir.
     *
     * @param is
     * @param ext
     *
     * @return
     */
    public String saveTempFile(InputStream is, String ext) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File f = new File(tempDir, new StringGenerator().generateString(20) + '.' + ext);

        try {
            OutputStream os = new FileOutputStream(f);
            try {
                byte[] buffer = new byte[4096];
                for (int n; (n = is.read(buffer)) != -1; )
                    os.write(buffer, 0, n);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            logger.warn("IOException", e);
            return null;
        }
        return f.getAbsolutePath();
    }

    public void setAccessionNumber(Integer accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public Integer getAccessionNumber() {
        return accessionNumber;
    }

    public void setDefaultSheetUniqueKey(String defaultSheetUniqueKey) {
        this.defaultSheetUniqueKey = defaultSheetUniqueKey;
    }

    public String getDefaultSheetUniqueKey() {
        return defaultSheetUniqueKey;
    }

    public void setCommandLineSB(StringBuilder commandLineWarningsSB) {
        this.commandLineWarningsSB = commandLineWarningsSB;
    }
    public StringBuilder getCommandLineSB() {
        return commandLineWarningsSB;
    }

    public void setFinalCopy(Boolean finalCopy) {
        this.finalCopy = finalCopy;
    }

    public boolean getFinalCopy() {
        return finalCopy;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * method for setting the userId when given a username
     * @param username
     */
    public void setUserId(String username) {
        Database db = new Database();
        Integer userId = db.getUserId(username);
        if (userId == null) {
            throw new ServerErrorException("invalid username");
        }
        this.userId = userId;
    }
}
