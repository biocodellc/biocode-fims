package digester;

import settings.FimsPrinter;

import java.io.PrintWriter;
import java.util.*;

/**
 * Entity representation
 */
public class Entity {

    private String worksheet;
    private String worksheetUniqueKey;
    private String conceptAlias;
    private String conceptURI;
    private String entityId;

    private final LinkedList<Attribute> attributes = new LinkedList<Attribute>();

    /**
     * Add an Attribute to this Entity by appending to the LinkedList of attributes
     *
     * @param a
     */
    public void addAttribute(Attribute a) {
        attributes.addLast(a);
    }

    public LinkedList<Attribute> getAttributes() {
        return attributes;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getWorksheet() {
        return worksheet;
    }

    public void setWorksheet(String worksheet) {
        this.worksheet = worksheet;
    }

    public String getWorksheetUniqueKey() {
        return worksheetUniqueKey;
    }

    public void setWorksheetUniqueKey(String worksheetUniqueKey) {
        this.worksheetUniqueKey = worksheetUniqueKey;
    }

    public String getConceptAlias() {
        return conceptAlias;
    }

    public void setConceptAlias(String conceptAlias) {
        this.conceptAlias = conceptAlias.replace(" ","_");
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    /*public String getBcid() {
        return Bcid;
    }

    public void setBcid(String Bcid) {
        this.Bcid = Bcid;
    }*/

    /**
     * Get the table.column notation
     *
     * @return
     */
    public String getColumn() {
        return worksheet + "." + worksheetUniqueKey;
    }

    /**
     * Generate D2RQ Mapping Language ClassMap name of this Entity.
     *
     * @return D2RQ Mapping ClassMap name.
     */
    String classMap() {
        return worksheet + "_" + worksheetUniqueKey + "_" + conceptAlias;
    }

    /**
     * Basic Text printer
     */
    public void print() {
        FimsPrinter.out.println("  EntityId:" + entityId);
        FimsPrinter.out.println("    worksheet=" + worksheet);
        FimsPrinter.out.println("    worksheetUniqueKey=" + worksheetUniqueKey);
        FimsPrinter.out.println("    conceptName=" + conceptAlias);
        FimsPrinter.out.println("    conceptURI=" + conceptURI);
        //fimsPrinter.out.println("    Bcid=" + Bcid);
         if (attributes.size() > 0) {
            for (Attribute attribute : attributes)
                attribute.print();
        }

    }

    /**
     * Generate D2RQ Mapping Language representation of this Entity with Attributes.
     *
     * @param pw PrintWriter used to write output to.
     */
    public void printD2RQ(PrintWriter pw, Object parent) {
        pw.println("map:" + classMap() + " a d2rq:ClassMap;");
        pw.println("\td2rq:dataStorage " + "map:database;");
        pw.println(((Mapping) parent).getPersistentIdentifier(this));
        pw.println("\td2rq:class <" + this.conceptURI + ">;");
        // ensures non-null values ... don't apply if this is a hash
        if (!getColumn().contains("hash"))
            pw.println("\td2rq:condition \"" + getColumn() + " <> ''\";");

        // TODO: add in extra conditions (May not be necessary)
        //pw.println(getExtraConditions());
        pw.println("\t.");

        // Get a list of colNames that we know are good from the spreadsheet
        java.util.List<String> colNames = ((Mapping) parent).getColNames();
        // Normalize the column names so they can be mapped according to how they appear in SQLite
        ArrayList<String> normalizedColNames = new ArrayList<String>();
        Iterator it = colNames.iterator();
        while (it.hasNext()) {
            String colName = (String)it.next();
            normalizedColNames.add(colName.replace(" ","_").replace("/",""));
        }

        // Loop through attributes associated with this Entity
        if (attributes.size() > 0) {
            for (Attribute attribute : attributes)
                attribute.printD2RQ(pw, this, normalizedColNames);
        }
    }

}
