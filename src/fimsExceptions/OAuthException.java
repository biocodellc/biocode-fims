package fimsExceptions;

/**
 * An exception that encapsulates errors from the biocode-fims oAuth system.
 */
public class OAuthException extends FimsAbstractException {

    public OAuthException(String usrMessage, String developerMessage, Integer httpStatusCode, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }

    public OAuthException(String usrMessage, String developerMessage, Integer httpStatusCode) {
        super(usrMessage, developerMessage, httpStatusCode);
    }
}
