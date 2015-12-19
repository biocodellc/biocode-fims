package fimsExceptions;

import javax.ws.rs.core.Response;

/**
 * An exception that encapsulates server errors
 */
public class ServerErrorException extends FimsAbstractException {
    private static Integer httpStatusCode = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    public ServerErrorException(String usrMessage, Throwable cause) {
        super(usrMessage, null, httpStatusCode, cause);
    }

    public ServerErrorException(String usrMessage) {
        super(usrMessage, httpStatusCode);
    }

    public  ServerErrorException(String usrMessage, String developerMessage) {
        super(usrMessage, developerMessage, httpStatusCode);
    }

    public ServerErrorException(Throwable cause) {
        super("Server Error", null, httpStatusCode, cause);
    }

    public ServerErrorException() {
        super("Server Error", httpStatusCode);
    }

    public ServerErrorException(String usrMessage, String developerMessage, Throwable cause) {
        super(usrMessage, developerMessage, httpStatusCode, cause);
    }
}