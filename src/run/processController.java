package run;

import digester.Validation;

/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class processController {
    private Boolean hasErrors = false;
    private StringBuilder errorsSB;
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

    public processController(Integer project_id, String expeditionCode) {
        this.expeditionCode = expeditionCode;
        this.project_id = project_id;
    }

    public StringBuilder getErrorsSB() {
        return errorsSB;
    }

    public void setErrorsSB(StringBuilder errorsSB) {
        this.errorsSB = errorsSB;
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
