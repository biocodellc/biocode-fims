package digester;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * digester.Worksheet class holds all elements pertaining to worksheets
 */
public class Worksheet implements ValidationInterface {

    // the name of this worksheet (as defined by the spreadsheet)
    private String sheetname;
    // store the rules associated with this worksheet
    private final List<Rule> rules = new ArrayList<Rule>();

    private Validation validation = null;

    /**
     * Get a reference to the validation object.  This is useful when working with
     * worksheets and rules to reference objects belonging to the validation object,
     * in particular lists.
     * @return
     */
    public Validation getValidation() {
        return validation;
    }

    /**
     * Set the name of this worksheet
     * @param sheetname
     */
    public void setSheetname(String sheetname) {
        this.sheetname = sheetname;
    }

    /**
     * Get the name of this worksheet
     * @return
     */
    public String getSheetname() {
        return sheetname;
    }

    /**
     * Add a rule for this worksheet
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

    public void run(Object parent)  {
        // Set a reference to the validation parent
        validation = (Validation)parent;

         for (Iterator<Rule> i = rules.iterator(); i.hasNext(); ) {
            Rule r = i.next();
            r.run(this);
        }
    }
}
