package fimsExceptions;

/**
* Exception class for handling FIMS Exceptions, which will be bubbled up to the calling classes and
 * handled appropriate in a simple dialog box.  This is to create a cleaner environment for handling error messages.
 *
 */
public class FimsException extends Exception {
    public FimsException() {}
    
    public FimsException(String s) {
        super(s);
    }

    public FimsException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public FimsException(Throwable throwable) {
        super(throwable);
    }
}
