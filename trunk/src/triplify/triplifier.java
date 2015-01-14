package triplify;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import digester.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.processController;
import settings.FIMSRuntimeException;
import settings.PathManager;
import settings.fimsPrinter;

import java.io.*;

/**
 * Triplify source file, using code adapted from the BiSciCol Triplifier
 * http://code.google.com/p/triplifier
 */
public class triplifier {

    private String outputFolder;
    private Model model;
    private String tripleOutputFile;
    private String updateOutputFile;
    private String filenamePrefix;

    private static Logger logger = LoggerFactory.getLogger(triplifier.class);

    /**
     * triplify dataset on the tabularDataReader, writing output to the specified outputFolder and filenamePrefix
     * @param filenamePrefix
     * @param outputFolder
     */
    public triplifier(String filenamePrefix, String outputFolder) {
        this.outputFolder = outputFolder;
        this.filenamePrefix = filenamePrefix;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public String getFilenamePrefix() {
        return filenamePrefix;
    }

    public Model getModel() {
        return model;
    }

    public String getTripleOutputFile() {
        return tripleOutputFile;
    }

    public String getUpdateOutputFile() {
        return updateOutputFile;
    }


    /**
     * Return triples
     *
     * @param mapping
     * @return
     */
    public void getTriples(Mapping mapping, processController processController) {
        //String filenamePrefix = inputFile.getName();
        System.gc();
        String status = "\tWriting Temporary Output ...";
        processController.appendStatus(status + "<br>");
        // Inform cmd line users
        fimsPrinter.out.println(status);

        // Write the model
        model = new ModelD2RQ(FileUtils.toURL(getMapping(filenamePrefix, mapping, true)),
                FileUtils.langN3, "urn:x-biscicol:");
        // Write the model as simply a Turtle file
        File tripleFile = PathManager.createUniqueFile(filenamePrefix + ".n3", outputFolder);
        try {
            FileOutputStream fos = new FileOutputStream(tripleFile);
            model.write(fos, FileUtils.langNTriple, null);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new FIMSRuntimeException(500, e);
        } catch (IOException e) {
            logger.warn("IOException thrown trying to close FileOutputStream object.", e);
        }
        tripleOutputFile = outputFolder + File.separator +  tripleFile.getName();

        if (tripleFile.length() < 1)
            throw new FIMSRuntimeException("No triples to write!", 500);
    }

    /**
     * Construct the mapping file for D2RQ to read
     *
     * @param filenamePrefix
     * @param mapping
     * @param verifyFile
     * @return
     */
    private String getMapping(String filenamePrefix, Mapping mapping, Boolean verifyFile) {
        if (verifyFile)
            mapping.connection.verifyFile();

        File mapFile = PathManager.createUniqueFile(filenamePrefix + ".mapping.n3", outputFolder);
        try {
            PrintWriter pw = new PrintWriter(mapFile);
            mapping.printD2RQ(pw, mapping);
            pw.close();
        } catch (FileNotFoundException e) {
            throw new FIMSRuntimeException(500, e);
        }
        return outputFolder + File.separator + mapFile.getName();
    }
}
