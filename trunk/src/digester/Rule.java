package digester;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Rule does the heavy lift for the Validation Components. This is where the code is written for each of the rules
 * encountered in the XML configuration file.
 */
public class Rule implements ValidationInterface {

    // General values
    private String level;
    private String type;
    private String column;
    private String list;

    // A reference to the validation object this rule belongs to
    private Validation validation;
    // A reference to the worksheet object this rule belongs to
    private Worksheet worksheet;

    // Rules can also own their own fields
    private List fields = new ArrayList();

    /*
    // NOTE: not sure i want these values in this class, maybe define a sub-class?
    // values for DwCLatLngChecker (optional)
    private String decimalLatitude;
    private String decimalLongitude;
    private String maxErrorInMeters;
    private String horizontalDatum;

    private String plateName;
    private String wellNumber;
    */

    public String getList() {
        return list;
    }

    public void setList(String list) {
        this.list = list;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public void addField(String field) {
        fields.add(field);
    }

    public List getFields() {
        return fields;
    }


    public void print() {
        System.out.println("    rule type = " + this.type + "; column = " + this.column + "; level = " + this.level);

        for (Iterator i = fields.iterator(); i.hasNext(); ) {
            String field = (String) i.next();
            System.out.println("      field data : " + field);
        }
    }

    public void run(Object parent) {
        // Create the reference to the parent object (useful for rule processing)
        validation = ((Worksheet) parent).getValidation();
        worksheet = (Worksheet) parent;

        // Run this particular rule
        try {
            Method method = this.getClass().getMethod(this.getType());
            if (method != null) {
                method.invoke(this);
            } else {
                System.out.println("No method " + this.getType() + " (" + this.getColumn() + ")");
            }
        } catch (Exception e) {
            System.out.println("Exception running " + this.getType() + " (" + this.getColumn() + ")");
        }
    }

    /**
     * Make sure this column contains unique values
     */
    public void uniqueValue() {
        System.out.println("\tunique value rule " + worksheet.getSheetname());
    }

    /**
     * Lookup the fields belonging to a particular list
     */
    public void lookupList() {
        System.out.println("\tlookupList rule for list=" + getList() + " on column " + getColumn());
        // Fetch the fields belonging to this list and create a List object
        List list =  validation.findList(this.getList()).getFields();
        // Loop values
        for (Iterator i = list.iterator(); i.hasNext(); ) {
            System.out.println("\t\t" + i.next());
        }
    }


}

