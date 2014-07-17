package run;

import digester.*;
import digester.List;
import fims.fimsFilterCondition;
import fims.fimsQueryBuilder;
import org.apache.commons.cli.*;
import org.apache.commons.digester3.Digester;
import org.apache.log4j.Level;
import org.xml.sax.SAXException;
import reader.ReaderManager;
import reader.plugins.TabularDataReader;
import settings.*;
import triplify.triplifier;
import utils.SettingsManager;
import utils.stringGenerator;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Core class for running fims processes.  Here you specify the input file, configuration file, output folder, and
 * a expedition code, which is used to specify identifier roots in the BCID (http://code.google.com/p/bcid/) system.
 * The main class is configured to run this from the command-line while the class itself can be extended to run
 * in different situations, while specifying  fimsPrinter and fimsInputter classes for a variety of styles of output
 * and
 * input
 */
public class process {

    public File configFile;
    Mapping mapping;
    String outputFolder;
    String outputPrefix;
    bcidConnector connector;
    private processController processController;


    /**
     * Setup class variables for processing FIMS data.
     *
     * @param inputFilename The data to run.process, usually an Excel spreadsheet
     * @param outputFolder  Where to store output files
     */
    public process(
            String inputFilename,
            String outputFolder,

            bcidConnector connector,
            processController processController) throws FIMSException {

        // Setup logging
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // Update the processController Settings
        this.processController = processController;

        processController.setInputFilename(inputFilename);
        this.outputFolder = outputFolder;

        // Control the file outputPrefix... set them here to expedition codes.
        this.outputPrefix = processController.getExpeditionCode() + "_output";
        this.connector = connector;

        // Read the Configuration File
        try {
            configFile = new configurationFileFetcher(processController.getProject_id(), outputFolder, false).getOutputFile();
        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException("Unable to obtain configuration file from server... <br>" +
                    "Please check that your project code is valid.<br>");
        }


        // Parse the Mapping object (this object is used extensively in downstream functions!)
        try {
            mapping = new Mapping();
            addMappingRules(new Digester(), mapping);
        } catch (Exception e) {
            throw new FIMSException("Problem reading mapping in configuration file", e);
        }
    }

    /**
     * Always use this method to fetch the process Controller from the process class as it has the current status
     *
     * @return
     */
    public processController getProcessController() {
        return processController;
    }

    /**
     * A constructor for when we're running queries or reading template files
     *
     * @param outputFolder
     * @param configFile
     *
     * @throws settings.FIMSException
     */
    public process(
            String outputFolder,
            File configFile
    ) throws FIMSException {
        this.outputFolder = outputFolder;
        this.configFile = configFile;
        this.outputPrefix = "output";
    }

    /**
     * Check if this is an NMNH project
     *
     * @return
     *
     * @throws Exception
     */
    public Boolean isNMNHProject() throws Exception {
        Fims fims = new Fims(mapping);
        addFimsRules(new Digester(), fims);

        String nmnh = fims.getMetadata().getNmnh();
        if (nmnh == null || !nmnh.equalsIgnoreCase("true"))
            return false;
        else
            return true;
    }

    public static bcidConnector createConnection(String username, String password) throws FIMSException {
        bcidConnector bcidConnector = new bcidConnector();

        // Authenticate all the time, even if not uploading
        fimsPrinter.out.println("Authenticating ...");

        boolean authenticationSuccess = false;
        try {
            authenticationSuccess = bcidConnector.authenticate(username, password);
        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException("A system error occurred attempting to authenticate " + username + ". Is authentication server running?");
        }

        if (!authenticationSuccess) {
            String message = "Unable to authenticate " + username + " using the supplied credentials!";
            fimsInputter.in.haltOperation(message);
            return null;
        }





        return bcidConnector;
    }

    /**
     * Check the status of this expedition
     *
     * @throws FIMSException
     */
    public void runExpeditionCheck() throws FIMSException {
        try {
            Boolean checkExpedition = connector.checkExpedition(processController);
            processController.setExpeditionCreateRequired(checkExpedition);
            if (!checkExpedition) {
                processController.setExpeditionAssignedToUserAndExists(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException(e.getMessage(), e);
        }
    }

    /**
     * Create an expedition
     *
     * @throws FIMSException
     */
    public void runExpeditionCreate() throws FIMSException {
        try {
            if (connector.checkExpedition(processController))
            connector.createExpedition(processController, mapping);
            processController.setExpeditionCreateRequired(false);
            processController.setExpeditionAssignedToUserAndExists(true);
        } catch (Exception e) {
            throw new FIMSException(e.getMessage(), e);
        }
    }

    /**
     * runAll method is designed to go through the FIMS process for a local application.  The REST services
     * would handle user input/output differently
     */
    public void runAllLocally(Boolean triplifier, Boolean upload) throws FIMSException {
        // Set whether this is an NMNH project or not
        try {
            Fims fims = new Fims(mapping);
            addFimsRules(new Digester(), fims);
            processController.setNMNH(fims.getMetadata().getNmnh());
            if (processController.getNMNH()) {
                System.out.println("\tthis is an NMNH designated project");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException(e.getMessage(), e);
        }

        // Validation Step
        runValidation();

        // Run the validation step
        if (!processController.isValidated() && processController.getHasWarnings()) {
            String message = "\tWarnings found on " + mapping.getDefaultSheetName() + " worksheet.\n" + processController.getWarningsSB().toString();
            Boolean continueOperation = fimsInputter.in.continueOperation(message);

            if (!continueOperation)
                return;
            processController.setClearedOfWarnings(true);
            processController.setValidated(true);
        }

        // We only need to check on assigning Expedition if the user wants to triplify or upload data
        if (triplifier || upload) {

            // Expedition Check Step
            if (!processController.isExpeditionAssignedToUserAndExists())
                runExpeditionCheck();

            // if an expedition creation is required, get feedback from user
            if (processController.isExpeditionCreateRequired()) {
                String message = "\nThe dataset code \"" + processController.getExpeditionCode() + "\" does not exist.  " +
                        "Do you wish to create it now?" +
                        "\nIf you choose to continue, your data will be associated with this new dataset code.";
                Boolean continueOperation = fimsInputter.in.continueOperation(message);
                if (!continueOperation)
                    return;
                else
                    runExpeditionCreate();
            }

            // Triplify OR Upload -- not ever both
            if (triplifier)
                runTriplifier();
            else if (upload)
                runUpload();
        }
    }

    public void runValidation() throws FIMSException {
        Validation validation = null;

        try {
            // Create the tabulardataReader for reading the input file
            ReaderManager rm = new ReaderManager();
            TabularDataReader tdr = null;
            rm.loadReaders();

            tdr = rm.openFile(processController.getInputFilename(), mapping.getDefaultSheetName(), outputFolder);

            // Load validation rules
            validation = new Validation();
            addValidationRules(new Digester(), validation);

            // Run the validation
            validation.run(tdr, outputPrefix, outputFolder, mapping);

            processController = validation.printMessages(processController);
            processController.setValidation(validation);

        } catch (Exception e) {
            e.printStackTrace();
            throw new FIMSException(e.getMessage(), e);
        }
    }

    /**
     * Run the triplification engine
     *
     * @return
     *
     * @throws FIMSException
     */
    public boolean runTriplifier() throws FIMSException {
        // If Validation passed, we can go ahead and triplify
        Boolean triplifyGood = false;
        if (processController.isValidated()) {
            try {
                triplifyGood = mapping.run(
                        connector,
                        new triplifier(outputPrefix, outputFolder),
                        processController
                );
            } catch (Exception e) {
                //e.printStackTrace();
                throw new FIMSException(e.getMessage(), e);
            }

            mapping.print();
        }

        return triplifyGood;
    }

    public void runUpload() throws FIMSException {
        // If the triplification was good and the user wants to upload, then proceed
        if (processController.isReadyToUpload() &&
                runTriplifier()) {
            Fims fims = new Fims(mapping);
            try {
                addFimsRules(new Digester(), fims);
                fims.run(connector, processController);
                String results = fims.results();
                processController.appendStatus("<br>" + results);
                fimsPrinter.out.println(results);
            } catch (Exception e) {
                throw new FIMSException(e.getMessage(), e);
            }
        }
    }


    /**
     * Run a query from the command-line. This is not meant to be a full-featured query service but a simple way of
     * fetching results
     *
     * @throws settings.FIMSException
     */
    public String query(String graphs, String format, ArrayList<fimsFilterCondition> filter) throws FIMSException {
        try {
            // Build the Query Object by passing this object and an array of graph objects, separated by commas
            fimsQueryBuilder q = new fimsQueryBuilder(this, graphs.split(","), outputFolder);
            // Add our filter conditions
            q.addFilter(filter);
            // Run the query, passing in a format and returning the location of the output file
            return q.run(format);
        } catch (Exception e) {
            throw new FIMSException(e.getMessage(), e);
        }
    }

    /**
     * Process metadata component rules
     *
     * @param d
     */
    public synchronized void addFimsRules(Digester d, Fims fims) throws IOException, SAXException {
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
    public synchronized void addValidationRules(Digester d, Validation validation) throws IOException, SAXException {
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
    public synchronized void addMappingRules(Digester d, Mapping mapping) throws IOException, SAXException {
        d.push(mapping);

        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Add attributes associated with this entity
        d.addObjectCreate("fims/mapping/entity/attribute", Attribute.class);
        d.addSetProperties("fims/mapping/entity/attribute");
        d.addCallMethod("fims/mapping/entity/attribute", "addDefinition", 0);
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
        //processController processController = new processController();
        String defaultOutputDirectory = System.getProperty("user.dir") + File.separator + "tripleOutput";
        String username = "";
        String password = "";
        Integer project_id = 0;
        //System.out.print(defaultOutputDirectory);

        // Test configuration :
        // -d -t -u -i sampledata/Apogon***.xls

        // Direct output using the standardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        fimsPrinter.out = new standardPrinter();


        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // The expedition code corresponds to a expedition recognized by BCID
        String dataset_code = "";
        // The configuration template
        //String configuration = "";
        // The input file
        String input_file = "";
        // The directory that we write all our files to
        String output_directory = "tripleOutput";
        // Write spreadsheet content back to a spreadsheet file, for testing
        Boolean triplify = false;
        Boolean upload = false;


        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("q", "query", true, "Run a query and pass in graph UUIDs to look at for this query -- Use this along with options C and S");
        options.addOption("f", "format", true, "excel|html|json  specifying the return format for the query");
        options.addOption("F", "filter", true, "Filter results based on a keyword search");

        options.addOption("e", "dataset_code", true, "Dataset code.  You will need to obtain a data code before " +
                "loading data");
        options.addOption("o", "output_directory", true, "Output Directory");
        options.addOption("i", "input_file", true, "Input Spreadsheet");
        options.addOption("p", "project_id", true, "Project Identifier.  A numeric integer corresponding to your project");

        options.addOption("t", "triplify", false, "Triplify only (upload process triplifies)");
        options.addOption("u", "upload", false, "Upload");

        options.addOption("U", "username", true, "Username (for uploading data)");
        options.addOption("P", "password", true, "Password (for uploading data)");
        options.addOption("y", "yes", false, "Answer 'y' to all questions");


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

        // Set the input format
        if (cl.hasOption("y")) {
            fimsInputter.in = new forceInputter();
        }   else {
            fimsInputter.in = new standardInputter();
        }


        if (cl.hasOption("U")) {
            username = cl.getOptionValue("U");
        }
        if (cl.hasOption("P")) {
            password = cl.getOptionValue("P");
        }
        if (cl.hasOption("u") && (username.equals("") || password.equals(""))) {
            fimsPrinter.out.println("Must specify a valid username or password for uploading data!");
            return;
        }
        // query option must also have project_id option
        if (cl.hasOption("q")) {
            if (!cl.hasOption("p")) {
                helpf.printHelp("fims ", options, true);
                return;
            }

        }
        // help options
        else if (cl.hasOption("h")) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // Nop options returns help message
        if (cl.getOptions().length < 1) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        if (cl.hasOption("p")) {
            try {
                project_id = new Integer(cl.getOptionValue("p"));
            } catch (Exception e) {
                fimsPrinter.out.println("Bad option for project_id");
                helpf.printHelp("fims ", options, true);
                return;
            }
        }
        if (cl.hasOption("u") && project_id < 1) {
            fimsPrinter.out.println("Must specify a valid project_id when uploading data");
            return;
        }
        if (cl.hasOption("i"))
            input_file = cl.getOptionValue("i");
        if (cl.hasOption("o"))
            output_directory = cl.getOptionValue("o");
        if (cl.hasOption("e"))
            dataset_code = cl.getOptionValue("e");

        if (cl.hasOption("t"))
            triplify = true;
        if (cl.hasOption("u"))
            upload = true;
        if (!cl.hasOption("o")) {
            fimsPrinter.out.println("Using default output directory " + defaultOutputDirectory);
            output_directory = defaultOutputDirectory;
        }

        // Check that output directory is writable
        try {
            if (!new File(output_directory).canWrite()) {
                fimsPrinter.out.println("Unable to write to output directory " + output_directory);
                return;
            }
        } catch (Exception e) {
            fimsPrinter.out.println("Unable to write to output directory " + output_directory);
            return;
        }

        // Run the command
        try {
            /*
            Run a query
             */
            if (cl.hasOption("q")) {

                File file = new configurationFileFetcher(project_id, output_directory, true).getOutputFile();

                process p = new process(
                        output_directory,
                        file
                );

                //p.query(cl.getOptionValue("q"), cl.getOptionValue("f"), cl.getOptionValue("F"));
                // TODO: construct filter statements from arguments passed in on command-line
                System.out.println(p.query(cl.getOptionValue("q"), cl.getOptionValue("f"), null));

            }
            /*
           Run the validator
            */
            else {

                // Create the appropritate connection string depending on options
                bcidConnector connector = null;
                if (triplify || upload) {
                    if (username == null || username.equals("") || password == null || password.equals("")) {
                        fimsPrinter.out.println("Need valid username / password for uploading or triplifying");
                        helpf.printHelp("fims ", options, true);
                        return;
                    } else {
                        connector = createConnection(username, password);
                        if (!cl.hasOption("e")) {
                            fimsPrinter.out.println("Need to enter a dataset code before triplifying or uploading");
                            helpf.printHelp("fims ", options, true);
                            return;
                        }
                    }
                } else {
                    connector = new bcidConnector();
                }


                // Now run the process
                if (connector != null) {
                    process p = new process(
                            input_file,
                            output_directory,
                            connector,
                            new processController(project_id, dataset_code)
                    );

                    fimsPrinter.out.println("Initializing ...");
                    fimsPrinter.out.println("\tinputFilename = " + input_file);

                    // Run the processor
                    p.runAllLocally(triplify, upload);
                }

            }
        } catch (Exception e) {
            fimsPrinter.out.println("\nError: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
