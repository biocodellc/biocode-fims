package digester;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import renderers.RendererInterface;
import uploader.uploader;

import java.io.File;

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
        System.out.println("Upload ...");
        System.out.println("\tdoes this do anything??");
    }

    /**
     * Object details for this object
     */
    public void printObject() {
        System.out.println("Upload object target = " + target);
    }

    public boolean run() {
        return true;
    }
}
