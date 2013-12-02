package run;

import digester.*;
import org.apache.commons.cli.*;
import org.xml.sax.SAXException;
import reader.ReaderManager;
import reader.plugins.TabularDataReader;
import settings.*;
import triplify.triplifier;
import org.apache.commons.digester.Digester;
import org.apache.log4j.Level;
import org.apache.commons.cli.HelpFormatter;


import java.io.*;

/**
 * Core class for running fims processes.  Here you specify the input file, configuration file, output folder, and
 * a project code, which is used to specify identifier roots in the BCID (http://code.google.com/p/bcid/) system.
 * The main class is configured to run this from the command-line while the class itself can be extended to run
 * in different situations, while specifying  fimsPrinter and fimsInputter classes for a variety of styles of output and
 * input
 */
public class process {

    File configFile;
    String inputFilename;
    String outputFolder;
    String project_code;
    Boolean write_spreadsheet;
    Boolean triplify;
    Boolean upload;
    String username;
    String password;


    /**
     * run.process is the main function for validating, triplifying, & uploading fims data
     *
     * @param inputFilename     The data to run.process, usually an Excel spreadsheet
     * @param outputFolder      Where to store output files
     * @param project_code      A distinct project code for the project being loaded, used to lookup projects in the BCID system
     *                          for assigning identifier roots.
     * @param write_spreadsheet Write back a spreadsheet to test to the entire cycle
     */
    public process(
            String inputFilename,
            String outputFolder,
            String project_code,
            Boolean write_spreadsheet,
            Boolean triplify,
            Boolean upload,
            String username,
            String password) throws Exception {
        // Set class variables
        this.inputFilename = inputFilename;
        this.outputFolder = outputFolder;
        this.project_code = project_code;
        this.write_spreadsheet = write_spreadsheet;
        this.triplify = triplify;
        this.upload = upload;
        this.username = username;
        this.password = password;

        // Setup logging
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

    }

    /**
     * runAll method is designed to go through entire fims run.process: validate, triplify, upload
     */
    public void runAll() throws Exception {

        boolean validationGood = true;
        boolean triplifyGood = true;
        boolean updateGood = true;
        Validation validation = null;

        // If the user wants to upload, first authenticate to see if we have the credentials correct!
        bcidConnector bcidConnector = null;
        if (upload) {
            fimsPrinter.out.println("Authenticating ...");
            bcidConnector = new bcidConnector();
            boolean authenticationSuccess = bcidConnector.authenticate(username, password);
            if (!authenticationSuccess)
                throw new Exception("You indicated you wanted to upload data but we were unable to authenticate using the supplied credentials!");
        }

        try {
            // Initializing
            fimsPrinter.out.println("Initializing ...");
            fimsPrinter.out.println("\tinputFilename = " + inputFilename);

            configFile = new configurationFileFetcher(project_code, outputFolder).getOutputFile();
            fimsPrinter.out.println("\tconfiguration file = " + configFile.getAbsoluteFile());

            // Read the input file & create the ReaderManager and load the plugins.
            ReaderManager rm = new ReaderManager();
            rm.loadReaders();
            TabularDataReader tdr = rm.openFile(inputFilename);

            // Validation
            validation = new Validation();
            addValidationRules(new Digester(), validation);
            validation.run(tdr, project_code + "_output", outputFolder);
            validationGood = validation.printMessages();

            // Triplify if we validate
            if (triplify & validationGood) {

                Mapping mapping = new Mapping();
                addMappingRules(new Digester(), mapping);
                triplifyGood = mapping.run(validation, new triplifier(project_code + "_output", outputFolder), project_code);
                mapping.print();

                // Upload after triplifying
                if (upload & triplifyGood) {
                    Fims fims = new Fims(mapping);
                    addFimsRules(new Digester(), fims);
                    fims.run(bcidConnector, project_code);
                    fims.print();

                    if (write_spreadsheet)
                        fimsPrinter.out.println("\tspreadsheet = " + fims.write());
                }
            }

        } finally {
            if (validation != null)
                validation.close();
        }
    }


    /**
     * Process metadata component rules
     *
     * @param d
     */
    private synchronized void addFimsRules(Digester d, Fims fims) throws IOException, SAXException {
        d.push(fims);
        d.addObjectCreate("fims/metadata", Metadata.class);
        d.addSetProperties("fims/metadata");
        d.addCallMethod("fims/metadata", "addText_abstract", 0);
        d.addSetNext("fims/metadata", "addMetadata");

        d.parse(configFile);
    }

    /**
     * Process validation component rules
     *
     * @param d
     */
    private synchronized void addValidationRules(Digester d, Validation validation) throws IOException, SAXException {
        d.push(validation);

        // Create worksheet objects
        d.addObjectCreate("fims/validation/worksheet", Worksheet.class);
        d.addSetProperties("fims/validation/worksheet");
        d.addSetNext("fims/validation/worksheet", "addWorksheet");

        // Create rule objects
        d.addObjectCreate("fims/validation/worksheet/rule", Rule.class);
        d.addSetProperties("fims/validation/worksheet/rule");
        d.addSetNext("fims/validation/worksheet/rule", "addRule");
        d.addCallMethod("fims/validation/worksheet/rule/field", "addField", 0);

        // Create list objects
        d.addObjectCreate("fims/validation/lists/list", List.class);
        d.addSetProperties("fims/validation/lists/list");
        d.addSetNext("fims/validation/lists/list", "addList");
        d.addCallMethod("fims/validation/lists/list/field", "addField", 0);

        // Create column objects
        d.addObjectCreate("fims/validation/worksheet/column", Column_trash.class);
        d.addSetProperties("fims/validation/worksheet/column");
        d.addSetNext("fims/validation/worksheet/column", "addColumn");

        d.parse(configFile);
    }

    /**
     * Process mapping component rules
     *
     * @param d
     */
    private synchronized void addMappingRules(Digester d, Mapping mapping) throws IOException, SAXException {
        d.push(mapping);

        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Add attributes associated with this entity
        d.addObjectCreate("fims/mapping/entity/attribute", Attribute.class);
        d.addSetProperties("fims/mapping/entity/attribute");
        d.addSetNext("fims/mapping/entity/attribute", "addAttribute");

        // Create relation objects
        d.addObjectCreate("fims/mapping/relation", Relation.class);
        d.addSetNext("fims/mapping/relation", "addRelation");
        d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);
        d.addCallMethod("fims/mapping/relation/predicate", "addPredicate", 0);
        d.addCallMethod("fims/mapping/relation/object", "addObject", 0);

        d.parse(configFile);
    }

    /**
     * Run the program from the command-line
     *
     * @param args
     */
    public static void main(String args[]) {
        String defaultOutputDirectory = System.getProperty("user.dir") + File.separator + "tripleOutput";

        // Test configuration :
        // -d -t -u -i sampledata/Apogon***.xls

        // Direct output using the standardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        fimsPrinter.out = new standardPrinter();
        // Set the input format
        fimsInputter.in = new standardInputter();

        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // The project code corresponds to a project recognized by BCID
        String project_code = "";
        // The configuration template
        //String configuration = "";
        // The input file
        String input_file = "";
        // The directory that we write all our files to
        String output_directory = "";
        // Write spreadsheet content back to a spreadsheet file, for testing
        Boolean write_spreadsheet = false;
        Boolean triplify = false;
        Boolean upload = false;


        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("p", "project_code", true, "Project code.  You will need to obtain a project code before " +
                "loading data, or use the demo_mode.");
        options.addOption("o", "output_directory", true, "Output Directory");
        options.addOption("i", "input_file", true, "Input Spreadsheet");
        options.addOption("d", "demo_mode", false, "Demonstration mode.  Do not need to specify project_code, " +
                "configuration, or output_directory.  You still need to specify an input file.");
        options.addOption("t", "triplify", false, "Triplify");
        options.addOption("u", "upload", false, "Upload");
        options.addOption("w", "write_spreadsheet", false, "Write back an excel spreadsheet from the triplestore.  " +
                "This option is useful for testing output from flatfile -> RDF -> flatfile but not necessary.");
        // Create the commands parser and parse the command line arguments.
        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            fimsPrinter.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            fimsPrinter.out.println("Error: " + e.getMessage());
            return;
        }

        // If help was requested, print the help message and exit.
        if (cl.hasOption("h") ||
                (cl.hasOption("d") && !cl.hasOption("i")) ||
                (!cl.hasOption("d") && (!cl.hasOption("p") || !cl.hasOption("i")))) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        if (cl.hasOption("d")) {
            project_code = "DEMOH";
            //configuration = "sampledata/indoPacificConfiguration.xml";
            //input_file = "sampledata/Apogon_indoPacificTemplate_v3.xlsx";
            output_directory = defaultOutputDirectory;
            triplify = true;
            upload = true;
        }
        if (cl.hasOption("i"))
            input_file = cl.getOptionValue("i");
        if (cl.hasOption("o"))
            output_directory = cl.getOptionValue("o");
        if (cl.hasOption("p"))
            project_code = cl.getOptionValue("p");
        if (cl.hasOption("w"))
            write_spreadsheet = true;
        if (cl.hasOption("t"))
            triplify = true;
        if (cl.hasOption("u"))
            upload = true;
        if (!cl.hasOption("o") && !cl.hasOption("d")) {
            fimsPrinter.out.println("Using default output directory " + defaultOutputDirectory);
            output_directory = defaultOutputDirectory;
        }
        // Need to choose "triplify" if you choose "update"
        if (cl.hasOption("u") && !cl.hasOption("t")) {
            fimsPrinter.out.println("Must specify 'triplify' option if you choose to 'upload'");
            return;
        }

        // Check that output directory is writable
        if (!new File(output_directory).canWrite()) {
            fimsPrinter.out.println("Unable to write to output directory " + output_directory);
            return;
        }

        // TODO: create username/password combinations for upload script
        // Run the processor
        process p = null;
        try {
            p = new process(
                    input_file,
                    output_directory,
                    project_code,
                    write_spreadsheet,
                    triplify,
                    upload,
                    "demo",
                    "demo"
            );
        } catch (Exception e) {
            fimsPrinter.out.println("\nError: " + e.getMessage());
            System.exit(-1);
        }

        try {
            p.runAll();
        } catch (Exception e) {
            fimsPrinter.out.println("\nError: " + e.getMessage());
            //e.printStackTrace();
            System.exit(-1);
        }
    }

}
