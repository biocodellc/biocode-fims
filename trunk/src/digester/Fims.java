package digester;

import renderers.RendererInterface;

/**
 * Add the core FIMS object
 */
public class Fims implements RendererInterface{
    private Metadata metadata;

    public void addMetadata(Metadata m) {
        metadata = m;
    }

    public void print() {
        metadata.print();
    }
    public void printCommand() {
        System.out.println("Reading metadata ...");
    }

}
