package digester;

import ognl.IteratorElementsAccessor;
import renderers.Message;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * digester.Worksheet class holds all elements pertaining to worksheets
 */
public class Worksheet {

    // the name of this worksheet (as defined by the spreadsheet)
    private String sheetname;
    // store the rules associated with this worksheet
    private final List<Rule> rules = new ArrayList<Rule>();
    private Validation validation = null;
    // Store all messages related to this Worksheet
    private LinkedList<Message> messages = new LinkedList<Message>();

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
     * @param r
     */
    public void addRule(Rule r) {
        rules.add(r);
    }

    public void print() {
        System.out.println("  sheetname=" + sheetname);

        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            r.print();
        }
    }

    public boolean run(Object parent) {
        // Set a reference to the validation parent
        validation = (Validation) parent;

        for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();

            // Run this particular rule
            try {
                // Call the Rule's run method to pass in TabularDataReader reference
                //r.run(validation.getTabularDataReader());
                // Set the digester worksheet instance for this Rule
                r.setDigesterWorksheet(this);
                // Set the TabularDataReader worksheet instance this Rule
                r.setWorksheet(validation.getTabularDataReader());
                //System.out.println("current tablename = " + r.getWorksheet().getCurrentTableName());

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
