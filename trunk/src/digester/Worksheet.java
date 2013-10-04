package digester;

import renderers.Message;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
    private final List<Rule> rules = new ArrayList<Rule>();
    // Store the validation object, passed into the run method
    private Validation validation = null;
    // Store all messages related to this Worksheet
    private LinkedList<Message> messages = new LinkedList<Message>();
    // Store the reference for the columns associated with this worksheet
    private final List<Column_trash> columns = new ArrayList<Column_trash>();

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
    public LinkedList<Message> getMessages() {
        return messages;
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


    public void print() {
        System.out.println("  sheetname=" + sheetname);

        System.out.println("  rules ... ");
        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            r.print();
        }

        System.out.println("  columns ... ");
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

        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();

            // Run this particular rule
            try {
                // Set the digester worksheet instance for this Rule
                r.setDigesterWorksheet(this);
                // Set the TabularDataReader worksheet instance for this Rule
                r.setWorksheet(validation.getTabularDataReader());

                Method method = r.getClass().getMethod(r.getType());
                if (method != null) {
                    method.invoke(r);
                } else {
                    System.out.println("\tNo method " + r.getType() + " (" + r.getColumn() + ")");
                }
            } catch (Exception e) {
                //e.getCause();
                //e.printStackTrace();
                System.out.println("\tInternal exception attempting to run rule = " + r.getType() + ", for column = " + r.getColumn() + ")");
            }

            // Display warnings/etc...
            messages.addAll(r.getMessages());

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
        for (Iterator<Message> m = messages.iterator(); m.hasNext(); ) {
            if (m.next().getLevel() == Message.ERROR)
                return false;
        }
        return true;
    }
}
