package bcidExceptions;

/**
 * An exception that encapsulates errors from the bcid system.
 */
public class BCIDRuntimeException extends BCIDAbstractException {

    public BCIDRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(usrMessage, developerMessage, httpStatusCode);
    }

    public BCIDRuntimeException(String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(developerMessage, httpStatusCode, cause);
    }

    public BCIDRuntimeException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }
}
