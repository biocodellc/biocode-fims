package digester;

import com.hp.hpl.jena.update.*;
import com.sun.jdi.InvocationException;
import renderers.RendererInterface;
import sun.awt.CausedFocusEvent;
import uploader.uploader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Add the core FIMS object
 */
public class Fims implements RendererInterface {
    private Metadata metadata;
    private Mapping mapping;
    private boolean updateGood = true;
    uploader uploader;
    private String graph;

    public Fims(Mapping mapping) {
        this.mapping = mapping;
    }

    public void addMetadata(Metadata m) {
        metadata = m;
    }

    /**
     * running the fims uploads the file to the target into a new graph
     * @return
     */
    public boolean run() {
        uploader = new uploader(
                metadata.getTarget(),
                //new File("/Users/jdeck/IdeaProjects/biocode-fims/tripleOutput/biocode_template.xls.triples.80.ttl"));
                new File(mapping.getTriplifier().getTripleOutputFile()));

        try {
            graph = uploader.execute();
        } catch (Exception e) {
            //System.out.println("Exception occurred while attempting to upload data. " + e.getMessage());
            updateGood = false;
        }
        return updateGood;
    }

    /**
     * Print out command prompt data
     */
    public void print() {
        if (updateGood) {
            System.out.println("Uploading to FIMS ...");
            System.out.println("\ttarget = " + metadata.getTarget());
            System.out.println("\tgraph =  " + uploader.getGraph());
            System.out.println("\tquery = " + uploader.getService() + "/query" +
                    "?query=select+*+%7Bgraph+ " + uploader.getEncodedGraph() + "++%7B%3Fs+%3Fp+%3Fo%7D%7D" +
                    "&output=text" +
                    "&stylesheet=%2Fxml-to-html.xsl");
        } else {
            System.out.println("Uploading to FIMS ...");
            System.out.println("\tUnable to reach FIMS server for upload at " + metadata.getTarget() + ". Try later ...");
        }
    }

    /**
     * Print out metadata object
     */
    public void printObject() {
        metadata.print();
    }

}
