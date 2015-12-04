package bcidExceptions;

/**
 * An exception that encapsulates errors from the bcid system.
 */
public class BCIDException extends Exception {
    public BCIDException() { super(); }
    public BCIDException(String Message) { super(Message); }
    public BCIDException(String Message, Throwable cause) { super(Message, cause); }
    public BCIDException(Throwable cause) { super(cause); }
}
