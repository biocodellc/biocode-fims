package digester;

import settings.fimsPrinter;

import java.io.PrintWriter;
import java.util.*;
import java.util.List;

/**
 * Attribute representation
 */
public class Attribute implements Comparable {
    private String column;
    private String uri;
    private String datatype = "string";  // string is default type
    private String definition;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
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

        // Only print this column if it is in a list of colNames
        if (colNames.contains(column)) {
            pw.println("map:" + classMap + "_" + column + " a d2rq:PropertyBridge;");
            pw.println("\td2rq:belongsToClassMap " + "map:" + classMap + ";");
            pw.println("\td2rq:property <" + uri + ">;");
            pw.println("\td2rq:column \"" + table + "." + column + "\";");
            //if (datatype != null) {
            //    pw.println("\td2rq:datatype " + datatype + ";");
            //}
            pw.println("\td2rq:condition \"" + table + "." + column + " <> ''\";");
            pw.println("\t.");
        }
    }


    public int compareTo(Object o) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
