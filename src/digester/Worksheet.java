package digester;

import com.hp.hpl.jena.graph.query.PatternStageBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import renderers.Message;
import renderers.RowMessage;
import settings.FIMSRuntimeException;
import settings.fimsPrinter;
import utils.SettingsManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * digester.Worksheet class holds all elements pertaining to worksheets including most importantly
 * rules and columns.  Rules define all the validation rules associated with this worksheet and
 * columns define all of the column names to be found in this worksheet.  The Worksheet class also
 * defines a LinkedList of messages which store all processing messages (errors or warnings) when
 * running through this worksheet's rules.
 */
public class Worksheet {

    // The name of this worksheet (as defined by the spreadsheet)
    private String sheetname;
    // Store the rules associated with this worksheet
    private final java.util.List<Rule> rules = new ArrayList<Rule>();
    // Store the validation object, passed into the run method
    private Validation validation = null;
    // Store all messages related to this Worksheet
    private LinkedList<RowMessage> messages = new LinkedList<RowMessage>();
    // Store the reference for the columns associated with this worksheet
    private final java.util.List<Column_trash> columns = new ArrayList<Column_trash>();

    private static Logger logger = LoggerFactory.getLogger(Worksheet.class);
    /**
     * Add columns element to the worksheet element
     *
     * @param column
     */
    public void addColumn(Column_trash column) {
        columns.add(column);
    }

    /**
     * Get all the processing/validation messages associated with this worksheet
     *
     * @return
     */
    public LinkedList<RowMessage> getMessages() {
        return messages;
    }

    /**
     * Get just the unique messages
     *
     * @return
     */
    public LinkedList<String> getUniqueMessages(Integer errorLevel) {
        LinkedList<String> stringMessage = new LinkedList<String>();

        // Create just a plain Message, no row designation
        for (RowMessage m : messages) {
            if (m.getLevel() == errorLevel) {
                Message newMsg = new Message(m.getMessage(), m.getLevel(), m.getGroupMessage());
                stringMessage.add(newMsg.print());
            }
        }
        LinkedList<String> newList = new LinkedList<String>(new HashSet<String>(stringMessage));
        return newList;
    }

    /**
     * Get a reference to the validation object.  This is useful when working with
     * worksheets and rules to reference objects belonging to the validation object,
     * in particular lists.
     *
     * @return
     */
    public Validation getValidation() {
        return validation;
    }

    /**
     * Set the name of this worksheet
     *
     * @param sheetname
     */
    public void setSheetname(String sheetname) {
        this.sheetname = sheetname;
    }

    /**
     * Get the name of this worksheet
     *
     * @return
     */
    public String getSheetname() {
        return sheetname;
    }

    /**
     * Add a rule for this worksheet
     *
     * @param rule
     */
    public void addRule(Rule rule) {
        rules.add(rule);
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void print() {
        fimsPrinter.out.println("  sheetname=" + sheetname);

        fimsPrinter.out.println("  rules ... ");
        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            r.print();
        }

        fimsPrinter.out.println("  columns ... ");
        for (Iterator<Column_trash> i = columns.iterator(); i.hasNext(); ) {
            Column_trash c = i.next();
            c.print();
        }
    }

    /**
     * Print metadata on all the rules contained in this worksheet
     * @return
     */
    /*
    public String printRuleMetadata() {
        StringBuilder output = new StringBuilder();

        Iterator rulesIt = rules.iterator();
        while (rulesIt.hasNext()) {
            Rule r = (Rule) rulesIt.next();
            output.append("<li>\n");
            //
            String prettyTypeName = r.getType();
            if (r.getType().equals("checkInXMLFields")) {
                prettyTypeName = "Lookup Value From List";
            }
            // Display the Rule type
            output.append("\t<li>type: " + r.getType() + "</li>\n");
            // Display warning levels
            output.append("\t<li>level: " + r.getLevel() + "</li>\n");
            // Display values
            if (r.getValue() != null) {
                output.append("\t<li>value: " + r.getValue() + "</li>\n");
            }
            // Display fields
            Iterator it = null, listIt = null;
            try {
                it = r.getFields().iterator();
            } catch (Exception e) {
                // Null List
            }
            try {
                listIt = r.getListElements().iterator();
            } catch (Exception e) {
                // Null List
            }
            boolean someValuesInList = false;
            // One or the other types of list need data
            if (it != null || listIt != null &&
                    (it.hasNext() || listIt.hasNext())) {
                someValuesInList = true;

            }
            if (someValuesInList)
                output.append("\t<li>list: \n");

            // Look at the Fields
            if (it != null) {
                while (it.hasNext()) {
                    String field = (String) it.next();
                    output.append("\t\t<li>" + field + "</li>\n");
                }
            }
            // Now look at lists
            if (listIt != null) {
                while (listIt.hasNext()) {
                    String field = (String) listIt.next();
                    output.append("\t\t<li>" + field + "</li>\n");
                }
            }

            if (someValuesInList)
                output.append("\t</li>\n");

            output.append("</li>\n");
        }
        return output.toString();
    }
       */
    /**
     * Return a list of columns given a list alias.  This shows us ALL of the columns that may reference
     * a particular list
     *
     * @param list
     *
     * @return
     */
    public ArrayList<String> getColumnsForList(String list) {
        ArrayList columns = new ArrayList();
        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            if (list.equals(r.getList())) {
                columns.add(r.getColumn());
            }
        }
        return columns;
    }

    /**
     * Loop all validation rules associated with this worksheet
     *
     * @param parent
     *
     * @return
     */
    public boolean run(Object parent) {
        SettingsManager sm = SettingsManager.getInstance();
        sm.loadProperties();

        // Set a reference to the validation parent
        validation = (Validation) parent;
        java.sql.Connection connection = validation.getConnection();

        // Default rule... always check that there is some data
        if (validation.getTabularDataReader().getNumRows() < 1) {
            messages.addLast(new RowMessage("No data found", "Spreadsheet check", RowMessage.ERROR));
            //System.out.println("number of rows = " + validation.getTabularDataReader().getNumRows());
        } else {

            for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
                Rule r = i.next();

                // Run this particular rule
                try {
                    /*
                   //Text to show what rules are running (this is not necessary normally)
                   String message = "\trunning rule " + r.getType();
                   // Create a special connection to use here
                   if (r.getColumn() != null)
                       message += " for " + r.getColumn();
                   fimsPrinter.out.println(message);
                    */


                    // Set the digester worksheet instance for this Rule
                    r.setDigesterWorksheet(this);
                    // Set the SQLLite reference for this Rule
                    r.setConnection(connection);
                    // Set the TabularDataReader worksheet instance for this Rule
                    r.setWorksheet(validation.getTabularDataReader());
                    // FIMS Service root
                    r.setServiceRoot(sm.retrieveValue("fims_service_root"));
                    // Run this rule
                    Method method = r.getClass().getMethod(r.getType());
                    if (method != null) {
                        method.invoke(r);
                    } else {
                        fimsPrinter.out.println("\tNo method " + r.getType() + " (" + r.getColumnWorksheetName() + ")");
                    }

                    // Close the connection
                } catch (NoSuchMethodException e) {
                   throw new FIMSRuntimeException("Unable to run " + r.getType() + " on \"" + r.getColumnWorksheetName() +
                        "\" column", 500, e);
                } catch(IllegalAccessException e) {
                    throw new FIMSRuntimeException("Unable to run " + r.getType() + " on \"" + r.getColumnWorksheetName() +
                        "\" column", 500, e);
                } catch (InvocationTargetException e) {
                    throw new FIMSRuntimeException("Unable to run " + r.getType() + " on \"" + r.getColumnWorksheetName() +
                        "\" column", 500, e);
                }

                // Display warnings/etc...
                messages.addAll(r.getMessages());

            }
        }
        // Close our connection
        try {
            connection.close();
        } catch (SQLException e) {
            logger.warn("SQLException", e);
        }

        return errorFree();
    }

    /**
     * Indicate whether this worksheet is error free or not
     *
     * @return true if this worksheet is clean
     */
    public boolean errorFree() {
        // Check all messages to see if any type of error has been found
        for (Iterator<RowMessage> m = messages.iterator(); m.hasNext(); ) {
            if (m.next().getLevel() == RowMessage.ERROR)
                return false;
        }
        return true;
    }

    /**
     * Indicate whether this worksheet is warning free or not
     *
     * @return true if this worksheet is clean
     */
    public boolean warningFree() {
        // Check all messages to see if any type of error has been found
        for (Iterator<RowMessage> m = messages.iterator(); m.hasNext(); ) {
            if (m.next().getLevel() == RowMessage.WARNING)
                return false;
        }
        return true;
    }
}
