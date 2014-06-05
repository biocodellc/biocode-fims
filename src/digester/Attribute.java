package digester;

import settings.fimsPrinter;

import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * Attribute representation
 */
public class Attribute implements Comparable {
    private String isDefinedByURIString = "http://www.w3.org/2000/01/rdf-schema#isDefinedBy";

    private String group;
    private String column;
    private String uri;
    private String defined_by;
    private String datatype = "string";  // string is default type
    private String definition;


    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDefined_by() {
        return defined_by;
    }

    public void setDefined_by(String defined_by) {
        this.defined_by = defined_by;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDefinition() {
        return definition;
    }

    public void addDefinition(String definition) {
        this.definition = definition;
    }

    /**
     * Basic Text printer
     */
    public void print() {
        fimsPrinter.out.println("  Attribute:");
        fimsPrinter.out.println("    column=" + column);
        fimsPrinter.out.println("    uri=" + uri);
        fimsPrinter.out.println("    datatype=" + datatype);
        fimsPrinter.out.println("    isDefinedBy=" + defined_by);
    }

    /**
     * * Generate D2RQ Mapping Language representation of this Attribute.
     *
     * @param pw       PrintWriter used to write output to.
     * @param parent
     * @param colNames
     */
    public void printD2RQ(PrintWriter pw, Object parent, List<String> colNames) throws Exception {

        String classMap = ((Entity) parent).classMap();
        String table = ((Entity) parent).getWorksheet();
        String classMapStringEquivalence = "";

        // Only print this column if it is in a list of colNames
        if (colNames.contains(column)) {
            String classMapString = "map:" + classMap + "_" + column;
            pw.println(classMapString + " a d2rq:PropertyBridge;");
            pw.println("\td2rq:belongsToClassMap " + "map:" + classMap + ";");
            pw.println("\td2rq:property <" + uri + ">;");
            pw.println("\td2rq:column \"" + table + "." + column + "\";");
            // Specify Datatype here
            //if (datatype != null) {
            //    pw.println("\td2rq:datatype " + datatype + ";");
            //}
            pw.println("\td2rq:condition \"" + table + "." + column + " <> ''\";");
            // Specify an equivalence, which is isDefinedBy
            classMapStringEquivalence = classMapString + "_Equivalence";
            pw.println("\td2rq:additionalPropertyDefinitionProperty " + classMapStringEquivalence + ";");

            pw.println("\t.");

            // Always use isDefinedBy, even if the user has not expressed it explicitly.  We do this by
            // using the uri value if NO isDefinedBy is expressed.
            pw.println(classMapStringEquivalence + " a d2rq:AdditionalProperty;");
            pw.println("\td2rq:propertyName <" + isDefinedByURIString + ">;");
            if (defined_by != null) {
                pw.println("\td2rq:propertyValue <" + defined_by + ">;");
            } else {
                pw.println("\td2rq:propertyValue <" + uri + ">;");
            }
            pw.println("\t.");
        }
    }

    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
