package bcidExceptions;

/**
 * An exception that encapsulates errors from the bcid oauth system.
 */
public class OAUTHException extends BCIDAbstractException {

    public OAUTHException (String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }

    public OAUTHException (String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(usrMessage, developerMessage, httpStatusCode);
    }
}
