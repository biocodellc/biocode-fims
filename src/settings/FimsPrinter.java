package settings;

/**
 * Abstract class for printing output in a variety of situations.  For instance, to the System (command-line), or
 * to a dialog box (e.g. in Geneious), or to a REST service.
 */
public abstract class FimsPrinter {
    public abstract void print(String content);
    public abstract void println(String content);
    // make the StandardPrinter the default output class so we never get a null pointer
    public static FimsPrinter out = new StandardPrinter();
}
