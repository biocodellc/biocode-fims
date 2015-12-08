package fimsExceptions;

/**
* Exception class for handling FIMS Exceptions, which will be bubbled up to the calling classes and
 * handled appropriate in a simple dialog box.  This is to create a cleaner environment for handling error messages.
 *
 */
public class FIMSException extends Exception {
    public FIMSException() {}
    
    public FIMSException(String s) {
        super(s);
    }

    public FIMSException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public FIMSException(Throwable throwable) {
        super(throwable);
    }
}
