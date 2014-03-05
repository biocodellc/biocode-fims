package triplify;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import digester.Mapping;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import settings.PathManager;
import settings.fimsPrinter;

import javax.print.DocFlavor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

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

    /**
     * triplify dataset on the tabularDataReader, writing output to the specified outputFolder and filenamePrefix
     * @param filenamePrefix
     * @param outputFolder
     * @throws Exception
     */
    public triplifier(String filenamePrefix, String outputFolder) throws Exception {
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
     * @throws Exception
     */
    public void getTriples(Mapping mapping) throws Exception {
        //String filenamePrefix = inputFile.getName();
        System.gc();
        fimsPrinter.out.println("\tWriting Temporary Output ...");

        // Write the model
        model = new ModelD2RQ(FileUtils.toURL(getMapping(filenamePrefix, mapping, true)),
                FileUtils.langN3, "urn:x-biscicol:");
        // Write the model as simply a Turtle file
        File tripleFile = PathManager.createUniqueFile(filenamePrefix + ".ttl", outputFolder);
        FileOutputStream fos = new FileOutputStream(tripleFile);
        model.write(fos, FileUtils.langTurtle,null);
        fos.close();
        tripleOutputFile = outputFolder + File.separator +  tripleFile.getName();

        // Write out as a Sparql Update Statement
        File updateFile = PathManager.createUniqueFile(filenamePrefix + ".n3", outputFolder);
        FileOutputStream fosUpdateFile = new FileOutputStream(updateFile);
        fosUpdateFile.write("INSERT DATA {\n".getBytes());
        StmtIterator stmtIterator = model.listStatements();
        while (stmtIterator.hasNext()) {
            Statement stmt = stmtIterator.next();
            String subject = "", predicate = "", object = "";
            if (stmt.asTriple().getSubject().isURI()) {
                subject = "<" + stmt.asTriple().getSubject().toString() + ">";
            }
            if (stmt.asTriple().getPredicate().isURI()) {
                predicate = "<" + stmt.asTriple().getPredicate().toString() + ">";
            }
            if (stmt.asTriple().getObject().isURI()) {
                object = "<" + stmt.asTriple().getObject().toString() + ">";
            } else {
                object = stmt.asTriple().getObject().toString();
            }
            // get the content in bytes
            byte[] contentInBytes = (subject + " " + predicate + " " + object + " .\n").getBytes();

            fosUpdateFile.write(contentInBytes);
        }
        fosUpdateFile.write("}".getBytes());
        fosUpdateFile.close();

        //return outputFolder + tripleFile.getName();
        updateOutputFile =  outputFolder + File.separator + updateFile.getName();
    }

    /**
     * Construct the mapping file for D2RQ to read
     *
     * @param filenamePrefix
     * @param mapping
     * @param verifyFile
     * @return
     * @throws Exception
     */
    private String getMapping(String filenamePrefix, Mapping mapping, Boolean verifyFile) throws Exception {
        if (verifyFile)
            mapping.connection.verifyFile();

        File mapFile = PathManager.createUniqueFile(filenamePrefix + ".mapping.n3", outputFolder);
        PrintWriter pw = new PrintWriter(mapFile);
        mapping.printD2RQ(pw, mapping);
        pw.close();
        return outputFolder + File.separator + mapFile.getName();
    }
}
