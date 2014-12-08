package settings;

import org.json.simple.JSONObject;

/**
 * An exception class to wrap exceptions thrown by the biocode-fims system.
 */
public class FIMSRuntimeException extends RuntimeException {
    String usrMessage = "Server Error";
    Integer httpStatusCode;
    String developerMessage;

    public FIMSRuntimeException(String usrMessage, Integer httpStatusCode) {
        super();
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
    }

    public FIMSRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(developerMessage);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public FIMSRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public FIMSRuntimeException(Integer httpStatusCode, Throwable cause) {
        super(cause);
        this.httpStatusCode = httpStatusCode;
    }

    public FIMSRuntimeException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.developerMessage = developerMessage;
    }

    public FIMSRuntimeException(JSONObject response) {
        super((String) response.get("developerMessage"));
        this.httpStatusCode = ((Long) response.get("httpStatusCode")).intValue();
        this.usrMessage = (String) response.get("usrMessage");
        this.developerMessage = (String) response.get("developerMessage");
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
