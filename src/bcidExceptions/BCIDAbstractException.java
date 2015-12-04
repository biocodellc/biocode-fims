package bcidExceptions;

/**
 * An abstract exception to be extended by exceptions thrown to return appropriate responses.
 */
public abstract class BCIDAbstractException extends RuntimeException{
    String usrMessage;
    Integer httpStatusCode;
    String developerMessage;

    public BCIDAbstractException(String usrMessage, Integer httpStatusCode) {
        super();
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
    }

    public BCIDAbstractException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(developerMessage);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public BCIDAbstractException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.usrMessage = usrMessage;
        this.developerMessage = developerMessage;
    }

    public BCIDAbstractException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, cause);
        this.httpStatusCode = httpStatusCode;
        this.developerMessage = developerMessage;
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