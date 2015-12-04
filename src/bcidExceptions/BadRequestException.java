package bcidExceptions;

import javax.ws.rs.core.Response;

/**
 * An exception that encapsulates bad requests
 */
public class BadRequestException extends BCIDAbstractException {
    private static Integer httpStatusCode = Response.Status.BAD_REQUEST.getStatusCode();

    public BadRequestException(String usrMessage) {
        super(usrMessage,httpStatusCode);
    }

    public BadRequestException(String usrMessage, String developerMessage) {
        super(usrMessage, developerMessage,httpStatusCode);
    }
}
