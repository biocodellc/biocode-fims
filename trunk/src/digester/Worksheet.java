package digester;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * digester.Worksheet class holds all elements pertaining to worksheets
 */
public class Worksheet {

    private String sheetname;
    private final List<Rule> rules = new ArrayList<Rule>();

    public void setSheetname(String sheetname) {
        this.sheetname = sheetname;
    }


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
}
