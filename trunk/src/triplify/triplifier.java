package triplify;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import digester.Mapping;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import settings.PathManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: jdeck
 * Date: 9/10/13
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
public class triplifier {

       private String outputFolder;
    private String inputFilename;

    public triplifier(String inputFilename, String outputFolder) {
        this.outputFolder = outputFolder;
        this.inputFilename = inputFilename;
    }

    public  File createSqlLite() throws Exception {
        // Create the ReaderManager and load the plugins.
        ReaderManager rm = new ReaderManager();
        try {
            rm.loadReaders();
        } catch (FileNotFoundException e) {
            throw new Exception("could not load data reader plugins.");
        }

        // Load the SQLite JDBC driver.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new Exception("could not load the SQLite JDBC driver.");
        }

        PathManager pm = new PathManager();
        File processDirectory = null;
        File inputFile = null;

        try {
            inputFile = pm.setFile(inputFilename);
        } catch (Exception e) {
            throw new Exception("unable to read " + inputFilename);
        }

        TabularDataReader tdr = rm.openFile(inputFile.getAbsolutePath());
        if (tdr == null) {
            throw new Exception("unable to open input file " + inputFile.getAbsolutePath());
        }

        try {
            processDirectory = pm.setDirectory(outputFolder);
        } catch (Exception e) {
            throw new Exception("unable to set output directory " + processDirectory);
        }

        // Create SQLite file
        //System.out.println("Beginning SQlite creation & connection");
        String pathPrefix = processDirectory + File.separator + inputFile.getName();
        File sqlitefile = createUniqueFile(pathPrefix + ".sqlite");
        //sqlitefile = new File(pathPrefix + "_" + ".sqlite");

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
    private  File createUniqueFile(String pFilename) throws Exception {

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

    public  String getTriples(Mapping mapping) throws Exception {
        String filenamePrefix = new File(inputFilename).getName();
        System.gc();

        Model model = new ModelD2RQ(FileUtils.toURL(getMapping(filenamePrefix, mapping, true)),
                FileUtils.langN3, "urn:x-biscicol:");
        File tripleFile = createUniqueFile(filenamePrefix + ".triples.n3");
        FileOutputStream fos = new FileOutputStream(tripleFile);
        model.write(fos, FileUtils.langNTriple);
        fos.close();
        return outputFolder + tripleFile.getName();
    }

    private  String getMapping(String filenamePrefix, Mapping mapping, Boolean verifyFile) throws Exception {
        if (verifyFile)
            mapping.connection.verifyFile();

        File mapFile = createUniqueFile(filenamePrefix + ".mapping.n3");
        PrintWriter pw = new PrintWriter(mapFile);
        mapping.printD2RQ(pw);
        pw.close();
        return outputFolder + mapFile.getName();
    }
}
