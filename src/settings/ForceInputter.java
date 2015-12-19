package settings;

/**
 * Always force a positive response
 */
public class ForceInputter extends FimsInputter {

    /**
     * just return true for continuing operation for ForceInputter
     * @param message
     * @return
     */
    @Override
    public boolean continueOperation(String message) {
        FimsPrinter.out.print(message);
       return true;
    }

    /**
     * haltOperation
     * @param message
     */
    @Override
    public void haltOperation(String message) {
        FimsPrinter.out.print(message);
    }
}
