package digester;

import renderers.RendererInterface;
import fims.uploader;
import settings.fimsPrinter;

/**
 * Upload target specification
 */
public class Upload implements RendererInterface {
    private String target;
    private Mapping mapping;
    private String graph;
    uploader uploader;

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
        fimsPrinter.out.println("Upload ...");
        fimsPrinter.out.println("\tdoes this do anything??");
    }

    /**
     * Object details for this object
     */
    public void printObject() {
        fimsPrinter.out.println("Upload object target = " + target);
    }

    public boolean run() {
        return true;
    }
}
