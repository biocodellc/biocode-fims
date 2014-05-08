package run;

import com.sun.xml.internal.fastinfoset.algorithm.BooleanEncodingAlgorithm;
import digester.Validation;
import org.json.simple.JSONObject;
import utils.stringGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class processController {
    private Boolean hasErrors = false;
    private Boolean hasWarnings = false;
    private StringBuilder warningsSB;
    private Boolean clearedOfWarnings = false;
    private Boolean expeditionAssignedToUserAndExists = false;   // checks that the user is authenticated against the supplied expedition
    private Boolean expeditionCreateRequired = false;
    private Boolean validated = false;
    private String inputFilename;
    private String expeditionCode;
    private Integer project_id;
    private Validation validation;
    private String worksheetName;
    private StringBuilder statusSB = new StringBuilder();
    private Boolean NMNH;

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

    public processController(Integer project_id, String expeditionCode) {
        this.expeditionCode = expeditionCode;
        this.project_id = project_id;
    }

    public StringBuilder getWarningsSB() {
        return warningsSB;
    }

    public void setWarningsSB(StringBuilder warningsSB) {
        this.warningsSB = warningsSB;
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


    public Integer getProject_id() {
        return project_id;
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
                project_id > 0)
            return true;
        else
            return false;
    }

    /**
     * take an InputStream and extension and write it to a file in the operating systems temp dir.
     *
     * @param is
     * @param ext
     * @return
     */
    public String saveTempFile(InputStream is, String ext) {
        String tempDir = System.getProperty("java.io.tmpdir");
        File f = new File(tempDir, new stringGenerator().generateString(20) + '.' + ext);

        try {
            OutputStream os = new FileOutputStream(f);
            try {
                byte[] buffer = new byte[4096];
                for (int n; (n = is.read(buffer)) != -1; )
                    os.write(buffer, 0, n);
            } finally {
                os.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return f.getAbsolutePath();
    }

    /**
     * return a string that is to be used in html and is json safe
     *
     * @param s
     * @return
     */
    public String stringToHTMLJSON(String s) {
        s = s.replaceAll("\n", "<br>").replaceAll("\t", "");
        return JSONObject.escape(s);
    }

    public String printStatus() {
        String retVal = "";
        retVal += "\tproject_id = " + project_id + "\n";
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


}
