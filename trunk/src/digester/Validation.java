package digester;

import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import renderers.Message;
import renderers.RowMessage;
import renderers.RendererInterface;
import settings.CommandLineInputReader;
import settings.Connection;
import settings.PathManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * digester.Validation class holds all worksheets that are part of this validator
 */
public class Validation implements RendererInterface {
    // Loop all the worksheets associated with the validation element
    private final LinkedList<Worksheet> worksheets = new LinkedList<Worksheet>();
    // Loop all the lists associated with the validation element
    private final LinkedList<List> lists = new LinkedList<List>();
    // Create a tabularDataReader for reading the data source associated with the validation element
    private TabularDataReader tabularDataReader = null;
    // File reference for a sqlite Database
    private File sqliteFile;
    // A SQL Lite connection is mainted by the validation class so we can run through the various rules
    private java.sql.Connection connection = null;

    /**
     * Construct using tabularDataReader object, defining how to read the incoming tabular data
     */
    public Validation() throws Exception {

    }

    /**
     * Return the tabularDataReader object
     *
     * @return the tabularDataReader object associated with the validation element
     */
    public TabularDataReader getTabularDataReader() {
        return tabularDataReader;
    }

    /**
     * The reference to the SQLite instance
     *
     * @return
     */
    public File getSqliteFile() {
        return sqliteFile;
    }

    /**
     * Add a worksheet to the validation component
     *
     * @param w
     */
    public void addWorksheet(Worksheet w) {
        worksheets.addLast(w);
    }

    public LinkedList<Worksheet> getWorksheets() {
        return worksheets;
    }

    /**
     * Add a list to the validation component
     *
     * @param l
     */
    public void addList(List l) {
        lists.addLast(l);
    }

    /**
     * Get the set of lists defined by this validation object
     *
     * @return
     */
    public LinkedList<List> getLists() {
        return lists;
    }

    /**
     * Lookup a list by its alias
     *
     * @param alias
     * @return
     */
    public List findList(String alias) {
        for (Iterator<List> i = lists.iterator(); i.hasNext(); ) {
            List l = i.next();
            if (l.getAlias().equals(alias))
                return l;
        }
        return null;
    }

    /**
     * Create a SQLLite database instance
     *
     * @return
     * @throws Exception
     */
    private void createSqlLite(String filenamePrefix, String outputFolder) throws Exception {
        PathManager pm = new PathManager();
        File processDirectory = null;

        try {
            processDirectory = pm.setDirectory(outputFolder);
        } catch (Exception e) {
           // e.printStackTrace();
            throw new Exception("unable to set output directory " + processDirectory);
        }

        // Load the SQLite JDBC driver.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            //ex.printStackTrace();
            throw new Exception("could not load the SQLite JDBC driver.");
        }

        try {
            // Create SQLite file
            //String pathPrefix = processDirectory + File.separator + inputFile.getName();
            String pathPrefix = processDirectory + File.separator + filenamePrefix;
            sqliteFile = PathManager.createUniqueFile(pathPrefix + ".sqlite", outputFolder);

            TabularDataConverter tdc = new TabularDataConverter(tabularDataReader, "jdbc:sqlite:" + sqliteFile.getAbsolutePath());
            tdc.convert(false);
            tabularDataReader.closeFile();
        } catch (Exception e) {
            //e.printStackTrace();
            throw new Exception("Trouble creating SQLlite file");
        }


        // Create the SQLLite connection
        try {
            Connection localConnection = new Connection(sqliteFile);
            connection = java.sql.DriverManager.getConnection(localConnection.getJdbcUrl());
        } catch (Exception e) {
            // e.printStackTrace();
            throw new Exception("Trouble finding SQLLite Connection", e);
        }

    }

    /**
     * Loop through worksheets and print out object data
     */
    public void printObject() {
        System.out.println("Validate");

        for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
            Worksheet w = i.next();
            w.print();
        }
    }

    /**
     * Standard print method
     */
    public void print() {

    }
    /**
     * Print output for the commandline
     */
    public boolean printMessages() {

        for (Iterator<Worksheet> w = worksheets.iterator(); w.hasNext(); ) {
            Worksheet worksheet = w.next();
            System.out.println("\t" + worksheet.getSheetname() + " worksheet results");
            for (String msg : worksheet.getUniqueMessages(Message.ERROR)) {
                System.out.println("\t\t" + msg);
            }
            for (String msg : worksheet.getUniqueMessages(Message.WARNING)) {
                System.out.println("\t\t" + msg);
            }
            // Worksheet has errors
            if (!worksheet.errorFree()) {
                System.out.println("\tErrors found on " + worksheet.getSheetname() + " worksheet.  Must fix to continue.");
                return false;
            } else {
                // Worksheet has no errors but does have some warnings
                if (!worksheet.warningFree()) {
                    System.out.println("\tWarnings found on " + worksheet.getSheetname() + " worksheet. ");
                    System.out.print("\tIf you wish to continue loading with warnings, enter 'Y': ");
                    try {
                        String response = new CommandLineInputReader().getResponse();
                        if (response.equalsIgnoreCase("Y")) {
                            return true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                //Worksheet has no errors or warnings
                } else {
                    return true;
                }
            }
        }
        return true;
    }

    /**
     * Begin the validation process, looping through worksheets
     *
     * @return
     * @throws Exception
     */
    public boolean run(TabularDataReader tabularDataReader, String filenamePrefix, String outputFolder) throws Exception {
        System.out.println("Validate ...");

        // Default the tabularDataReader to the first sheet defined by the digester Worksheet instance
          this.tabularDataReader = tabularDataReader;
        tabularDataReader.setTable(worksheets.get(0).getSheetname());

        try {
            createSqlLite(filenamePrefix, outputFolder);
        } catch (Exception e) {
            throw new Exception("Unable to create SQLLite DB instance", e);
        }

        boolean errorFree = true;
        for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
            digester.Worksheet w = i.next();

            boolean thisError = w.run(this);
            if (errorFree)
                errorFree = thisError;
        }
        return errorFree;
    }

    /**
     * Close the validation component when we're done with it-- here we close the reference to the SQLlite connection
     */
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.sql.Connection getConnection() {
        return connection;
    }
}