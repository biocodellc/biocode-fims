package fimsExceptions;

import org.json.simple.JSONObject;

/**
 * An abstract exception to be extended by exceptions thrown to return appropriate responses.
 */
public abstract class FIMSAbstractException extends RuntimeException {
    String usrMessage = "Server Error";
    Integer httpStatusCode;
    String developerMessage;

    public FIMSAbstractException(String usrMessage, Integer httpStatusCode) {
        super();
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
    }

    public FIMSAbstractException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(developerMessage);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public FIMSAbstractException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public FIMSAbstractException(Integer httpStatusCode, Throwable cause) {
        super(cause);
        this.httpStatusCode = httpStatusCode;
    }

    public FIMSAbstractException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.developerMessage = developerMessage;
    }

    public FIMSAbstractException(JSONObject response) {
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