import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileUtils;
import de.fuberlin.wiwiss.d2rq.jena.ModelD2RQ;
import digester.*;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Level;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import settings.PathManager;
import java.io.*;
import settings.Connection;


/**
 * Test the Apache Digester
 */
public class digesterTester {

    static String configFilename = "sampledata/configuration.xml";
    static String inputFilename = "sampledata/biocode_template.xls";
    static String outputFolder = System.getProperty("user.dir") + File.separator + "tripleOutput" + File.separator;

    public static Mapping mapping;


    public static void main(String args[]) {
        // Setup logging
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // Run validation
        //Validation validation = new Validation();
        //d.push(validation);
        //addValidationRules(d);

        // Digester setup
        Digester d = new Digester();

        // Create a connection to a SQL Lite Instance
        Connection connection = null;
        try {
            connection = new Connection(createSqlLite(inputFilename));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }

        // Create Mapping Digester Instance
        mapping = new Mapping(connection);
        d.push(mapping);
        addMappingRules(d);

        // Digester Parsing configuration File
        try {
            File srcfile = new File(configFilename);
            d.parse(srcfile);
        } catch (java.io.IOException ioe) {
            System.out.println("Error reading input file:" + ioe.getMessage());
            System.exit(-1);
        } catch (org.xml.sax.SAXException se) {
            System.out.println("Error parsing input file:" + se.getMessage());
            System.exit(-1);
        }

        // Create Mapping File
        /*
        PrintWriter pw = null;
        try {
            pw = createMapping();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        } */

       /* // Print D2RQ Mapping File
        try {
            mapping.printD2RQ(pw);
            pw.close();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }     */

        // Triplify
        System.out.println("Beginning triple file creation");

        String results = null;
        try {
            results = getTriples(new File(inputFilename).getName(), mapping);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("Done! see, " + results);
    }

    /**
     * Create new file in given folder, add incremental number to base if filename already exists.
     *
     * @param fileName Name of the file.
     * @return The new file.
     */
    public static File createUniqueFile(String fileName) throws Exception {

        // Get just the filename
        File fileFilename = new File(fileName);
        fileName = fileFilename.getName();

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
     * Process validation component rules
     *
     * @param d
     */
    private static void addValidationRules(Digester d) {
        // Create worksheet objects
        d.addObjectCreate("fims/validation/worksheet", Worksheet.class);
        d.addSetProperties("fims/validation/worksheet");
        d.addSetNext("fims/validation/worksheet", "addWorksheet");

        // Create rule objects
        d.addObjectCreate("fims/validation/worksheet/rule", Rule.class);
        d.addSetProperties("fims/validation/worksheet/rule");
        d.addSetNext("fims/validation/worksheet/rule", "addRule");
        d.addCallMethod("fims/validation/worksheet/rule/field", "addField", 0);
    }

    /**
     * Process mapping component rules
     *
     * @param d
     */
    private static void addMappingRules(Digester d) {
        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Create relation objects
        d.addObjectCreate("fims/mapping/relation", Relation.class);
        d.addSetNext("fims/mapping/relation", "addRelation");
        //d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);

        //d.addObjectCreate("fims/mapping/relation/subject", Entity.class);
        //d.addSetProperties("fims/mapping/relation/subject");
        d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);
        d.addCallMethod("fims/mapping/relation/predicate", "addPredicate", 0);


        //d.addObjectCreate("fims/mapping/relation/object", Entity.class);
        //d.addSetProperties("fims/mapping/relation/object");
        d.addCallMethod("fims/mapping/relation/object", "addObject", 0);

    }

    private static File createSqlLite(String inputFilePath) throws Exception {
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
            inputFile = pm.setFile(inputFilePath);
        } catch (Exception e) {
            throw new Exception("unable to read " + inputFilePath);
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
        System.out.println("Beginning SQlite creation & connection");
        String pathPrefix = processDirectory + File.separator + inputFile.getName();
        File sqlitefile = createUniqueFile(pathPrefix + ".sqlite");
        //sqlitefile = new File(pathPrefix + "_" + ".sqlite");

        TabularDataConverter tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getAbsolutePath());
        tdc.convert(false);
        tdr.closeFile();

        return sqlitefile;
    }

    private static PrintWriter createMapping() throws Exception {
        // Create mapping file
        System.out.println("Beginning Mapping File Creation");
        File mapFile = null;
        try {
            mapFile = createUniqueFile(inputFilename + ".mapping.n3");
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(mapFile);
        } catch (FileNotFoundException e) {
            throw new Exception(e.getMessage());
        }
        return pw;
    }

    private static String getTriples(String filenamePrefix, Mapping mapping) throws Exception {
        System.gc();

        Model model;

        model = new ModelD2RQ(FileUtils.toURL(getMapping(filenamePrefix, mapping, true)),
                FileUtils.langN3, null);

        File tripleFile = createUniqueFile(filenamePrefix + ".triples.n3");
        FileOutputStream fos = new FileOutputStream(tripleFile);
        model.write(fos, FileUtils.langNTriple);
        fos.close();
        return outputFolder + tripleFile.getName();
    }

    private static String getMapping(String filenamePrefix, Mapping mapping, Boolean verifyFile) throws Exception {
        if (verifyFile)
            mapping.connection.verifyFile();

        File mapFile = createUniqueFile(filenamePrefix + ".mapping.n3");
        PrintWriter pw = new PrintWriter(mapFile);
        mapping.printD2RQ(pw);
        pw.close();
        return outputFolder + mapFile.getName();
    }
}
