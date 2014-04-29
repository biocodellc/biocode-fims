package settings;

/**
 * Created by rjewing on 4/19/14.
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
