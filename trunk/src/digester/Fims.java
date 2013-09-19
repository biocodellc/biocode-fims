package digester;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import renderers.RendererInterface;

import java.lang.reflect.InvocationTargetException;

/**
 * Add the core FIMS object
 */
public class Fims implements RendererInterface {
    private Metadata metadata;
    private Mapping mapping;

    public Fims(Mapping mapping) {
        this.mapping = mapping;
    }

    public void addMetadata(Metadata m) {
        metadata = m;
    }

    public boolean run() throws Exception {
        // Using TDB -- good to keep this stub code in case we want to write TDB to client
        // String directory = outputFolder + File.pathSeparator + "Dataset1";
        // DatasetGraph dataset = TDBFactory.createDatasetGraph(directory);
        // Model tdb = FileManager.get().loadModel(mapping.getOutputFile());

        // Perform a simple query on our dataset
        // String sparqlQueryString = "SELECT * { ?s ?p ?o }";
        // com.hp.hpl.jena.query.Query query = QueryFactory.create(sparqlQueryString);

        // Upload to Fuseki
        try {
            UpdateRequest updateRequest = UpdateFactory.read(mapping.getTriplifier().getUpdateOutputFile());
            UpdateProcessor qexec = UpdateExecutionFactory.createRemote(updateRequest, metadata.getTarget());
            qexec.execute();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Print out command prompt data
     */
    public void print() {
        System.out.println("Uploading to FIMS ...");
    }

    /**
     * Print out metadata object
     */
    public void printObject() {
        metadata.print();
    }

}
