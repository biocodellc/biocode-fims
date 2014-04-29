package settings;

/**
 * Exception class for catching 401 responses. This allows the calling method to handle 401 error appropriately.
 */
public class NotAuthorizedException extends Exception {
    public NotAuthorizedException(Throwable throwable) {
        super(throwable);
    }

    public NotAuthorizedException(String s, Throwable throwable) {

        super(s, throwable);
    }

    public NotAuthorizedException(String s) {
        super(s);
    }
}
