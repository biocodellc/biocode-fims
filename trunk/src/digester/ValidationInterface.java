package digester;

/**
 * All Validation objects must implement these methods.
 */
public interface ValidationInterface {

    /**
     * print metadata about this element
     */
    public void print();

    /**
     * run method can be called on all validation descendents so any of them can be validated
     *
     * @param o pass in an object value that defines the parent or current object for use downstream
     */
    public void run(Object o);
}
