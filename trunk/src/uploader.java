import java.io.File;
import java.io.FileNotFoundException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.Level;
import org.joda.time.DateTime;
import reader.ReaderManager;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import reader.fimsSimplifier;

import triplifyEngine.Connection;
import triplifyEngine.Mapping;
import triplifyEngine.triplifier;

/**
 * Provides a command-line tool for using the triplifier.
 */
public class uploader {

    public static void main(String[] args) throws Exception {

        // D2RQ uses log4j... usually the DEBUG messages are annoying so here we can just get the ERROR Messages
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.ERROR);

        Options opts = new Options();
        HelpFormatter helpf = new HelpFormatter();
        TabularDataReader tdr;
        TabularDataConverter tdc;
        boolean fixDwCA = true;

        // Add the options for the program.
        opts.addOption("s", "sqlite", false, "output SQLite files only");
        opts.addOption("h", "help", false, "print this help message and exit");

        // Create the commands parser and parse the command line arguments.
        CommandLineParser clp = new GnuParser();
        CommandLine cl;
        try {
            cl = clp.parse(opts, args);
        } catch (UnrecognizedOptionException e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        // If help was requested, print the help message and exit.
        if (cl.hasOption("h")) {
            helpf.printHelp("java triplify input_files", opts, true);
            return;
        }

        // Create the ReaderManager and load the plugins.
        ReaderManager rm = new ReaderManager();
        try {
            rm.loadReaders();
        } catch (FileNotFoundException e) {
            System.out.println("Error: Could not load data reader plugins.");
            return;
        }

        // Load the SQLite JDBC driver.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            System.out.println("Error: Could not load the SQLite JDBC driver.");
            return;
        }

        String[] fnames = cl.getArgs();
        File file, sqlitefile;
        int filecounter;

        // Process each input file specified on the command line.
        for (int cnt = 0; cnt < fnames.length; cnt++) {
            file = new File(fnames[cnt]);

            tdr = rm.openFile(fnames[cnt]);
            if (tdr == null) {
                System.out.println("Error: Unable to open input file " + fnames[cnt] +
                        ".  Will continue trying to read any reamaining input files.");
                continue;
            }

            // Read the properties
            System.out.println("Configuring ...");
            System.out.println("\tTODO: Read XML File, set global propertie");

            // Validate Spreadsheet
            System.out.println("Validating ...");
            System.out.println("\tTODO: Incorporate bioValidator engine here ");

            // Create SQLite file
            System.out.println("Triplifying ...");
            System.out.println("\tLoading to SQLlite DB");
            String pathPrefix = System.getProperty("user.dir") + File.separator + file.getName();
            sqlitefile = new File(pathPrefix + ".sqlite");
            filecounter = 1;
            while (sqlitefile.exists())
                sqlitefile = new File(pathPrefix + "_" + filecounter++ + ".sqlite");
            tdc = new TabularDataConverter(tdr, "jdbc:sqlite:" + sqlitefile.getName());
            tdc.convert(fixDwCA);
            tdr.closeFile();

            // Triplify
            if (!cl.hasOption("s")) {
                // Create connection to SQLlite database
                Connection connection = new Connection(sqlitefile);
                triplifier t = new triplifier();

                // Construct the crudeSimplifier
                System.out.println("\tBeginning simplifier instantiation");
                fimsSimplifier s = new fimsSimplifier(connection);

                // Create mapping file
                System.out.println("\tBeginning mapping file creation");
                Mapping mapping = new Mapping(connection, s.join, s.entity, s.relation);

                // Triplify
                System.out.println("\tBeginning triple file creation");
                String results = t.getTriples(file.getName(), mapping);
                System.out.println("\tTriple file written to: " + results);
            }

            // Upload triplified file to specified database
            System.out.println("Publishing ...");
            System.out.println("\tTODO: Load this into a public triplestore, instance by instance ");
        }
    }


}
