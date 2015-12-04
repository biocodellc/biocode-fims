package bcid.Renderer;

/**
 * rendererInterface defines an interface for working with rendered identifiers .
 * Can enter an object, printMetadata, and leave.
 * These methods are meant to populate a class level variable in the
 * Renderer
 */
public interface RendererInterface  {


    /**
     * Enter the genericIdentifier and render any information before looking at metadata
     */
    public void enter();

    /**
     * Print an identifier's metadata
     */
    public void printMetadata();

    /**
     * Leave the object and print any relevant closing information
     */
    public void leave();

    /**
     * Need to always check the identifier and provide a consistent method for returning
     * error messages if it is bad.
     */
    public boolean validIdentifier();

}
