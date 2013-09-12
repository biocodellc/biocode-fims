package digester;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Rule {

    // General values
    private String level;
    private String type;
    private String column;

    // values for DwCLatLngChecker (optional)
    private String decimalLatitude;
    private String decimalLongitude;
    private String maxErrorInMeters;
    private String horizontalDatum;

    private String plateName;
    private String wellNumber;

    // A list of values described in the metadata section (optional)
    private String list;

    private List fields = new ArrayList();

    /**
     * That which has an attribute of list in a ruletype we can look up a generic
     * list of names in the metadata section of the document.
     *
     * @param pList
     * @return
     */
    public List getListElements(String pList) {

        List<String> dynamicList = null;
        try {
          //  Field field = me.getClass().getField(pList);
          //  dynamicList = (List<String>) field.get(me);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return dynamicList;
    }

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
        System.out.println("    rule type = " + this.type + "; column = " + this.column + "; level = " + this.level );

        for (Iterator i = fields.iterator(); i.hasNext();) {
            String field = (String) i.next();
            System.out.println("      field data : " + field);
        }

    }
}

