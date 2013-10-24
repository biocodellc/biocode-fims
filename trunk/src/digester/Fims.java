package digester;

import com.hp.hpl.jena.util.FileManager;
import fims.fimsModel;
import org.apache.log4j.Level;
import renderers.RendererInterface;
import fims.uploader;
import settings.PathManager;
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
    private String graph;

    public Fims(Mapping mapping) {
        this.mapping = mapping;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public void addMetadata(Metadata m) {
        metadata = m;
    }

    /**
     * running the fims uploads the file to the target into a new graph
     *
     * @return
     */
    public boolean run() {
        fimsPrinter.out.println("Uploading to FIMS ...");

        uploader = new uploader(
                metadata.getTarget(),
                //new File("/Users/jdeck/IdeaProjects/biocode-fims/tripleOutput/biocode_template.xls.triples.80.ttl"));
                new File(mapping.getTriplifier().getTripleOutputFile()));

        try {
            graph = uploader.execute();
        } catch (Exception e) {
            //fimsPrinter.out.println("Exception occurred while attempting to upload data. " + e.getMessage());
            updateGood = false;
        }
        return updateGood;
    }

    /**
     * Print out command prompt data
     */
    public void print() {
        if (updateGood) {
            fimsPrinter.out.println("\ttarget = " + metadata.getTarget());
            fimsPrinter.out.println("\tgraph =  " + uploader.getGraph());
            fimsPrinter.out.println("\tquery = " + uploader.getService() + "/query" +
                    "?query=select+*+%7Bgraph+" + uploader.getEncodedGraph(true) + "++%7B%3Fs+%3Fp+%3Fo%7D%7D" +
                    "&output=text" +
                    "&stylesheet=%2Fxml-to-html.xsl");
        } else {
            fimsPrinter.out.println("\tUnable to reach FIMS server for upload at " + metadata.getTarget() + ". Try later ...");
        }
    }

    /**
     * Print out metadata object
     */
    public void printObject() {
        metadata.print();
    }

    /**
     * Write FIMS output to a spreadsheet, using the filename input/output data from the mapping/triplifier instance
     */
    public String write() throws Exception {
        File file = PathManager.createUniqueFile(
                mapping.getTriplifier().getFilenamePrefix() + ".xls",
                mapping.getTriplifier().getOutputFolder());
        String sheetname = mapping.getDefaultSheetName();
        // Create a queryWriter object
        QueryWriter queryWriter = new QueryWriter(
                mapping.getAllAttributes(sheetname),
                sheetname,
                file.getAbsolutePath());
        // Construct the FIMS model

        fimsModel fimsModel = new fimsModel(
                FileManager.get().loadModel(metadata.getTarget() + "/data?graph=" + uploader.getEncodedGraph(false)),
                queryWriter);
        // Read rows starting at the Resource node
        fimsModel.readRows("http://www.w3.org/2000/01/rdf-schema#Resource");
        // Send the filename back to the caller
        return fimsModel.toExcel();
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
        /*
        Just used for testing
        Model model = FileManager.get().loadModel("http://localhost:3030/ds/data?graph=urn:uuid:75959876-c944-4ad6-a173-d605f176bfae");
        fimsModel fimsModel = new fimsModel(model);
        // Read the rows starting with a specified class, Note: the assumption here is that row level metadata is a "Resource"
        fimsModel.readRows("http://www.w3.org/2000/01/rdf-schema#Resource");
        // Write output to JSON
        fimsPrinter.out.println(fimsModel.toJSON());
        */

        /*


         */

    }
}
