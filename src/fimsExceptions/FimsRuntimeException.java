package fimsExceptions;

import org.json.simple.JSONObject;

/**
 * An exception class to wrap exceptions thrown by the biocode-fims system.
 */
public class FimsRuntimeException extends FimsAbstractException {

    public FimsRuntimeException(String usrMessage, Integer httpStatusCode) {
        super(usrMessage, httpStatusCode);
    }

    public FimsRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(usrMessage, developerMessage, httpStatusCode);
    }

    public FimsRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }

    public FimsRuntimeException(Integer httpStatusCode, Throwable cause) {
        super(httpStatusCode, cause);
    }

    public FimsRuntimeException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, httpStatusCode, cause);
    }

    public FimsRuntimeException(JSONObject response) {
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
