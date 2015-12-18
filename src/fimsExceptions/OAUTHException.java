package fimsExceptions;

/**
 * An exception that encapsulates errors from the biocode-fims oAuth system.
 */
public class OAUTHException extends FIMSAbstractException {

    public OAUTHException (String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }

    public OAUTHException (String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(usrMessage, developerMessage, httpStatusCode);
    }
}
