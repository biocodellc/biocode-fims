package settings;

/**
 * Abstract class for printing output in a variety of situations.  For instance, to the System (command-line), or
 * to a dialog box (e.g. in Geneious), or to a REST service.
 */
public abstract class FimsInputter {

    public abstract boolean continueOperation(String question);
    public abstract void haltOperation(String message);


    // make the StandardPrinter the default output class so we never get a null pointer
    public static FimsInputter in = new StandardInputter();
}
