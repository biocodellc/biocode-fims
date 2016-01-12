package digester;

import static ch.lambdaj.Lambda.*;

import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import ch.lambdaj.group.Group;
import biocode.fims.fimsExceptions.FimsException;
import org.apache.commons.digester3.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import biocode.fims.reader.plugins.TabularDataReader;
import org.xml.sax.SAXException;
import renderers.RendererInterface;
import renderers.RowMessage;
import run.ProcessController;
import biocode.fims.settings.FimsPrinter;
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
    // A SQL Lite connection is mainted by the validation class so we can run through the various rules
    private java.sql.Connection connection = null;
    private static Logger logger = LoggerFactory.getLogger(Validation.class);

    // Create a reference to the mapping component
    private Mapping mapping;

    /**
     * Construct using tabularDataReader object, defining how to read the incoming tabular data
     */
    public Validation() {

    }

    public TabularDataReader getTabularDataReader() {return tabularDataReader;}
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
     * Loop through worksheets and print out object data
     */
    public void printObject() {
        FimsPrinter.out.println("Validate");

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
    public ProcessController printMessages(ProcessController processController) {
        StringBuilder warningSB = new StringBuilder();
        // Create a simplified output stream just for commandline printing.
        StringBuilder commandLineWarningSB = new StringBuilder();
        Html2Text htmlParser = new Html2Text();

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
                commandLineWarningSB.append(htmlParser.convert(key)+"\n");

                for (RowMessage m : rowMessageList) {
                    warningSB.append("<dd>" + m.print() + "</dd>");
                    commandLineWarningSB.append("\t" + m.print() + "\n");
                }
                warningSB.append("</dl>");
            }
            warningSB.append("</div>");

            // Worksheet has errors
            if (!worksheet.errorFree()) {

                processController.appendStatus("<br><b>1 or more errors found.  Must fix to continue. Click each message for details</b><br>");
                processController.appendStatus(warningSB.toString());

                processController.setHasErrors(true);
                processController.setCommandLineSB(commandLineWarningSB);
                return processController;
            } else {
                // Worksheet has no errors but does have some warnings
                if (!worksheet.warningFree()) {
                    processController.appendStatus("<br><b>1 or more warnings found. Click each message for details</b><br>");
                    processController.appendStatus(warningSB.toString());
                    processController.setHasWarnings(true);
                    processController.setCommandLineSB(commandLineWarningSB);

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
     * Get the mapping component that was set by the run() method
     * @return
     */
    public Mapping getMapping() {
        return mapping;
    }

    /**
     * Begin the validation run.process, looping through worksheets
     *
     * @return
     */
    public boolean run(TabularDataReader tabularDataReader, String filenamePrefix, String outputFolder, Mapping mapping) {
        FimsPrinter.out.println("Validate ...");
        this.mapping = mapping;
        this.tabularDataReader = tabularDataReader;

        Worksheet sheet = null;
        String sheetName = "";
        try {
            sheet = worksheets.get(0);
            sheetName = sheet.getSheetname();
            tabularDataReader.setTable(sheetName);
        } catch (FimsException e) {
            // An error here means the sheetname was not found, throw an application message
            sheet.getMessages().addLast(new RowMessage("Unable to find a required worksheet named '" + sheetName + "' (no quotes)", "Spreadsheet check", RowMessage.ERROR));
            return false;
        }

        // Use the errorFree variable to control validation checking workflow
        boolean errorFree = true;

        // Create the SQLLite Connection and catch any exceptions, and send to Error Message
        // Exceptions generated here are most likely useful to the user and the result of SQL exceptions when
        // processing data, such as worksheets containing duplicate column names, which will fail the data load.
        try {
            connection = mapping.createSqlLite(filenamePrefix, outputFolder, mapping, tabularDataReader);
        }   catch (FimsException e) {
            errorFree = false;
            sheet.getMessages().addLast(new RowMessage(
                    "Unable to parse spreadsheet.  This is most likely caused by having two columns with the same " +
                    "name.  Please rename or delete the extra column to pass validation.",
                    "Initial Spreadsheet check",
                    RowMessage.ERROR));
            e.printStackTrace();
        }

        if (errorFree) {
            // Loop rules to be run after connection
            for (Iterator<Worksheet> i = worksheets.iterator(); i.hasNext(); ) {
                digester.Worksheet w = i.next();

                boolean thisError = w.run(this);
                if (errorFree)
                    errorFree = thisError;
            }
            return errorFree;
        } else {
            return false;
        }
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
            logger.warn("SQLException", e);
        }
    }

    public java.sql.Connection getConnection() {
        return connection;
    }

    /**
     * Process validation component rules
     *
     * @param d
     */
    public synchronized void addValidationRules(Digester d, File configFile) {
        d.push(this);

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
}