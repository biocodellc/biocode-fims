package digester;

import bcid.bcidMinter;
import bcid.expeditionMinter;
import com.hp.hpl.jena.rdf.model.Model;
import fims.fimsModel;
import org.apache.log4j.Level;
import renderers.RendererInterface;
import fims.uploader;
import run.processController;
import settings.fimsPrinter;

import java.io.File;
import java.lang.String;

/**
 * Add the core FIMS object
 */
public class Fims implements RendererInterface {
    private Metadata metadata;
    private Mapping mapping;
    private Validation validation;
    uploader uploader;
    private String bcidPrefix;

    /**
     * Validation is usually NULL, fill it in when running CSPACE queries
     * @param mapping
     * @param validation
     */
    public Fims(Mapping mapping, Validation validation) {
        this.mapping = mapping;
        this.validation = validation;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void addMetadata(Metadata m) {
        metadata = m;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public uploader getUploader() {
        return uploader;
    }

    /**
     * running the fims uploads the file to the target into a new graph
     *
     * @return
     */
    public void run(processController processController) {
        Integer projectId = processController.getProjectId();
        String expeditionCode = processController.getExpeditionCode();

        uploader = new uploader(
                metadata.getTarget(),
                new File(mapping.getTriplifier().getTripleOutputFile()));

        uploader.execute();

        bcidMinter bcidMinter = new bcidMinter(false);
        bcidPrefix = bcidMinter.createEntityBcid(processController.getUserId(), "http://purl.org/dc/dcmitype/Dataset",
                uploader.getEndpoint(), uploader.getGraphID(), null, false);
        bcidMinter.close();
        // Create the BCID to use for upload service
        //String status1 = "\tCreated BCID " + bcidPrefix + " to represent your uploaded dataset\n";
        //status1 += "\tDataset metadata available at http://ezid.cdlib.org/id/" + bcidPrefix + "\n";
        String status1 = "\n\nDataset Identifier: http://n2t.net/" + bcidPrefix + " (wait 15 minutes for resolution to become active)\n";

        processController.appendStatus(status1);
        // Inform cmd line users
        fimsPrinter.out.println(status1);
        // Associate the expeditionCode with this bcidPrefix
        expeditionMinter expedition = new expeditionMinter();
        expedition.attachReferenceToExpedition(expeditionCode, bcidPrefix, projectId);
        expedition.close();
        String status2 = "\t" + "Data Elements Root: " + expeditionCode;
        processController.appendStatus(status2);
        // Inform cmd line users
        fimsPrinter.out.println(status2);
        return;
    }

    /**
     * Print out command prompt data
     */
    public void print() {
        //fimsPrinter.out.println("\ttarget = " + metadata.getTarget());
        //fimsPrinter.out.println("\tBCID =  " + bcidPrefix);
        //fimsPrinter.out.println("\tTemporary named graph reference = http://biscicol.org/id/" + bcidPrefix);
        //fimsPrinter.out.println("\tGraph ID = " + uploader.getGraphID());
        //fimsPrinter.out.println("\tView results as ttl = " + uploader.getConnectionPoint());

        //fimsPrinter.out.println("\tBCID (directs to graph endpoint) =  " + bcidPrefix);
    }

    /**
     * return the results of run()
     */
    public String results() {
        String retVal = "";
        //retVal += "\tTemporary named graph reference = http://biscicol.org/id/" + bcidPrefix + "\n";
        //retVal += "\tGraph ID = " + uploader.getGraphID() + "\n";
        //retVal += "\tView results as ttl = " + uploader.getConnectionPoint() + "\n";
        return retVal;
    }

    /**
     * Print out metadata object
     */
    public void printObject() {
        metadata.print();
    }

    /**
     * Create a fimsModel to store the results from this query
     */
    public fimsModel getFIMSModel(Model model, boolean getOnlySpecifiedProperties) {

        // Set the default sheetName
        String sheetName = mapping.getDefaultSheetName();

        // Create a queryWriter object
        QueryWriter queryWriter = new QueryWriter(
                mapping.getAllAttributes(sheetName),
                sheetName,
                validation);

        // Construct the FIMS model
        fimsModel fimsModel = new fimsModel(
                model,
                queryWriter,
                mapping,
                getOnlySpecifiedProperties);

        // Read rows starting at the Resource node
        fimsModel.readRows("http://www.w3.org/2000/01/rdf-schema#Resource");

        // Return the fimsModel
        return fimsModel;
    }

    /**
     * For Testing the FIMS query engine only
     *
     * @param args
     */
    public static void main(String[] args) {
        // Setup logging
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // Create model

        //Just used for testing
        /*Model model = FileManager.get().loadModel("http://localhost:3030/ds/data?graph=urn:uuid:75959876-c944-4ad6-a173-d605f176bfae");
        fimsModel fimsModel = new fimsModel(model);
        // Read the rows starting with a specified class, Note: the assumption here is that row level metadata is a "Resource"
        fimsModel.readRows("http://www.w3.org/2000/01/rdf-schema#Resource");
        // Write output to JSON
        fimsPrinter.out.println(fimsModel.toJSON());
         */

    }
}
