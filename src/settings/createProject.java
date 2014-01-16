package settings;

import org.apache.commons.cli.*;

/**
 * Code to create a project.
 * Ultimately, there will be a web-interface for administrators to create projects.
 * For now, we define a couple of project types statically with fixed pointers to BCID
 * resources.  The BCID resources are immutable so the integer reference here presumed to be safe
 *
 */
public class createProject {

    static Options opts = new Options();

    bcidConnector bcidConnector;
    String project_code;

    // Reference to single resource Type
    static Integer[] biocodeFIMSGroupElements = new Integer[]{34};

    // References to biocode-fims Resources Types Full Ontology (within BCID System)
    static Integer[] biocodeFIMSGroupElementsFull = new Integer[]{
            34, // Resource
            10, // Location
            24, // Identification
            36, // NucleicAcidSequenceSource
            37, // Sequencing
            11, // Agent
            15, // MaterialSample
            13, // InformationContentEntity
            2,  // Event
            25  // Taxon
    };

    // References to DwC Group Elements (within BCID System)
    static Integer[] dwcGroupElements = new Integer[]{
            10, // Location
            24, // Identification
            2,  // Event
            25, // Taxon
            23  // Occurrence
    };


    /**
     * Create a Project in the BCID system.  This creates the project code.  Must first authenticate as a valid user
     * before proceeding.
     *
     * Steps:
     * 1. Create authentication request object, which stores authentication credentials
     * 2. Create the project itself
     *
     * @param username
     * @param password
     * @throws Exception
     */
    public createProject(String project_code, String username, String password) throws Exception {
        this.project_code = project_code;

        // First, authenticate username/password here
        fimsPrinter.out.println("Authenticating ...");
        bcidConnector = new bcidConnector();
        boolean authenticationSuccess = bcidConnector.authenticate(username, password);
        if (!authenticationSuccess)
            throw new Exception("Unable to authenticate user " + username);

        fimsPrinter.out.println("\tUser " + username + " authenticated");

        // Now check that this project code is available
        // TODO: the configuration URL is hard-coded below!
        String response = bcidConnector.createProject(project_code, project_code + " data group", null, 1);

        fimsPrinter.out.println("\t" + response);
        //throw new Exception("Project code " + project_code + " is unavailable");
    }


    /**
     * Command-line tool for creating projects.  Used by system administrator only
     *
     * @param args
     */
    public static void main(String[] args) {
        // A reference to this class
        createProject p = null;

        // Add the options for the program
        opts.addOption("h", "help", false, "Print this help message and exit");
        opts.addOption("u", "username", true, "The BCID application username");
        opts.addOption("p", "password", true, "The BCID application password");
        opts.addOption("c", "project_code", true, "Your proposed project code.  Must be between 4 and 6 characters " +
                "and NOT exist within the BCID system");
        opts.addOption("t", "type", true, "{dwc|biocode-fims} indicates the types of classes to associate " +
                "with this project");

        // Create the commands parser and parse the command line arguments.
        CommandLineParser clp = new GnuParser();
        CommandLine cl = null;
        try {
            cl = clp.parse(opts, args);
        } catch (UnrecognizedOptionException e) {
            exit("Error: " + e.getMessage());
        } catch (ParseException e) {
            exit("Error: " + e.getMessage());
        }

        // If help was requested, print the help message and exit.
        if (cl.hasOption("h")) {
            exit("");
        }

        String missingOptions = "";
        if (!cl.hasOption("u")) missingOptions += "username ";
        if (!cl.hasOption("p")) missingOptions += "password ";
        if (!cl.hasOption("c")) missingOptions += "project_code ";
        if (!cl.hasOption("t")) missingOptions += "type ";

        if (!missingOptions.equals("")) {
            exit("Missing one or more options: " + missingOptions);
        }

        // Setup variables based on user input options
        String username = cl.getOptionValue("u");
        String password = cl.getOptionValue("p");
        String project_code = cl.getOptionValue("c");
        String type = cl.getOptionValue("t");


        // Set the proper groupElement array
        Integer[] groupElements = null;
        if (type.equals("dwc")) {
            groupElements = dwcGroupElements;
        } else if (type.equals("biocode-fims")) {
            groupElements = biocodeFIMSGroupElements;
        } else {
            exit("Did not recognize type = " + type);
        }

        // Instantiate settings.createProject object
        try {
            p = new createProject(project_code, username, password);
        } catch (Exception e) {
            fimsPrinter.out.println("Unable to create project: " + e.getMessage());
            System.exit(-1);
        }

        // Create individual BCIDs that go with this project
        fimsPrinter.out.println("Creating BCIDs:");
        for (Integer id : groupElements) {
            try {
                fimsPrinter.out.println("\t" + p.createBCIDAndAssociate(id));
            } catch (Exception e) {
                fimsPrinter.out.println("\tTrouble creating BCID: " + e.getMessage());
                System.exit(-1);
            }
        }
        return;
    }

    /**
     * Create an individual BCID and Associate
     *
     * @return
     * @throws Exception
     */
    private String createBCIDAndAssociate(Integer resourceType) throws Exception {
        String bcid = bcidConnector.createBCID("", project_code + " group element", resourceType);
        bcidConnector.associateBCID(project_code, bcid);
        return bcid;
    }

    /**
     * exit message
     *
     * @param message
     */
    private static void exit(String message) {
        HelpFormatter helpf = new HelpFormatter();
        fimsPrinter.out.println(message + "\n");
        helpf.printHelp("settings.createProject input_files", opts, true);
        System.exit(-1);
    }

}
