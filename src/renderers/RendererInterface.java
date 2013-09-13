package renderers;

/**
 * Define the types of renderer functions there are for appropriate objects
 */
public interface RendererInterface {
    /**
     * Print metadata about objects themselves
     */
    public void print();

    /**
     * Print output suitable for a command prompt
     */
    public void printCommand();
}
