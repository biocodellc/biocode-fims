package digester;

import static ch.lambdaj.Lambda.*;

import ch.lambdaj.group.Group;
import reader.TabularDataConverter;
import reader.plugins.TabularDataReader;
import renderers.Message;
import renderers.RendererInterface;
import renderers.RowMessage;
import run.processController;
import settings.*;
import utils.Html2Text;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

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
     *
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
     *
     * @throws Exception
     */
    private void createSqlLite(String filenamePrefix, String outputFolder, Mapping mapping) throws Exception {
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
            tdc.convert(mapping);
            tabularDataReader.closeFile();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
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
        fimsPrinter.out.println("Validate");

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
    public processController printMessages(processController processController) {
        StringBuilder errorSB = new StringBuilder();
        StringBuilder warningSB = new StringBuilder();
        // Create a simplified output stream just for commandline printing.
        StringBuilder commandLineWarningSB = new StringBuilder();
        Html2Text htmlParser = new Html2Text();
        java.util.List<String> warnings = new ArrayList<String>();

        for (Iterator<Worksheet> w = worksheets.iterator(); w.hasNext(); ) {
            Worksheet worksheet = w.next();
            processController.setWorksheetName(worksheet.getSheetname());
            String status1 = "\t<b>Validation results on \"" + worksheet.getSheetname() + "\" worksheet.</b>";
            processController.appendStatus("<br>" + status1);
            //fimsPrinter.out.println(status1);

            /*
            for (String msg : worksheet.getUniqueMessages(Message.ERROR)) {
                errorSB.append("\t\t" + msg + "\n");
                //fimsPrinter.out.println("\t\t" + msg);
            }
            for (String msg : worksheet.getUniqueMessages(Message.WARNING)) {
                warningSB.append("\t\t" + msg + "\n");
                //warnings.add(msg);
            }
            */

            // Group all Messages using lambdaj jar library
            Group<RowMessage> rowGroup = group(worksheet.getMessages(), by(on(RowMessage.class).getGroupMessageAsString()));
            warningSB.append("<div id=\"expand\">");
            for (String key : rowGroup.keySet()) {
                warningSB.append("<dl>");
                warningSB.append("<dt>" + key + "</dt>");
                java.util.List<RowMessage> rowMessageList = rowGroup.find(key);

                // Parse the Row Messages that are meant for HTML display
                try {
                    commandLineWarningSB.append(htmlParser.convert(key)+"\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                for (RowMessage m : rowMessageList) {
                    warningSB.append("<dd>" + m.print() + "</dd>");
                    commandLineWarningSB.append("\t" + m.print() + "\n");
                }
                warningSB.append("</dl>");
            }
            warningSB.append("</div>");

            // Worksheet has errors
            if (!worksheet.errorFree()) {

                //fimsPrinter.out.println(warningSB.toString());
                fimsPrinter.out.println(commandLineWarningSB.toString());

                processController.appendStatus("<br><b>1 or more errors found.  Must fix to continue. Click each message for details</b><br>");
                processController.appendStatus(warningSB.toString());

                processController.setHasErrors(true);
                processController.setWarningsSB(warningSB);
                return processController;
            } else {
                // Worksheet has no errors but does have some warnings
                if (!worksheet.warningFree()) {
                    processController.appendStatus("<br><b>1 or more warnings found. Click each message for details</b><br>");
                    processController.appendStatus(warningSB.toString());
                    processController.setHasWarnings(true);
                    processController.setWarningsSB(warningSB);
                    return processController;
                    //Worksheet has no errors or warnings
                } else {
                    processController.setHasWarnings(false);
                    processController.setValidated(true);
                    return processController;
                }
            }
        }
        return processController;
    }

    /**
     * Begin the validation run.process, looping through worksheets
     *
     * @return
     *
     * @throws Exception
     */
    public boolean run(TabularDataReader tabularDataReader, String filenamePrefix, String outputFolder, Mapping mapping) throws Exception {
        fimsPrinter.out.println("Validate ...");

        // Default the tabularDataReader to the first sheet defined by the digester Worksheet instance
        this.tabularDataReader = tabularDataReader;

        Worksheet sheet = null;
        String sheetName = "";
        try {
            sheet = worksheets.get(0);
            sheetName = sheet.getSheetname();
            tabularDataReader.setTable(sheetName);
        } catch (Exception e) {
            // An error here means the sheetname was not found, throw an application message
            sheet.getMessages().addLast(new RowMessage("Unable to find a required worksheet named '" + sheetName + "' (no quotes)", "Spreadsheet check", RowMessage.ERROR));
            return false;
        }

        try {
            createSqlLite(filenamePrefix, outputFolder, mapping);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage(), e);
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
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public java.sql.Connection getConnection() {
        return connection;
    }
}