package digester;

import org.openjena.riot.pipeline.SinkTripleNodeTransform;
import renderers.Message;
import renderers.RowMessage;
import settings.fimsPrinter;
import sun.rmi.transport.Connection;

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
                Message newMsg = new Message(m.getMessage(), m.getLevel(), m.getList());
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
     * Loop all validation rules associated with this worksheet
     *
     * @param parent
     * @return
     */
    public boolean run(Object parent) {
        // Set a reference to the validation parent
        validation = (Validation) parent;
        java.sql.Connection connection = validation.getConnection();

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
                // Run this rule
                Method method = r.getClass().getMethod(r.getType());
                if (method != null) {
                    method.invoke(r);
                } else {
                    fimsPrinter.out.println("\tNo method " + r.getType() + " (" + r.getColumn() + ")");
                }

                // Close the connection
            } catch (Exception e) {
                //e.printStackTrace();
                String message = "\tUnable to run " + r.getType() + " on " + r.getColumn()  + " column";
                if (e.getMessage() != null)
                    message += ", message = " + e.getMessage();
                fimsPrinter.out.println(message);
                //return false;
            }

            // Display warnings/etc...
            messages.addAll(r.getMessages());

        }
        // Close our connection
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
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
