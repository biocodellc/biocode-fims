package run;

import org.w3c.dom.html.HTMLTableCaptionElement;

import javax.servlet.http.HttpSession;

/**
 * Created by IntelliJ IDEA.
 * User: jdeck
 * Date: 4/14/14
 * Time: 11:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class processController {
    private Boolean clearedWarnings = false;
    private Boolean validated = false;

    public Boolean getClearedWarnings() {
        return clearedWarnings;
    }

    public void setClearedWarnings(Boolean clearedWarnings) {
        this.clearedWarnings = clearedWarnings;
    }

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public String printStatus() {
        String retVal = "";
        if (clearedWarnings)
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
