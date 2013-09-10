package digester;

import java.io.PrintWriter;

/**
 * Entity representation
 */
public class Entity {

    private String worksheet;
    private String worksheetUniqueKey;
    private String conceptName;
    private String conceptURI;
    private String bcid;
    private String entityId;

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

    public String getConceptName() {
        return conceptName;
    }

    public void setConceptName(String conceptName) {
        this.conceptName = conceptName;
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    public String getBcid() {
        return bcid;
    }

    public void setBcid(String bcid) {
        this.bcid = bcid;
    }

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
        return worksheet + "_" + worksheetUniqueKey + "_" + conceptName;
    }

    /**
     * Basic Text printer
     */
    public void print() {
        System.out.println("  EntityId:" + entityId);
        System.out.println("    worksheet=" + worksheet);
        System.out.println("    worksheetUniqueKey=" + worksheetUniqueKey);
        System.out.println("    conceptName=" + conceptName);
        System.out.println("    conceptURI=" + conceptURI);
        System.out.println("    bcid=" + bcid);
    }

    /**
     * Generate D2RQ Mapping Language representation of this Entity with Attributes.
     *
     * @param pw PrintWriter used to write output to.
     */
    void printD2RQ(PrintWriter pw, Mapping mapping) {
        pw.println("map:" + classMap() + " a d2rq:ClassMap;");
        pw.println("\td2rq:dataStorage " + "map:database;");
        pw.println(mapping.getPersistentIdentifier(this));
        pw.println("\td2rq:class <" + this.conceptURI + ">;");
        // ensures non-null values
        pw.println("\td2rq:condition \"" + getColumn() + " <> ''\";");

        // TODO: add in extra conditions (do i need this???)
        //pw.println(getExtraConditions());
        pw.println("\t.");

        // TODO: Join in Attributes
        /*pw.println("\t.");
        if (attributes != null) {
            for (Attribute attribute : attributes)
                attribute.printD2RQ(pw, classMap(), table);
        } */
    }

}
