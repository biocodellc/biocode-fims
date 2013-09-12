package digester;

import java.io.PrintWriter;

/**
 * Attribute representation
 */
public class Attribute {
    private String column;
    private String uri;

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Basic Text printer
     */
    public void print() {
        System.out.println("  Attribute:");
        System.out.println("    column=" + column);
        System.out.println("    uri=" + uri);
    }

    /**
     * Generate D2RQ Mapping Language representation of this Attribute.
     *
     * @param pw       PrintWriter used to write output to.
     * @param classMap D2RQ Mapping Language ClassMap that this Attribute belongs to.
     * @param table    Database table that this Attribute comes from.
     */
    void printD2RQ(PrintWriter pw, String classMap, String table) {
        pw.println("map:" + classMap + "_" + column + " a d2rq:PropertyBridge;");
        pw.println("\td2rq:belongsToClassMap " + "map:" + classMap + ";");
        pw.println("\td2rq:property <" + uri + ">;");
        pw.println("\td2rq:column \"" + table + "." + column + "\";");
        pw.println("\td2rq:condition \"" + table + "." + column + " <> ''\";");
        pw.println("\t.");
    }
}
