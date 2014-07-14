package settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Always force a positive response
 */
public class forceInputter extends fimsInputter {

    /**
     * just return true for continuing operation for forceInputter
     * @param message
     * @return
     */
    @Override
    public boolean continueOperation(String message) {
        fimsPrinter.out.print(message);
       return true;
    }

    /**
     * haltOperation
     * @param message
     */
    @Override
    public void haltOperation(String message) {
        fimsPrinter.out.print(message);
    }
}
