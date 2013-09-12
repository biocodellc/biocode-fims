package digester;

import java.io.PrintWriter;

/**
 * All Mapping objects must implement these methods.
 */
public interface MappingInterface {

    /**
     * Print metadata about this element
     */
    public void print();

    /**
     * Print D2RQ Mapping file components
     *
     * @param pw     A printwriter to print output
     * @param parent A reference to a generic parent object
     */
    public void printD2RQ(PrintWriter pw, Object parent) throws Exception;
}

