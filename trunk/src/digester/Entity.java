package digester;

import settings.fimsPrinter;

import java.io.PrintWriter;
import java.util.LinkedList;

/**
 * Entity representation
 */
public class Entity {

    private String worksheet;
    private String worksheetUniqueKey;
    private String conceptAlias;
    private String conceptURI;
    //private String bcid;
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
        this.conceptAlias = conceptAlias;
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    /*public String getBcid() {
        return bcid;
    }

    public void setBcid(String bcid) {
        this.bcid = bcid;
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
        fimsPrinter.out.println("  EntityId:" + entityId);
        fimsPrinter.out.println("    worksheet=" + worksheet);
        fimsPrinter.out.println("    worksheetUniqueKey=" + worksheetUniqueKey);
        fimsPrinter.out.println("    conceptName=" + conceptAlias);
        fimsPrinter.out.println("    conceptURI=" + conceptURI);
        //fimsPrinter.out.println("    bcid=" + bcid);

    }

    /**
     * Generate D2RQ Mapping Language representation of this Entity with Attributes.
     *
     * @param pw PrintWriter used to write output to.
     */
    public void printD2RQ(PrintWriter pw, Object parent) throws Exception {
        pw.println("map:" + classMap() + " a d2rq:ClassMap;");
        pw.println("\td2rq:dataStorage " + "map:database;");
        pw.println(((Mapping)parent).getPersistentIdentifier(this));
        pw.println("\td2rq:class <" + this.conceptURI + ">;");
        // ensures non-null values
        pw.println("\td2rq:condition \"" + getColumn() + " <> ''\";");

        // TODO: add in extra conditions (May not be necessary)
        //pw.println(getExtraConditions());
        pw.println("\t.");

        // Loop through attributes associated with this Entity
        if (attributes.size() > 0) {
            for (Attribute attribute : attributes)
                attribute.printD2RQ(pw, this);
        }
    }

}
