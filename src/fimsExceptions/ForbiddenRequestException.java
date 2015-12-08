package fimsExceptions;

import javax.ws.rs.core.Response;

/**
 * An exception that encapsulates forbidden requests
 */
public class ForbiddenRequestException extends FIMSAbstractException {
    private static Integer httpStatusCode = Response.Status.FORBIDDEN.getStatusCode();

    public ForbiddenRequestException (String usrMessage) {
        super(usrMessage,httpStatusCode);
    }

    public ForbiddenRequestException (String usrMessage, String developerMessage) {
        super(usrMessage, developerMessage,httpStatusCode);
    }
}