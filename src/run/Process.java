package run;

import auth.Authenticator;
import bcid.BcidMinter;
import bcid.Database;
import bcid.ExpeditionMinter;
import fims.FimsQueryBuilder;
import biocode.fims.fimsExceptions.FimsException;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.UnauthorizedRequestException;
import digester.*;
import fims.FimsFilterCondition;
import org.apache.commons.cli.*;
import org.apache.commons.digester3.Digester;
import org.apache.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import reader.ReaderManager;
import reader.plugins.TabularDataReader;
import settings.*;
import triplify.Triplifier;
import utils.SettingsManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.sql.Connection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Core class for running fims processes.  Here you specify the input file, configuration file, output folder, and
 * a expedition code, which is used to specify Bcid roots in the BCID (http://code.google.com/p/bcid/) system.
 * The main class is configured to run this from the command-line while the class itself can be extended to run
 * in different situations, while specifying  fimsPrinter and FimsInputter classes for a variety of styles of output
 * and
 * input
 */
public class Process {

    public File configFile;

    Mapping mapping;

    String outputFolder;
    String outputPrefix;
    private ProcessController processController;
    private static Logger logger = LoggerFactory.getLogger(Process.class);
    protected int projectId;
    private Database db;
    protected Connection conn;

    /**
     * Setup class variables for processing FIMS data.
     *
     * @param inputFilename The data to run.process, usually an Excel spreadsheet
     * @param outputFolder  Where to store output files
     */
    public Process(
            String inputFilename,
            String outputFolder,
            ProcessController processController) {

        // Setup
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // Update the processController Settings
        this.processController = processController;

        processController.setInputFilename(inputFilename);
        this.outputFolder = outputFolder;

        // Control the file outputPrefix... set them here to expedition codes.
        this.outputPrefix = processController.getExpeditionCode() + "_output";

        // Read the Configuration File
        configFile = new ConfigurationFileFetcher(processController.getProjectId(), outputFolder, false).getOutputFile();


        // Parse the Mapping object (this object is used extensively in downstream functions!)
        mapping = new Mapping();
        addMappingRules(new Digester(), mapping);

        // Initialize Database
        this.db = new Database();
        this.conn = db.getConn();
    }

    /**
     * Setup class variables for processing FIMS data.
     *
     * @param inputFilename The data to run.process, usually an Excel spreadsheet
     * @param outputFolder  Where to store output files
     */
    public Process(
            String inputFilename,
            String outputFolder,
            ProcessController processController,
            File configFile) {

        // Setup logging
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        // Update the processController Settings
        this.processController = processController;

        processController.setInputFilename(inputFilename);
        this.outputFolder = outputFolder;

        // Control the file outputPrefix... set them here to expedition codes.
        this.outputPrefix = processController.getExpeditionCode() + "_output";

        // Read the Configuration File
        this.configFile = configFile;


        // Parse the Mapping object (this object is used extensively in downstream functions!)
        mapping = new Mapping();
        addMappingRules(new Digester(), mapping);
    }

    /**
     * A constructor for when we're running queries or reading template files
     *
     * @param outputFolder
     * @param configFile
     */
    public Process(
            int projectId,
            String outputFolder,
            File configFile) {
        this.projectId = projectId;
        this.outputFolder = outputFolder;
        this.configFile = configFile;
        this.outputPrefix = "output";
    }

    /**
     * a constructor for DeepRoots lookupPrefix method
     */
    public Process() {}

    /**
     * Always use this method to fetch the process Controller from the process class as it has the current status
     *
     * @return
     */
    public ProcessController getProcessController() {
        return processController;
    }

    public Mapping getMapping() {
        return mapping;
    }

    public int getProject_id() {
        return projectId;
    }

    /**
     * Check if this is a NMNH project
     *
     * @return
     */
    public Boolean isNMNHProject() {
        Fims fims = new Fims(mapping, null);
        addFimsRules(new Digester(), fims);

        String nmnh = fims.getMetadata().getNmnh();
        if (nmnh == null || !nmnh.equalsIgnoreCase("true"))
            return false;
        else
            return true;
    }

    /**
     * Check the status of this expedition
     */
    public void runExpeditionCheck(boolean ignore_user) {
        Boolean checkExpedition = checkExpedition(ignore_user);
        processController.setExpeditionCreateRequired(checkExpedition);
        if (!checkExpedition) {
            processController.setExpeditionAssignedToUserAndExists(true);
        }
    }

    /**
     * validateExpedition ensures that this user is associated with this expedition and that the expedition code is
     * unique within
     * a particular project
     *
     * @return true if we need to insert a new expedition
     */

    public boolean checkExpedition(Boolean ignoreUser) {
        // if the expedition code isn't set we can just immediately return true which is
        if (processController.getExpeditionCode() == null || processController.getExpeditionCode() == "") {
            return true;
        }

        if (processController.getUserId() == null) {
            throw new UnauthorizedRequestException("You must be logged in to check an expedition status");
        }

        ExpeditionMinter expeditionMinter = new ExpeditionMinter();

        String response = expeditionMinter.validateExpedition(processController.getExpeditionCode(), processController.getProjectId(),
                ignoreUser, processController.getUserId());

        JSONObject r = (JSONObject) JSONValue.parse(response);
       if (r.containsKey("update")) {
            return false;
       } else if (r.containsKey("insert")) {
            return true;
            /*String message = "\nThe expedition code \"" + processController.getExpeditionCode() + "\" does not exist.  " +
                    "Do you wish to create it now?" +
                    "\nIf you choose to continue, your data will be associated with this new expedition code.";
            Boolean continueOperation = FimsInputter.in.continueOperation(message);
            return continueOperation;
            */
        } else {
            return false;
        }
    }

    /**
     * Create an expedition
     */
    public void runExpeditionCreate() {
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        Boolean ignoreUser = Boolean.parseBoolean(sm.retrieveValue("ignore_user"));

        if (checkExpedition(ignoreUser)) {
            System.out.println("Creating expedition " + processController.getExpeditionCode() + "...");
            createExpedition(processController, mapping);
        }
        processController.setExpeditionCreateRequired(false);
        processController.setExpeditionAssignedToUserAndExists(true);
    }

    private boolean createExpedition(ProcessController processController, Mapping mapping) {
        String status = "\tCreating expedition " + processController.getExpeditionCode() + " ... this is a one time process " +
                "before loading each spreadsheet and may take a minute...\n";
        processController.appendStatus(status);
        FimsPrinter.out.println(status);
        String expeditionTitle = processController.getExpeditionCode() + " spreadsheet";
        if (processController.getAccessionNumber() != null) {
            expeditionTitle += " (accession " + processController.getAccessionNumber() + ")";
        }
        ExpeditionMinter expedition = new ExpeditionMinter();
        try {
            // Mint a expedition
            expedition.mint(
                    processController.getExpeditionCode(),
                    expeditionTitle,
                    processController.getUserId(),
                    projectId,
                    processController.getPublicStatus()
            );
        } catch (FimsException e) {
            expedition.close();
            throw new BadRequestException(e.getMessage());
        }
        //fimsPrinter.out.println("\t" + output);

        // Loop the mapping file and create a BCID for every entity that we specified there!
        if (mapping != null) {
            LinkedList<Entity> entities = mapping.getEntities();
            Iterator it = entities.iterator();
            while (it.hasNext()) {
                Entity entity = (Entity) it.next();

                String s = "\t\tCreating bcid root for " + entity.getConceptAlias() + " and resource type = " + entity.getConceptURI() + "\n";
                processController.appendStatus(s);
                FimsPrinter.out.println(s);
                // Create the entity BCID

                // Mint the data group
                BcidMinter bcidMinter = new BcidMinter(true);

                String identifier = bcidMinter.createEntityBcid(processController.getUserId(), entity.getConceptAlias(),
                        "", null, null, false);
                bcidMinter.close();
                // Associate this Bcid with this expedition
                expedition.attachReferenceToExpedition(processController.getExpeditionCode(), identifier, processController.getProjectId());

            }
        }
        expedition.close();

        return true;


    }

    /**
     * runAll method is designed to go through the FIMS process for a local application.  The REST services
     * would handle user input/output differently
     *
     * @param triplifier
     * @param upload
     * @param expeditionCheck -- only set to FALSE for testing and debugging usually, or local triplify usage.
     */
    public void runAllLocally(Boolean triplifier, Boolean upload, Boolean expeditionCheck, Boolean forceAll) {
        // Set whether this is a NMNH project or not
        Fims fims = new Fims(mapping, null);
        addFimsRules(new Digester(), fims);
        processController.setNMNH(fims.getMetadata().getNmnh());
        if (processController.getNMNH()) {
            System.out.println("\tthis is a NMNH designated project");
        }

        // Validation Step
        runValidation();

        // If there is errors, tell the user and stop the operation
        if (processController.getHasErrors()) {
            FimsPrinter.out.println(processController.getCommandLineSB().toString());
            return;
        }
        // Run the validation step
        if (!processController.isValidated() && processController.getHasWarnings()) {
            Boolean continueOperation = false;
            if (forceAll) {
                continueOperation = true;
            } else {
                String message = "\tWarnings found on " + mapping.getDefaultSheetName() + " worksheet.\n" + processController.getCommandLineSB().toString();
                // In LOCAL version convert HTML tags to readable Text
                // the FimsInputter isn't working correctly, just using the StandardInputter for now
                //Boolean continueOperation = FimsInputter.in.continueOperation(message);
                continueOperation = new StandardInputter().continueOperation(message);
            }
            if (!continueOperation)
                return;
            processController.setClearedOfWarnings(true);
            processController.setValidated(true);
        }

        // We only need to check on assigning Expedition if the user wants to triplify or upload data
        if (triplifier || upload) {

            if (expeditionCheck) {
                // make sure that the user is logged in and set the userId in the process
                if (processController.getUserId() == null) {
                    throw new UnauthorizedRequestException("You must be logged in to continue");
                }
                // Expedition Check Step
                if (!processController.isExpeditionAssignedToUserAndExists())
                    runExpeditionCheck(false);
                // if an expedition creation is required, get feedback from user
                if (processController.isExpeditionCreateRequired()) {
                    if (forceAll) {
                        runExpeditionCreate();
                    } else {
                        String message = "\nThe expedition code \"" + processController.getExpeditionCode() + "\" does not exist.  " +
                                "Do you wish to create it now?" +
                                "\nIf you choose to continue, your data will be associated with this new expedition code.";
                        Boolean continueOperation = FimsInputter.in.continueOperation(message);
                        if (!continueOperation)
                            return;
                        else
                            runExpeditionCreate();
                    }

                }

                // Triplify OR Upload -- not ever both
                if (triplifier)
                    runTriplifier();
                else if (upload)
                    runUpload();
                // If we don't run the expedition check then we DO NOT assign any ARK roots or special expedition information
                // In other, words, this is typically used for local debug & test modes
            } else {
                Triplifier t = new Triplifier("test", this.outputFolder);
                mapping.run(t, processController, false);
                mapping.print();
            }


        }
    }

    public void runValidation() {
        Validation validation = null;

        // Create the tabulardataReader for reading the input file
        ReaderManager rm = new ReaderManager();
        TabularDataReader tdr = null;
        rm.loadReaders();

        tdr = rm.openFile(processController.getInputFilename(), mapping.getDefaultSheetName(), outputFolder);

        if (tdr == null) {
            processController.setHasErrors(true);
            processController.appendStatus("<br>Unable to open the file you attempted to upload.<br>");
            processController.setCommandLineSB(new StringBuilder("Unable to open the file you attempted to upload."));
            return;
        }
        // Load validation rules
        validation = new Validation();
        addValidationRules(new Digester(), validation);

        // Run the validation
        validation.run(tdr, outputPrefix, outputFolder, mapping);

        //
        processController = validation.printMessages(processController);
        processController.setValidation(validation);
        processController.setDefaultSheetUniqueKey(mapping.getDefaultSheetUniqueKey());
    }

    /**
     * Run the triplification engine
     *
     * @return
     */
    public boolean runTriplifier() {
        // If Validation passed, we can go ahead and triplify
        Boolean triplifyGood = false;
        if (processController.isValidated()) {
            triplifyGood = mapping.run(
                    new Triplifier(outputPrefix, outputFolder),
                    processController,
                    true
            );

            mapping.print();
        }

        return triplifyGood;
    }

    public void runUpload() {
        // If the triplification was good and the user wants to upload, then proceed
        if (processController.isReadyToUpload() &&
                runTriplifier()) {
            Fims fims = new Fims(mapping, null);
            addFimsRules(new Digester(), fims);
            fims.run(processController);
            String results = fims.results();
            processController.appendStatus("<br>" + results);
            // Set the public status
            ExpeditionMinter expeditionMinter = new ExpeditionMinter();
            expeditionMinter.updateExpeditionPublicStatus(processController.getUserId(), processController.getExpeditionCode(),
                    processController.getProjectId(), processController.getPublicStatus());
            expeditionMinter.close();
            //Html2Text parser = new Html2Text();
            //fimsPrinter.out.println(parser.convert(results));
            FimsPrinter.out.println(results);
        }
    }


    /**
     * Run a query from the command-line. This is not meant to be a full-featured query service but a simple way of
     * fetching results
     */
    public String query(String[] graphs, String format, ArrayList<FimsFilterCondition> filter) {
        // Build the Query Object by passing this object and an array of graph objects, separated by commas
        FimsQueryBuilder q = new FimsQueryBuilder(this, graphs, outputFolder);
        // Add our filter conditions
        q.addFilter(filter);
        // Run the query, passing in a format and returning the location of the output file
        return q.run(format);
    }

    /**
     * Process metadata component rules
     *
     * @param d
     */
    public synchronized void addFimsRules(Digester d, Fims fims) {
        d.push(fims);
        d.addObjectCreate("fims/metadata", Metadata.class);
        d.addSetProperties("fims/metadata");
        d.addCallMethod("fims/metadata", "addTextAbstract", 0);
        d.addSetNext("fims/metadata", "addMetadata");

        try {
            d.parse(configFile);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        } catch (SAXException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    /**
     * Process validation component rules
     *
     * @param d
     */
    public synchronized void addValidationRules(Digester d, Validation validation) {
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
        //d.addCallMethod("fims/validation/lists/list/field", "addField", 0);

        // Create field objects
        d.addObjectCreate("fims/validation/lists/list/field", Field.class);
        d.addSetProperties("fims/validation/lists/list/field");
        d.addSetNext("fims/validation/lists/list/field", "addField");
        d.addCallMethod("fims/validation/lists/list/field", "setValue", 0);

        // Create column objects
        d.addObjectCreate("fims/validation/worksheet/column", ColumnTrash.class);
        d.addSetProperties("fims/validation/worksheet/column");
        d.addSetNext("fims/validation/worksheet/column", "addColumn");

        try {
            d.parse(configFile);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        } catch (SAXException e) {
            throw new FimsRuntimeException(500, e);
        }
    }

    /**
     * Process mapping component rules
     *
     * @param d
     */
    public synchronized void addMappingRules(Digester d, Mapping mapping) {
        d.push(mapping);

        // Create entity objects
        d.addObjectCreate("fims/mapping/entity", Entity.class);
        d.addSetProperties("fims/mapping/entity");
        d.addSetNext("fims/mapping/entity", "addEntity");

        // Add attributes associated with this entity
        d.addObjectCreate("fims/mapping/entity/attribute", Attribute.class);
        d.addSetProperties("fims/mapping/entity/attribute");
        d.addCallMethod("fims/mapping/entity/attribute", "addDefinition", 0);
        // Next two lines are newer, may not appear in all configuration files
        d.addCallMethod("fims/mapping/entity/attribute/synonyms", "addSynonyms", 0);
        d.addCallMethod("fims/mapping/entity/attribute/dataFormat", "addDataFormat", 0);
        d.addSetNext("fims/mapping/entity/attribute", "addAttribute");

        // Create relation objects
        d.addObjectCreate("fims/mapping/relation", Relation.class);
        d.addSetNext("fims/mapping/relation", "addRelation");
        d.addCallMethod("fims/mapping/relation/subject", "addSubject", 0);
        d.addCallMethod("fims/mapping/relation/predicate", "addPredicate", 0);
        d.addCallMethod("fims/mapping/relation/object", "addObject", 0);

        try {
            d.parse(configFile);
        } catch (IOException e) {
            throw new FimsRuntimeException(500, e);
        } catch (SAXException e) {
            throw new FimsRuntimeException(500, e);
        }
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
        Integer projectId = 0;
        //System.out.print(defaultOutputDirectory);

        // Test configuration :
        // -d -t -u -i sampledata/Apogon***.xls

        // Direct output using the StandardPrinter subClass of fimsPrinter which send to fimsPrinter.out (for command-line usage)
        FimsPrinter.out = new StandardPrinter();


        // Some classes to help us
        CommandLineParser clp = new GnuParser();
        HelpFormatter helpf = new HelpFormatter();
        CommandLine cl;

        // The expedition code corresponds to a expedition recognized by BCID
        String expeditionCode = "";
        // The configuration template
        //String configuration = "";
        // The input file
        String inputFile = "";
        // The directory that we write all our files to
        String outputDirectory = "tripleOutput";
        // Write spreadsheet content back to a spreadsheet file, for testing
        Boolean triplify = false;
        Boolean upload = false;
        Boolean local = false;


        // Define our commandline options
        Options options = new Options();
        options.addOption("h", "help", false, "print this help message and exit");
        options.addOption("q", "query", true, "Run a query and pass in graph UUIDs to look at for this query -- Use this along with options C and S");
        options.addOption("f", "format", true, "excel|html|json|cspace  specifying the return format for the query");
        options.addOption("F", "filter", true, "Filter results based on a keyword search");

        options.addOption("e", "expeditionCode", true, "Expedition code.  You will need to obtain a data code before " +
                "loading data");
        options.addOption("o", "outputDirectory", true, "Output Directory");
        options.addOption("i", "input_file", true, "Input Spreadsheet");
        options.addOption("p", "projectId", true, "Project Identifier.  A numeric integer corresponding to your project");
        options.addOption("configFile", true, "Use a local config file instead of getting from server");

        options.addOption("bcid", "triplify", false, "Triplify only (upload process triplifies)");
        options.addOption("l", "local", false, "Local option operates purely locally and does not create proper globally unique identifiers.  Running the local option means you don't need a username and password.");

        options.addOption("u", "upload", false, "Upload");

        options.addOption("U", "username", true, "Username (for uploading data)");
        options.addOption("P", "password", true, "Password (for uploading data)");
        options.addOption("y", "yes", false, "Answer 'y' to all questions");


        // Create the commands parser and parse the command line arguments.
        try {
            cl = clp.parse(options, args);
        } catch (UnrecognizedOptionException e) {
            FimsPrinter.out.println("Error: " + e.getMessage());
            return;
        } catch (ParseException e) {
            FimsPrinter.out.println("Error: " + e.getMessage());
            return;
        }

        // Set the input format
        if (cl.hasOption("y")) {
            FimsInputter.in = new ForceInputter();
        } else {
            FimsInputter.in = new StandardInputter();
        }

        // Set username
        if (cl.hasOption("U")) {
            username = cl.getOptionValue("U");
        }

        // Set password
        if (cl.hasOption("P")) {
            password = cl.getOptionValue("P");
        }

        // Check username and password
        if (cl.hasOption("u") && (username.equals("") || password.equals(""))) {
            FimsPrinter.out.println("Must specify a valid username or password for uploading data!");
            return;
        }

        // Query option must also have projectId option
        if (cl.hasOption("q")) {
            if (!cl.hasOption("p")) {
                helpf.printHelp("fims ", options, true);
                return;
            }

        }
        // Help
        else if (cl.hasOption("h")) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // No options returns help message
        if (cl.getOptions().length < 1) {
            helpf.printHelp("fims ", options, true);
            return;
        }

        // Sanitize project specification
        if (cl.hasOption("p")) {
            try {
                projectId = new Integer(cl.getOptionValue("p"));
            } catch (Exception e) {
                FimsPrinter.out.println("Bad option for projectId");
                helpf.printHelp("fims ", options, true);
                return;
            }
        }

        // Check for projectId when uploading data
        if (cl.hasOption("u") && projectId < 1) {
            FimsPrinter.out.println("Must specify a valid projectId when uploading data");
            return;
        }

        // Set input file
        if (cl.hasOption("i"))
            inputFile = cl.getOptionValue("i");

        // Set output directory
        if (cl.hasOption("o"))
            outputDirectory = cl.getOptionValue("o");

        // Set expeditionCode
        if (cl.hasOption("e"))
            expeditionCode = cl.getOptionValue("e");

        // Set triplify option
        if (cl.hasOption("bcid"))
            triplify = true;

        // Set the "local" option
        if (cl.hasOption("l"))
            local = true;

        // Set upload option
        if (cl.hasOption("u"))
            upload = true;

        // Set default output directory if one is not specified
        if (!cl.hasOption("o")) {
            FimsPrinter.out.println("Using default output directory " + defaultOutputDirectory);
            outputDirectory = defaultOutputDirectory;
        }

        // Check that output directory is writable
        try {
            if (!new File(outputDirectory).canWrite()) {
                FimsPrinter.out.println("Unable to write to output directory " + outputDirectory);
                return;
            }
        } catch (Exception e) {
            FimsPrinter.out.println("Unable to write to output directory " + outputDirectory);
            return;
        }

        // Run the command
        try {
            /*
            Run a query
             */
            if (cl.hasOption("q")) {

                File file = new ConfigurationFileFetcher(projectId, outputDirectory, true).getOutputFile();

                Process p = new Process(
                        projectId,
                        outputDirectory,
                        file
                );

                //p.query(cl.getOptionValue("q"), cl.getOptionValue("f"), cl.getOptionValue("F"));
                // TODO: construct filter statements from arguments passed in on command-line
                System.out.println(p.query(cl.getOptionValue("q").split(","), cl.getOptionValue("f"), null));
            }
            /*
           Run the validator
            */
            else {
                // if we only want to triplify and not upload, then we operate in LOCAL mode
                if (local && triplify) {
                    ProcessController pc = new ProcessController();
                    pc.appendStatus("Triplifying using LOCAL only options, useful for debugging\n");
                    pc.appendStatus("Does not construct GUIDs, use Deep Roots, or connect to project-specific configurationFiles");

                    Process p = new Process(inputFile, outputDirectory, pc, new File(cl.getOptionValue("configFile")));
                    p.runAllLocally(true, false, false, false);
                    /*p.runValidation();
                    triplifier t = new triplifier("test", outputDirectory);
                    p.mapping.run(t, pc);
                    p.mapping.print();  */

                } else {
                    // Create the appropritate connection string depending on options
                    if (triplify || upload) {
                        if (username == null || username.equals("") || password == null || password.equals("")) {
                            FimsPrinter.out.println("Need valid username / password for uploading");
                            helpf.printHelp("fims ", options, true);
                            return;
                        } else {
                            Authenticator authenticator = new Authenticator();
                            FimsPrinter.out.println("Authenticating ...");

                            if (!authenticator.login(username, password)) {
                                FimsPrinter.out.println("Unable to authenticate " + username +
                                        " using the supplied credentials!");
                                return;
                            }

                            // Check that a expedition code has been entered
                            if (!cl.hasOption("e")) {
                                FimsPrinter.out.println("Need to enter a expedition code before  uploading");
                                helpf.printHelp("fims ", options, true);
                                return;
                            }
                        }

                        // Now run the process
                        Process p;
                        ProcessController processController = new ProcessController(projectId, expeditionCode);
                        processController.setUserId(username);

                        // use local configFile if specified
                        if (cl.hasOption("configFile")) {
                            System.out.println("using local config file = " + cl.getOptionValue("configFile").toString());
                            p = new Process(
                                    inputFile,
                                    outputDirectory,
                                    processController,
                                    new File(cl.getOptionValue("configFile")));
                        } else {
                            p = new Process(
                                    inputFile,
                                    outputDirectory,
                                    processController
                            );
                        }

                        FimsPrinter.out.println("Initializing ...");
                        FimsPrinter.out.println("\tinputFilename = " + inputFile);

                        // Run the processor
                        p.runAllLocally(triplify, upload, true, false);
                    }
                }

            }
        } catch (
                Exception e
                )

        {
            FimsPrinter.out.println("\nError: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

}