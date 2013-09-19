package digester;

import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateProcessor;
import com.hp.hpl.jena.update.UpdateRequest;
import renderers.RendererInterface;

/**
 * Upload target specification
 */
public class Upload implements RendererInterface {
    private String target;
    private Mapping mapping;

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
        System.out.println("\tuploaded data to " + target);
    }

    /**
     * Object details for this object
     */
    public void printObject() {
        System.out.println("Upload object target = " + target);
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
            UpdateProcessor qexec = UpdateExecutionFactory.createRemote(updateRequest, target);
            qexec.execute();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
