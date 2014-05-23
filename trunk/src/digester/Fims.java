package digester;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.*;
//import com.sun.javaws.security.Resource;
import fims.fimsModel;
import org.apache.log4j.Level;
import renderers.RendererInterface;
import fims.uploader;
import run.processController;
import settings.bcidConnector;
import settings.fimsPrinter;

import java.io.File;
import java.lang.String;

/**
 * Add the core FIMS object
 */
public class Fims implements RendererInterface {
    private Metadata metadata;
    private Mapping mapping;
    private boolean updateGood = true;
    uploader uploader;
    private String bcid;

    public Fims(Mapping mapping) {
        this.mapping = mapping;
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
    public boolean run(bcidConnector bcidConnector, processController processController) throws Exception {
        Integer project_id = processController.getProject_id();
        String expedition_code = processController.getExpeditionCode();

        uploader = new uploader(
                metadata.getTarget(),
                new File(mapping.getTriplifier().getTripleOutputFile()));

        try {
            uploader.execute();
        } catch (Exception e) {
            e.printStackTrace();
            updateGood = false;
        }

        // If the update worked then set a BCID to refer to it
        if (updateGood) {
            try {
                bcid = bcidConnector.createDatasetBCID(uploader.getEndpoint(),uploader.getGraphID());
                // Create the BCID to use for upload service
                String status1 = "\tCreated BCID =" + bcid + " to represent your uploaded dataset";
                processController.appendStatus(status1);
                fimsPrinter.out.println(status1);
                // Associate the expedition_code with this bcid
                String status2 = "\tAssociator ... " + bcidConnector.associateBCID(project_id, expedition_code, bcid);
                processController.appendStatus(status2);
                fimsPrinter.out.println(status2);

            } catch (Exception e) {
                throw new Exception("Unable to create BCID", e);
            }
        }
        return updateGood;
    }

    /**
     * Print out command prompt data
     */
    public void print() {
        if (updateGood) {
            //fimsPrinter.out.println("\ttarget = " + metadata.getTarget());
            //fimsPrinter.out.println("\tBCID =  " + bcid);
            fimsPrinter.out.println("\tTemporary named graph reference = http://biscicol.org/id/" + bcid);
            fimsPrinter.out.println("\tGraph ID = " + uploader.getGraphID());
            fimsPrinter.out.println("\tView results as ttl = " + uploader.getConnectionPoint());
            //fimsPrinter.out.println("\tBCID (directs to graph endpoint) =  " + bcid);
        } else {
            fimsPrinter.out.println("\tUnable to reach FIMS server for upload at " + metadata.getTarget() + ". " +
                    "If this persists, your network may be blocking access to the database port. " +
                    "Please contact jdeck@berkeley.edu for more information.");
        }
    }

    /**
     * return the results of run()
     */
    public String results() {
        String retVal = "";
        if (updateGood) {
            retVal += "\tTemporary named graph reference = http://biscicol.org/id/" + bcid + "\n";
            retVal += "\tGraph ID = " + uploader.getGraphID() + "\n";
            retVal += "\tView results as ttl = " + uploader.getConnectionPoint() + "\n";
        } else {
            retVal += "\tUnable to reach FIMS server for upload at " + metadata.getTarget() + ". " +
                    "If this persists, your network may be blocking access to the database port. " +
                    "Please contact jdeck@berkeley.edu for more information.\n";
        }
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
    public fimsModel getFIMSModel(Model model) throws Exception {
       /*
        com.hp.hpl.jena.rdf.model.Resource r = model.getResource("ark:/21547/Hz2F9198780");
        StmtIterator s = r.listProperties();
        while (s.hasNext()) {
            Statement st = s.nextStatement();
            System.out.println(st.getSubject().toString() + " " + st.getPredicate().toString() + " "+ st.getObject().toString());
        }
        */

        // Set the default sheetname
        String sheetname = mapping.getDefaultSheetName();

        // Create a queryWriter object
        QueryWriter queryWriter = new QueryWriter(
                mapping.getAllAttributes(sheetname),
                sheetname);

        // Construct the FIMS model
        fimsModel fimsModel = new fimsModel(
                model,
                queryWriter);

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
