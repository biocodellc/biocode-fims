package digester;

import fims.Uploader;
import renderers.RendererInterface;
import settings.FimsPrinter;

/**
 * Upload target specification
 */
public class Upload implements RendererInterface {
    private String target;
    private Mapping mapping;
    private String graph;
    Uploader uploader;

    public Upload(Mapping mapping) {
        this.mapping = mapping;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * Print output
     */
    public void print() {
        FimsPrinter.out.println("Upload ...");
        FimsPrinter.out.println("\tdoes this do anything??");
    }

    /**
     * Object details for this object
     */
    public void printObject() {
        FimsPrinter.out.println("Upload object target = " + target);
    }

    public boolean run() {
        return true;
    }
}
