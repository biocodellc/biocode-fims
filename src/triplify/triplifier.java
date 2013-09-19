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
    private File inputFile;
    private TabularDataReader tdr;
    private Model model;
    private String tripleOutputFile;
    private String updateOutputFile;

    public triplifier(TabularDataReader tdr, String outputFolder) throws Exception {
        this.outputFolder = outputFolder;
        this.tdr = tdr;
        inputFile = tdr.getInputFile();
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

    public File createSqlLite() throws Exception {
        PathManager pm = new PathManager();
        File processDirectory = null;

        try {
            processDirectory = pm.setDirectory(outputFolder);
        } catch (Exception e) {
            throw new Exception("unable to set output directory " + processDirectory);
        }

        // Load the SQLite JDBC driver.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new Exception("could not load the SQLite JDBC driver.");
        }

        // Create SQLite file
        String pathPrefix = processDirectory + File.separator + inputFile.getName();
        File sqlitefile = createUniqueFile(pathPrefix + ".sqlite");

        TabularDataConverter tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getAbsolutePath());
        tdc.convert(false);
        tdr.closeFile();

        return sqlitefile;
    }

    /**
     * Create new file in given folder, add incremental number to base if filename already exists.
     *
     * @param pFilename Name of the file.
     * @return The new file.
     */
    private File createUniqueFile(String pFilename) throws Exception {

        // Get just the filename
        File fileFilename = new File(pFilename);
        String fileName = fileFilename.getName();

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1)
            dotIndex = fileName.length();
        String base = fileName.substring(0, dotIndex);
        String ext = fileName.substring(dotIndex);

        File file = new File(outputFolder + fileName);
        int i = 1;
        while (file.exists())
            file = new File(outputFolder + base + "." + i++ + ext);
        return file;
    }

    /**
     * Return triples
     *
     * @param mapping
     * @return
     * @throws Exception
     */
    public void getTriples(Mapping mapping) throws Exception {
        String filenamePrefix = inputFile.getName();
        System.gc();

        // Write the model
        model = new ModelD2RQ(FileUtils.toURL(getMapping(filenamePrefix, mapping, true)),
                FileUtils.langN3, "urn:x-biscicol:");

        // Write the model as simply a Turtle file
        File tripleFile = createUniqueFile(filenamePrefix + ".triples.n3");
        FileOutputStream fos = new FileOutputStream(tripleFile);
        model.write(fos, FileUtils.langTurtle);
        fos.close();
        tripleOutputFile = outputFolder + tripleFile.getName();

        // Write out as a Sparql Update Statement
        File updateFile = createUniqueFile(filenamePrefix + ".update.n3");
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
        updateOutputFile =  outputFolder + updateFile.getName();

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

        File mapFile = createUniqueFile(filenamePrefix + ".mapping.n3");
        PrintWriter pw = new PrintWriter(mapFile);
        mapping.printD2RQ(pw, mapping);
        pw.close();
        return outputFolder + mapFile.getName();
    }
}
