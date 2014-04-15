package run;

import digester.Validation;

/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class processController {
    private Boolean hasWarnings = false;
    private Boolean clearedOfWarnings = false;
    private Boolean expeditionAssignedToUser = false;   // checks that the user is authenticated against the supplied expedition
    private Boolean validated = false;
    private String inputFilename;
    private String expeditionCode;
    private Integer project_id;
    private Validation validation;

    public processController(Integer project_id, String expeditionCode) {
        this.expeditionCode = expeditionCode;
        this.project_id = project_id;
    }

    public Boolean getHasWarnings() {
        return hasWarnings;
    }

    public void setHasWarnings(Boolean hasWarnings) {
        this.hasWarnings = hasWarnings;
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

    public Boolean isExpeditionAssignedToUser() {
        return expeditionAssignedToUser;
    }

    public void setExpeditionAssignedToUser(Boolean expeditionAssignedToUser) {
        this.expeditionAssignedToUser = expeditionAssignedToUser;
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
        if (expeditionAssignedToUser &&
                validated &&
                clearedOfWarnings &&
                inputFilename != null &&
                expeditionCode != null &&
                project_id > 0)
            return true;
        else
            return false;
    }

    public String printStatus() {
        String retVal = "";
        if (clearedOfWarnings)
            retVal += "cleared Warnings";
        else
            retVal += "not cleared warnings";
        if (validated)
            retVal += "Validated";
        else
            retVal += "not validated";
        return retVal;

    }


}
