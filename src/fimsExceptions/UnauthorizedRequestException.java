package fimsExceptions;

import javax.ws.rs.core.Response;

/**
 * An exception that encapsulates unauthorized requests
 */
public class UnauthorizedRequestException extends FimsAbstractException {
    private static Integer httpStatusCode = Response.Status.UNAUTHORIZED.getStatusCode();

    public UnauthorizedRequestException(String usrMessage) {
        super(usrMessage,httpStatusCode);
    }

    public UnauthorizedRequestException(String usrMessage, String developerMessage) {
        super(usrMessage, developerMessage,httpStatusCode);
    }
}