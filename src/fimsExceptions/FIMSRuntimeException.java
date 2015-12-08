package fimsExceptions;

import org.json.simple.JSONObject;

/**
 * An exception class to wrap exceptions thrown by the biocode-fims system.
 */
public class FIMSRuntimeException extends FIMSAbstractException {

    public FIMSRuntimeException(String usrMessage, Integer httpStatusCode) {
        super(usrMessage, httpStatusCode);
    }

    public FIMSRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(usrMessage, developerMessage, httpStatusCode);
    }

    public FIMSRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }

    public FIMSRuntimeException(Integer httpStatusCode, Throwable cause) {
        super(httpStatusCode, cause);
    }

    public FIMSRuntimeException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, httpStatusCode, cause);
    }

    public FIMSRuntimeException(JSONObject response) {
        super(response);
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getUsrMessage() {
        return usrMessage;
    }

    public String getDeveloperMessage() {
        return developerMessage;
    }
}
