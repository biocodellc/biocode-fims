package renderers;

import java.lang.reflect.InvocationTargetException;

/**
 * Renderer Interface
 */
public interface RendererInterface {

    /**
     * Print output for command prompt
     */
    public void print();

    /**
     * Print Object Metadata
     */
    public void printObject();

    /**
     * require a run method that calls a renderer
     * An exception is thrown if some strange processing error occur.
     * @return true if successful and OK to progress to next step, e.g. in Validation return true if
     * validation rules have passed
     *
     */
    public boolean run() throws Exception;

}
